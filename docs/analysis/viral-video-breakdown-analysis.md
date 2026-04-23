# 爆款拆解功能深度分析报告

**日期**: 2026-04-20  
**问题**: 短视频下载后没有拆解成功  
**路由**: `/admin/shortvideo/viral-videos`

---

## 一、架构概览

### 1.1 前端流程（ViralVideoPage.tsx）

```
用户操作流程：
1. 列表展示 → viralList() → /api/v1/short-video/viral/list
2. 点击"拆解分析" → viralAnalyze(id) → /api/v1/short-video/viral/analyze
3. 打开详情抽屉 → viralGet(id) → /api/v1/short-video/viral/get
4. 自动轮询（4秒）→ 当 deepAnalyzeStatus = 'processing' | 'pending'
5. 展示结果：
   - Tab 0: 素材与关键帧（videoBosUrl, keyframeBosUrls）
   - Tab 1: 口播脚本（transcript）
   - Tab 2: 拆解结论（deepAnalysisResult JSON）
```

**关键状态字段**：
- `deepAnalyzeStatus`: `pending` | `processing` | `completed` | `failed`
- `deepAnalyzeProgress`: JSON 进度对象（11步）
- `videoBosUrl`: BOS 视频 CDN 地址
- `keyframeBosUrls`: JSON 数组，关键帧 CDN 地址列表
- `deepAnalysisResult`: 拆解 JSON 结果

### 1.2 后端调用链

```
ViralVideoController.analyze()
  ↓ POST /api/v1/short-video/viral/analyze
ViralVideoServiceImpl.triggerAnalysis()
  ↓ 判断是否有 sourceVideoId（旧链路）
ViralVideoDeepAnalysisServiceImpl.startDeepAnalyze()
  ↓ 设置 status=processing，写入 started_at
ViralDeepAnalyzeAsyncRunner.runDeepAnalyzeAsync()
  ↓ @Async 异步执行
ViralDeepAnalyzeExecutor.executeDeepAnalyze()
  ↓ 实际执行 11 步管线
```

---

## 二、深度拆解管线（11步）

### Step 0: 元数据提取
- **类**: `ViralMetadataExtractor`
- **工具**: `yt-dlp --dump-json`
- **作用**: 提取播放量、点赞数、作者信息、封面、时长等
- **失败影响**: 不阻塞管线，仅元数据不全

### Step 1: 下载视频
- **类**: `VideoAnalysisService.downloadVideo()`
- **工具**: `yt-dlp` 或 `PlaywrightDouyinDownloader`（回退）
- **配置**:
  ```yaml
  app.video-analysis.enabled: true
  app.video-analysis.yt-dlp-path: yt-dlp
  app.video-analysis.yt-dlp-cookies-file: /path/to/cookies.txt
  app.video-analysis.yt-dlp-cookies-from-browser: chrome
  app.video-analysis.douyin-skip-yt-dlp-download: false  # 抖音跳过下载开关
  ```
- **抖音特殊处理**:
  - 需要 cookies（登录态）
  - 未配置 cookies 会警告但继续尝试
  - 失败时回退到 Playwright 拦截 CDN（需 `playwright-enabled: true`）
  - 可配置 `douyin-skip-yt-dlp-download: true` 跳过下载，直接推演

**关键代码**（ViralDeepAnalyzeExecutor.java:466-544）：
```java
if (douyinSkipYtDlpDownload && isDouyinShareUrl(viral.getVideoUrl())) {
    // 跳过下载，直接推演
    progressTouch(viral, flush, "download", "download", "skipped", "已配置跳过本机下载（抖音链接）", null, true);
    transcript = "（抖音链接：已配置跳过本机 yt-dlp 下载...）";
} else {
    try {
        videoPath = videoAnalysisService.downloadVideo(viral.getVideoUrl());
        progressTouch(viral, flush, "download", "download", "done", "视频已下载", null, true);
        downloadSucceeded = true;
    } catch (Exception e) {
        // 下载失败，降级为推演模式
        progressTouch(viral, flush, "download", "download", "failed", "下载失败：" + e.getMessage(), null, true);
        transcript = "（视频下载失败，无真实口播转写。请基于标题与互动数据推演口播稿...）";
    }
}
```

### Step 2: ASR 转写
- **类**: `VideoAnalysisService.transcribeAudio()`
- **工具**: Whisper（需 `whisper-enabled: true`）
- **流程**: 
  1. `extractAudio()` 提取音频（ffmpeg）
  2. `transcribeAudio()` 转写
- **失败影响**: 降级为推演模式，LLM 基于标题生成口播稿

### Step 3: 场景检测与抽帧
- **类**: `SceneDetectionService` 或 `VideoAnalysisService.extractFrames()`
- **工具**: ffmpeg scenecut 或固定 FPS 抽帧
- **流程**:
  1. 优先使用 `sceneDetectionService.detectScenes()` 智能切场景
  2. 回退到固定 1 FPS 抽帧
  3. 调用多模态模型分析关键帧（`analyzeFrames()`）
- **失败影响**: 降级为推演模式，LLM 基于标题生成分镜

### Step 3.5: BOS 上传
- **类**: `ViralBosUploader`
- **配置**: `app.viral-analysis.bos-upload-enabled: true`（默认 true）
- **上传内容**:
  1. 视频文件 → `videoBosUrl`
  2. 关键帧列表 → `keyframeBosUrls`（JSON 数组）
  3. 封面图 → `coverBosUrl`
- **BOS Key 格式**: `{ownerId}/viral/{viralId}/video.mp4`

**关键代码**（ViralDeepAnalyzeExecutor.java:678-721）：
```java
private void uploadToBos(SvViralVideo viral, String videoPath, List<String> keyframePaths) {
    if (viralBosUploader == null) return;  // BOS 未配置，跳过
    
    // 上传视频
    if (videoPath != null) {
        var videoResult = viralBosUploader.uploadVideo(viral.getOwnerId(), viral.getId(), videoPath);
        if (videoResult != null) {
            viral.setVideoBosUrl(videoResult.cdnUrl());  // 写入 CDN 地址
        }
    }
    
    // 上传关键帧
    if (keyframePaths != null && !keyframePaths.isEmpty()) {
        var kfResults = viralBosUploader.uploadKeyframes(viral.getOwnerId(), viral.getId(), keyframePaths);
        List<String> urls = kfResults.stream().map(r -> r.cdnUrl()).toList();
        viral.setKeyframeBosUrls(JSON.writeValueAsString(urls));  // JSON 数组
    }
}
```

### Step 4: 清理临时文件
- **作用**: 删除本地下载的视频和关键帧
- **时机**: BOS 上传完成后

### Step 5: 评论抓取
- **类**: `ViralCommentExtractor`
- **作用**: 抓取评论用于 Round3 LLM 分析
- **失败影响**: 不阻塞管线

### Step 6-10: LLM 拆解（单次或多轮）
- **单次模式**（`runSinglePromptAnalysis`）:
  - 一次性大 prompt，包含口播+场景+元数据
  - 输出完整 JSON（transcript、structure、scenes、viralHypotheses、remakeVariableTable）
  
- **多轮模式**（`runMultiRoundAnalysis`）:
  - Round 1: 场景视觉分析（多模态模型）
  - Round 2: 叙事结构识别
  - Round 3: 爆款假设与二创变量表

**成功标志**（ViralDeepAnalyzeExecutor.java:1322-1338）：
```java
private void persistDeepAnalysisSuccess(SvViralVideo viral, String mergedJson, ...) {
    viral.setDeepAnalysisResult(mergedJson);  // 写入拆解 JSON
    viral.setDeepAnalyzeStatus("completed");  // 状态改为 completed
    viral.setDeepAnalyzeFinishedAt(new Timestamp(System.currentTimeMillis()));
    viralVideoRepository.save(viral);
}
```

### Step 11: 完成
- 设置 `deepAnalyzeStatus = "completed"`
- 写入 `deepAnalyzeFinishedAt`
- 前端轮询检测到 `completed` 后停止轮询

---

## 三、问题诊断：下载后拆解失败

### 3.1 症状分析

根据前端代码（ViralVideoPage.tsx:646-658）：
```tsx
{deepStatus === 'completed'
  && (!showDetail.videoBosUrl || String(showDetail.videoBosUrl).trim() === '')
  && keyframeList.length === 0 ? (
    <Alert severity="warning">
      分析已完成，但未返回 BOS 视频地址与关键帧：多为未开启 BOS、上传失败或仅走了推演未落库素材。
      请查看下方「状态说明」中的 download / scene / bos 步骤；仍异常时查服务端日志「BOS上传」。
    </Alert>
  ) : null}
```

**可能原因**：
1. ✅ 下载成功（`download` step = `done`）
2. ❌ BOS 上传失败或未配置
3. ❌ 关键帧为空（抽帧失败）
4. ✅ LLM 拆解成功（`deepAnalyzeStatus = completed`）

### 3.2 根因定位

#### 原因 1: BOS 未配置或上传失败

**检查点**：
```yaml
# application.yml
app.viral-analysis.bos-upload-enabled: true  # 必须为 true

# BosStorageService 配置
app.storage.bos.enabled: true
app.storage.bos.endpoint: https://bj.bcebos.com
app.storage.bos.access-key-id: ${BOS_ACCESS_KEY_ID}
app.storage.bos.secret-access-key: ${BOS_SECRET_ACCESS_KEY}
app.storage.bos.bucket-name: your-bucket
```

**日志关键字**：
```
[BOS上传] 视频已上传 key=...
[BOS上传] 关键帧上传完成 viralId=... 成功=.../...
[BOS上传] BOS 上传异常（不阻塞管线）viralId=...
```

**代码逻辑**（ViralBosUploader.java:43-64）：
```java
public BosUploadResult uploadVideo(Long ownerId, Long viralId, String localPath) {
    if (!isAvailable()) return null;  // BOS 未配置，返回 null
    
    Path path = Path.of(localPath).toAbsolutePath().normalize();
    if (!Files.isRegularFile(path)) {
        log.warn("[BOS上传] 视频文件不存在或不是普通文件: {}", path);
        return null;  // 文件不存在
    }
    
    try {
        String bosKey = String.format("%d/viral/%d/video.mp4", ownerId, viralId);
        byte[] data = Files.readAllBytes(path);
        String url = bosStorageService.uploadBytes(bosKey, data, "video/mp4");
        return new BosUploadResult(bosKey, url);
    } catch (Exception e) {
        log.warn("[BOS上传] 视频上传失败 viralId={}: {}", viralId, e.getMessage());
        return null;  // 上传失败
    }
}
```

**修复方案**：
1. 确认 BOS 配置正确（endpoint、credentials、bucket）
2. 检查 `BosStorageService` 是否注入成功
3. 查看服务端日志中的 `[BOS上传]` 相关错误

#### 原因 2: 视频下载失败但未正确标记

**检查点**：
- `deepAnalyzeProgress` JSON 中 `steps.download.status` 是否为 `done`
- 如果为 `failed` 但 `deepAnalyzeStatus = completed`，说明降级为推演模式

**代码逻辑**（ViralDeepAnalyzeExecutor.java:534-544）：
```java
if (!downloadSucceeded) {
    String dtl = "下载失败：" + truncateErr(oneLine);
    progressTouch(viral, flush, "download", "download", "failed", dtl, null, true);
    progressTouch(viral, flush, "asr", "asr", "skipped", "下载未成功，跳过 ASR", null, true);
    t = "（视频下载失败，无真实口播转写。请基于标题与互动数据推演口播稿...）";
    s = "（视频下载失败，无真实画面。请基于标题与互动数据推演分镜。）";
}
```

**特征**：
- `videoBosUrl` 为空
- `keyframeBosUrls` 为空
- `transcript` 包含 "（视频下载失败..."
- `sceneDescriptions` 包含 "（视频下载失败..."

#### 原因 3: 抽帧成功但 BOS 上传时文件已清理

**时序问题**：
```
1. 下载视频 → videoPath = "/tmp/video-analysis/videos/xxx.mp4"
2. 抽帧 → keyframePaths = ["/tmp/video-analysis/frames/xxx_0.jpg", ...]
3. BOS 上传 → 读取 videoPath 和 keyframePaths
4. 清理临时文件 → 删除 videoPath 和 keyframePaths
```

**代码顺序**（ViralDeepAnalyzeExecutor.java:586-594）：
```java
// Step 3.5: BOS 上传（视频+关键帧+封面）
progressTouch(viral, flush, "bos", "bos", "running", "上传 BOS...", null, true);
uploadToBos(viral, videoPath, keyframePaths);  // 先上传
progressTouch(viral, flush, "bos", "bos", "done", "BOS 上传完成", null, true);

// 清理本地临时文件（BOS 上传完成后）
progressTouch(viral, flush, "cleanup", "cleanup", "running", "清理本地临时文件...", null, true);
cleanupLocalFiles(videoPath, sceneSegments);  // 后清理
```

**结论**: 时序正确，不是此问题。

#### 原因 4: 抖音链接配置跳过下载

**检查点**：
```yaml
app.video-analysis.douyin-skip-yt-dlp-download: true  # 如果为 true，跳过下载
```

**代码逻辑**（ViralDeepAnalyzeExecutor.java:454-460）：
```java
if (douyinSkipYtDlpDownload && isDouyinShareUrl(viral.getVideoUrl())) {
    log.info("[深度分析] douyin-skip-yt-dlp-download=true，跳过本机下载 viralId={}", viralVideoId);
    progressTouch(viral, flush, "download", "download", "skipped", "已配置跳过本机下载（抖音链接）", null, true);
    transcript = "（抖音链接：已配置跳过本机 yt-dlp 下载...）";
    sceneDescriptions = "（无本机抽帧；若条目有封面图，将尽量用封面做视觉补充。）";
}
```

**特征**：
- `deepAnalyzeProgress.steps.download.status = "skipped"`
- `deepAnalyzeProgress.steps.download.detail = "已配置跳过本机下载（抖音链接）"`
- `videoBosUrl` 为空（因为没下载）
- `keyframeBosUrls` 为空（因为没抽帧）

---

## 四、排查步骤

### 4.1 前端检查

1. **打开浏览器开发者工具 → Network**
2. **触发拆解分析**，观察请求：
   ```
   POST /api/v1/short-video/viral/analyze
   Response: { status: 200, data: { ok: true, id: 123 } }
   ```
3. **查看详情轮询**：
   ```
   POST /api/v1/short-video/viral/get (每4秒)
   Response: {
     deepAnalyzeStatus: "processing" | "completed" | "failed",
     deepAnalyzeProgress: "{...}",  // JSON 字符串
     videoBosUrl: "https://...",    // 可能为空
     keyframeBosUrls: "[...]"       // JSON 数组字符串
   }
   ```
4. **解析 `deepAnalyzeProgress`**：
   ```json
   {
     "steps": {
       "download": { "status": "done", "detail": "视频已下载" },
       "asr": { "status": "done" },
       "scene": { "status": "done" },
       "bos": { "status": "done", "detail": "BOS 上传完成" },
       "llm_single": { "status": "done" }
     },
     "progress": 100
   }
   ```

### 4.2 后端日志检查

**关键日志**：
```bash
# 1. 下载视频
grep "Step1 下载视频" logs/app.log
grep "yt-dlp 下载失败" logs/app.log
grep "视频下载完成" logs/app.log

# 2. BOS 上传
grep "\[BOS上传\]" logs/app.log
grep "视频已上传" logs/app.log
grep "关键帧上传完成" logs/app.log
grep "BOS 上传异常" logs/app.log

# 3. 深度分析状态
grep "深度分析.*viralId=" logs/app.log
grep "setDeepAnalyzeStatus" logs/app.log
```

### 4.3 数据库检查

```sql
-- 查看爆款视频记录
SELECT 
  id,
  title,
  deep_analyze_status,
  video_bos_url,
  keyframe_bos_urls,
  deep_analyze_progress,
  deep_analyze_error,
  deep_analysis_result IS NOT NULL AS has_result
FROM sv_viral_video
WHERE id = 123;

-- 检查 BOS URL 是否为空
SELECT 
  COUNT(*) AS total,
  COUNT(video_bos_url) AS has_video_url,
  COUNT(keyframe_bos_urls) AS has_keyframes
FROM sv_viral_video
WHERE deep_analyze_status = 'completed';
```

### 4.4 配置检查

```bash
# 检查 BOS 配置
grep -A 10 "app.storage.bos" application.yml
grep -A 10 "app.viral-analysis" application.yml

# 检查环境变量
echo $BOS_ACCESS_KEY_ID
echo $BOS_SECRET_ACCESS_KEY
```

---

## 五、常见问题与解决方案

### 问题 1: 下载成功但 BOS 上传失败

**症状**：
- `deepAnalyzeProgress.steps.download.status = "done"`
- `deepAnalyzeProgress.steps.bos.status = "done"` 但 `videoBosUrl` 为空
- 日志：`[BOS上传] BOS 上传异常`

**原因**：
- BOS 配置错误（endpoint、credentials、bucket）
- BOS 服务不可达
- 文件过大超时

**解决方案**：
1. 检查 `BosStorageService` 配置
2. 测试 BOS 连接：
   ```java
   @Autowired
   private BosStorageService bosStorageService;
   
   public void testBos() {
       String url = bosStorageService.uploadBytes("test/test.txt", "hello".getBytes(), "text/plain");
       log.info("BOS 测试上传成功: {}", url);
   }
   ```
3. 增加上传超时时间

### 问题 2: 抖音视频下载失败

**症状**：
- `deepAnalyzeProgress.steps.download.status = "failed"`
- 日志：`yt-dlp 下载失败 exit=1`、`Fresh cookies required`

**原因**：
- 抖音需要登录态（cookies）
- yt-dlp 版本过旧
- 网络问题

**解决方案**：
1. **配置 cookies**：
   ```yaml
   app.video-analysis.yt-dlp-cookies-file: /path/to/cookies.txt
   # 或
   app.video-analysis.yt-dlp-cookies-from-browser: chrome
   ```
   
2. **导出 cookies**（Chrome）：
   - 安装插件：Get cookies.txt LOCALLY
   - 登录抖音网页版
   - 导出 cookies.txt（Netscape 格式）
   
3. **升级 yt-dlp**：
   ```bash
   pip install -U yt-dlp
   # 或
   yt-dlp -U
   ```
   
4. **启用 Playwright 回退**：
   ```yaml
   app.video-analysis.playwright-enabled: true
   app.video-analysis.douyin-yt-dlp-fallback-playwright: true
   ```

### 问题 3: 抽帧成功但关键帧为空

**症状**：
- `deepAnalyzeProgress.steps.scene.status = "done"`
- `keyframeBosUrls` 为空或 `[]`
- 日志：`关键帧上传完成 viralId=... 成功=0/10`

**原因**：
- 抽帧文件路径错误
- 抽帧文件在 BOS 上传前被清理
- BOS 上传关键帧失败

**解决方案**：
1. 检查抽帧日志：
   ```bash
   grep "extractFrames\|场景检测" logs/app.log
   ```
2. 检查 BOS 上传日志：
   ```bash
   grep "关键帧.*上传" logs/app.log
   ```
3. 确认文件存在：
   ```java
   if (!Files.exists(path)) {
       log.warn("关键帧文件不存在: {}", path);
       continue;
   }
   ```

### 问题 4: 配置跳过下载导致无素材

**症状**：
- `deepAnalyzeProgress.steps.download.status = "skipped"`
- `deepAnalyzeProgress.steps.download.detail = "已配置跳过本机下载（抖音链接）"`
- `videoBosUrl` 和 `keyframeBosUrls` 均为空
- `deepAnalysisResult` 有内容（推演模式）

**原因**：
- 配置了 `app.video-analysis.douyin-skip-yt-dlp-download: true`
- 系统走推演模式，不下载视频

**解决方案**：
1. **关闭跳过下载**：
   ```yaml
   app.video-analysis.douyin-skip-yt-dlp-download: false
   ```
2. **配置 cookies** 以支持抖音下载
3. **接受推演模式**：如果只需要拆解结论，可以接受无素材

---

## 六、前端改进建议

### 6.1 增强错误提示

当前前端仅提示"未返回 BOS 视频地址与关键帧"，可以更精确：

```tsx
// 解析 deepAnalyzeProgress 判断具体失败步骤
const steps = parseDeepAnalyzeProgressSteps(showDetail.deepAnalyzeProgress);

if (steps?.download?.status === 'failed') {
  return <Alert severity="error">
    视频下载失败：{steps.download.detail}
    <br />建议：检查 yt-dlp 配置、cookies 或网络连接
  </Alert>;
}

if (steps?.bos?.status === 'failed') {
  return <Alert severity="error">
    BOS 上传失败：{steps.bos.detail}
    <br />建议：检查 BOS 配置或联系管理员
  </Alert>;
}

if (steps?.download?.status === 'skipped') {
  return <Alert severity="info">
    已配置跳过视频下载，当前为推演模式（基于标题与元数据生成）。
    如需真实素材，请联系管理员开启视频下载功能。
  </Alert>;
}
```

### 6.2 显示进度详情

```tsx
// 在详情抽屉中显示各步骤状态
<Accordion>
  <AccordionSummary>查看拆解进度详情</AccordionSummary>
  <AccordionDetails>
    <List>
      <ListItem>
        <ListItemIcon>
          {steps?.download?.status === 'done' ? <CheckIcon color="success" /> : <ErrorIcon color="error" />}
        </ListItemIcon>
        <ListItemText 
          primary="视频下载" 
          secondary={steps?.download?.detail || '—'} 
        />
      </ListItem>
      {/* 其他步骤... */}
    </List>
  </AccordionDetails>
</Accordion>
```

---

## 七、总结

### 核心问题

**"短视频下载后没有拆解成功"** 实际上是 **"下载成功但 BOS 上传失败或未配置"**。

### 排查优先级

1. **检查 BOS 配置**（最可能）
   - `app.storage.bos.enabled: true`
   - `app.storage.bos.endpoint`、`access-key-id`、`secret-access-key`、`bucket-name`
   
2. **检查日志**
   - `[BOS上传]` 相关错误
   - `BosStorageService` 是否注入成功
   
3. **检查下载状态**
   - `deepAnalyzeProgress.steps.download.status`
   - 如果为 `skipped`，检查 `douyin-skip-yt-dlp-download` 配置
   
4. **检查抽帧状态**
   - `deepAnalyzeProgress.steps.scene.status`
   - 关键帧文件是否生成

### 快速修复

```yaml
# application.yml
app:
  storage:
    bos:
      enabled: true
      endpoint: https://bj.bcebos.com
      access-key-id: ${BOS_ACCESS_KEY_ID}
      secret-access-key: ${BOS_SECRET_ACCESS_KEY}
      bucket-name: your-bucket
  
  viral-analysis:
    bos-upload-enabled: true
  
  video-analysis:
    enabled: true
    douyin-skip-yt-dlp-download: false
    yt-dlp-cookies-file: /path/to/cookies.txt
```

### 监控指标

建议添加以下监控：
- BOS 上传成功率
- 视频下载成功率
- 深度分析完成率
- 各步骤耗时分布
