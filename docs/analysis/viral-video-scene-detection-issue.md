# 爆款拆解场景检测问题分析

**日期**: 2026-04-20  
**问题**: 场景检测只生成 1 个场景（0.0s-311.4s），关键帧只有 1 张，视觉分析无响应

---

## 问题现象

### 前端显示
```
场景 1 · 0.0s-311.4s
（视觉分析无响应）

视频 / 关键帧 — 状态说明
① 本机是否下载了视频（yt-dlp）？
   是 / 已完成 — 视频已下载 ✅

② 能否在下方播放缓存视频？
   可以：已写入 video_bos_url ✅

③ 是否做了抽帧 / 场景分析？
   是 / 已完成 — 场景/抽帧处理完成 已入库关键帧 URL 1 张 ⚠️

④ 页面上能展示吗？
   能：有缓存地址则展示播放器与关键帧图 ✅
```

### 异常点
1. **整个视频（311.4秒）被识别为 1 个场景** - 不合理
2. **只有 1 张关键帧** - 正常应该有多张
3. **"视觉分析无响应"** - 多模态模型调用失败

---

## 根因分析

### 场景检测流程

**代码位置**: `ViralDeepAnalyzeExecutor.java:488-523`

```java
// Step 3: 抽帧+视觉
if (multiRoundAnalysis && sceneDetectionService != null) {
    try {
        // 优先使用 scenecut 智能切场景
        sceneSegments = sceneDetectionService.detectScenes(videoPath);
        log.info("[深度分析] SceneDetection 检测到 {} 个场景", sceneSegments.size());
        
        // 收集关键帧路径
        keyframePaths = new ArrayList<>();
        for (int i = 0; i < sceneSegments.size(); i++) {
            var seg = sceneSegments.get(i);
            if (seg.keyframePath() != null) {
                keyframePaths.add(seg.keyframePath());
            }
        }
    } catch (Exception sce) {
        log.warn("[深度分析] 场景检测失败，回退固定 FPS: {}", sce.getMessage());
        sceneSegments = null;
    }
}

// 回退到固定 FPS 抽帧
if (sceneSegments == null || sceneSegments.isEmpty()) {
    List<String> framePaths = videoAnalysisService.extractFrames(videoPath, 1);  // 1 FPS
    if (framePaths != null && !framePaths.isEmpty()) {
        keyframePaths = new ArrayList<>(framePaths);
        List<String> sub = framePaths.subList(0, Math.min(10, framePaths.size()));
        List<String> descriptions = videoAnalysisService.analyzeFrames(sub, "...");
        s = String.join("\n", descriptions);
    }
}
```

### 问题 1: 场景检测只返回 1 个场景

**SceneDetectionService.java:65-148**

```java
public List<SceneSegment> detectScenes(String videoPath, double threshold) throws Exception {
    // Step 1: ffmpeg 检测场景变化
    ProcessBuilder pb = new ProcessBuilder(
        ffmpegPath,
        "-i", videoPath,
        "-vf", "select='gt(scene," + threshold + ")',showinfo",  // 场景检测
        "-vsync", "vfr",
        "-q:v", "2",
        outputPattern
    );
    
    // Step 2: 检查输出帧文件
    File[] frameFiles = scenesDir.toFile().listFiles((dir, name) -> name.endsWith(".jpg"));
    if (frameFiles == null || frameFiles.length == 0) {
        log.info("[SceneDetection] 未检测到场景切换（threshold={}），回退到首帧", threshold);
        return extractFirstFrame(videoPath, scenesDir);  // ← 只返回首帧！
    }
    
    // Step 3: 解析时间戳
    List<Double> timestamps = parsePtsTimestamps(output.toString());
    double duration = getVideoDuration(videoPath);
    
    // Step 4: 构建场景片段
    for (int i = 0; i < frameFiles.length; i++) {
        double startTime = (i < timestamps.size()) ? timestamps.get(i) : 0;
        double endTime;
        if (i + 1 < timestamps.size()) {
            endTime = timestamps.get(i + 1);
        } else {
            endTime = duration > 0 ? duration : startTime + 5;  // ← 最后一个场景到视频结束
        }
        segments.add(new SceneSegment(startTime, endTime, frameFiles[i].getAbsolutePath()));
    }
}
```

**可能原因**：

#### 原因 A: 场景检测阈值过高（最可能）
```yaml
app.video-analysis.scene-detection-threshold: 0.3  # 默认值
```

- **阈值 0.3** 表示场景变化需要达到 30% 才触发
- 如果视频画面变化不大（如固定机位、单一场景），可能检测不到场景切换
- ffmpeg 未输出任何场景切换帧 → `frameFiles.length == 0`
- 回退到 `extractFirstFrame()` → **只返回首帧**
- 首帧时间 0.0s，视频总时长 311.4s → **场景 1: 0.0s-311.4s**

#### 原因 B: ffmpeg scenecut 失败
- ffmpeg 命令执行失败（exit code != 0）
- 但代码逻辑：即使 exit != 0，只要有输出帧就算成功
- 如果完全失败，会抛异常回退到固定 FPS

#### 原因 C: 视频格式问题
- 某些视频编码格式可能导致 scenecut 失效
- 例如：高度压缩的视频、关键帧间隔过大

### 问题 2: 只有 1 张关键帧

**原因链**：
1. 场景检测只返回 1 个场景（首帧）
2. `keyframePaths` 只包含 1 个路径
3. BOS 上传只上传 1 张图
4. `keyframeBosUrls` JSON 数组只有 1 个元素

### 问题 3: 视觉分析无响应

**代码位置**: `ViralDeepAnalyzeExecutor.java:762-1020` (多轮分析 Round 1)

```java
private void runMultiRoundAnalysis(...) {
    // Round 1: 场景视觉分析（多模态模型）
    if (sceneSegments != null && !sceneSegments.isEmpty()) {
        AiModel visionModel = pickVisionModel();
        if (visionModel == null) {
            log.warn("[深度分析] 未配置多模态模型，跳过 Round1 视觉分析");
            // 回退到单次 prompt
            runSinglePromptAnalysis(...);
            return;
        }
        
        // 限制视觉分析的场景数
        int maxVisionScenes = multiRoundMaxVisionScenes > 0 
            ? Math.min(sceneSegments.size(), multiRoundMaxVisionScenes)
            : sceneSegments.size();
        
        for (int i = 0; i < maxVisionScenes; i++) {
            var seg = sceneSegments.get(i);
            String prompt = String.format(ROUND1_SCENE_VISION_PROMPT, 
                i + 1, seg.startTimeSec(), seg.endTimeSec());
            
            try {
                var resp = llmClient.chatWithImage(visionModel, "...", prompt, 
                    List.of(seg.keyframePath()));  // ← 调用多模态模型
                
                if (resp != null && resp.success()) {
                    sceneVisionResults.add(resp.content());
                } else {
                    sceneVisionResults.add("（视觉分析无响应）");  // ← 这里！
                }
            } catch (Exception e) {
                log.warn("[深度分析] Round1 场景 {} 视觉分析失败: {}", i + 1, e.getMessage());
                sceneVisionResults.add("（视觉分析失败：" + e.getMessage() + "）");
            }
        }
    }
}
```

**可能原因**：

#### 原因 A: 多模态模型未配置
```java
AiModel visionModel = pickVisionModel();
if (visionModel == null) {
    log.warn("[深度分析] 未配置多模态模型，跳过 Round1 视觉分析");
}
```

#### 原因 B: 多模态模型调用失败
- API 超时
- 模型不支持图片输入
- 图片路径错误或文件损坏
- API 配额耗尽

#### 原因 C: 多模态模型返回空响应
```java
if (resp != null && resp.success()) {
    sceneVisionResults.add(resp.content());
} else {
    sceneVisionResults.add("（视觉分析无响应）");  // ← 这里
}
```

---

## 解决方案

### 方案 1: 降低场景检测阈值（推荐）

**配置调整**：
```yaml
app:
  video-analysis:
    scene-detection-threshold: 0.15  # 从 0.3 降到 0.15（更敏感）
    scene-max-count: 20              # 最多保留 20 个场景
```

**效果**：
- 更容易检测到场景变化
- 生成更多关键帧
- 适合画面变化较小的视频

**验证方法**：
```bash
# 手动测试 ffmpeg scenecut
ffmpeg -i video.mp4 \
  -vf "select='gt(scene,0.15)',showinfo" \
  -vsync vfr \
  -q:v 2 \
  scene_%04d.jpg

# 查看输出帧数
ls -l scene_*.jpg | wc -l
```

### 方案 2: 回退到固定 FPS 抽帧

**场景检测失败时的回退逻辑**：
```java
// 当前代码（ViralDeepAnalyzeExecutor.java:513-522）
if (sceneSegments == null || sceneSegments.isEmpty()) {
    List<String> framePaths = videoAnalysisService.extractFrames(videoPath, 1);  // 1 FPS
    // ...
}
```

**问题**：1 FPS 对于 311 秒视频会生成 311 张图，但代码只取前 10 张分析。

**改进建议**：
```java
// 改进：根据视频时长动态调整 FPS
double duration = getVideoDuration(videoPath);
int targetFrames = 15;  // 目标关键帧数
double fps = duration > 0 ? Math.max(0.05, targetFrames / duration) : 1;

List<String> framePaths = videoAnalysisService.extractFrames(videoPath, fps);
```

### 方案 3: 修复多模态模型调用

**检查配置**：
```yaml
app:
  video-analysis:
    vision-model-id: 123  # 指定多模态模型 ID（ai_model 表）
    multi-round-analysis: true
    multi-round-max-vision-scenes: 10  # 限制视觉分析场景数
```

**检查模型配置**（数据库）：
```sql
SELECT id, model_name, model_version, provider, capabilities
FROM ai_model
WHERE id = 123;

-- capabilities 应包含 'vision' 或 'multimodal'
```

**日志检查**：
```bash
grep "未配置多模态模型\|视觉分析失败\|chatWithImage" logs/app.log
```

### 方案 4: 增强错误处理与日志

**当前问题**：
- 场景检测失败时日志不够详细
- 视觉分析失败时只显示"无响应"，不知道具体原因

**改进代码**：
```java
// SceneDetectionService.java
if (frameFiles == null || frameFiles.length == 0) {
    log.warn("[SceneDetection] 未检测到场景切换 threshold={} videoPath={} " +
             "ffmpeg_exit={} output_length={}", 
             threshold, videoPath, process.exitValue(), output.length());
    
    // 输出 ffmpeg 错误信息（前 500 字符）
    String ffmpegOutput = output.toString();
    if (ffmpegOutput.length() > 0) {
        log.debug("[SceneDetection] ffmpeg output: {}", 
                  ffmpegOutput.substring(0, Math.min(500, ffmpegOutput.length())));
    }
    
    return extractFirstFrame(videoPath, scenesDir);
}

// ViralDeepAnalyzeExecutor.java
try {
    var resp = llmClient.chatWithImage(visionModel, "...", prompt, List.of(seg.keyframePath()));
    if (resp != null && resp.success()) {
        sceneVisionResults.add(resp.content());
    } else {
        String detail = resp != null ? resp.errorMsg() : "null response";
        log.warn("[深度分析] Round1 场景 {} 视觉分析无响应: {}", i + 1, detail);
        sceneVisionResults.add("（视觉分析无响应：" + detail + "）");
    }
} catch (Exception e) {
    log.error("[深度分析] Round1 场景 {} 视觉分析异常", i + 1, e);
    sceneVisionResults.add("（视觉分析失败：" + e.getMessage() + "）");
}
```

---

## 排查步骤

### 1. 检查场景检测日志

```bash
# 查看场景检测相关日志
grep "\[SceneDetection\]" logs/app.log | tail -20

# 期望看到：
# [SceneDetection] 开始场景检测: videoPath=..., threshold=0.3
# [SceneDetection] 未检测到场景切换（threshold=0.3），回退到首帧  ← 问题！
# 或
# [SceneDetection] 完成：15 个场景片段  ← 正常
```

### 2. 手动测试 ffmpeg scenecut

```bash
# 进入工作目录
cd /tmp/video-analysis

# 下载测试视频（或使用已有视频）
VIDEO_PATH="/tmp/video-analysis/videos/test.mp4"

# 测试不同阈值
for threshold in 0.1 0.15 0.2 0.3 0.4; do
  echo "Testing threshold: $threshold"
  mkdir -p scenes_$threshold
  ffmpeg -i "$VIDEO_PATH" \
    -vf "select='gt(scene,$threshold)',showinfo" \
    -vsync vfr \
    -q:v 2 \
    scenes_$threshold/scene_%04d.jpg \
    2>&1 | grep "pts_time" | wc -l
  echo "Frames: $(ls scenes_$threshold/*.jpg 2>/dev/null | wc -l)"
done
```

### 3. 检查多模态模型配置

```sql
-- 查看配置的视觉模型
SELECT 
  id, 
  model_name, 
  model_version, 
  provider, 
  capabilities,
  status
FROM ai_model
WHERE capabilities LIKE '%vision%' 
   OR capabilities LIKE '%multimodal%';

-- 检查是否有可用的视觉模型
SELECT COUNT(*) AS vision_model_count
FROM ai_model
WHERE (capabilities LIKE '%vision%' OR capabilities LIKE '%multimodal%')
  AND status = 'active';
```

### 4. 检查配置文件

```bash
# 查看场景检测配置
grep -A 5 "scene-detection" application.yml

# 查看视觉模型配置
grep -A 5 "vision-model" application.yml

# 查看多轮分析配置
grep "multi-round" application.yml
```

### 5. 查看数据库记录

```sql
-- 查看该爆款视频的详细信息
SELECT 
  id,
  title,
  video_duration,
  deep_analyze_status,
  deep_analyze_progress,
  scene_descriptions,
  keyframe_bos_urls,
  deep_analysis_result
FROM sv_viral_video
WHERE id = 123;  -- 替换为实际 ID

-- 解析 keyframe_bos_urls（JSON 数组）
SELECT 
  id,
  title,
  JSON_LENGTH(keyframe_bos_urls) AS keyframe_count
FROM sv_viral_video
WHERE deep_analyze_status = 'completed'
ORDER BY deep_analyze_finished_at DESC
LIMIT 10;
```

---

## 前端改进建议

### 显示场景检测详情

```tsx
// 解析场景描述
const sceneLines = showDetail.sceneDescriptions?.split('\n') || [];
const sceneCount = sceneLines.filter(line => line.includes('场景')).length;

// 警告提示
{sceneCount === 1 && showDetail.videoDuration > 60 && (
  <Alert severity="warning" sx={{ mb: 2 }}>
    ⚠️ 场景检测异常：{showDetail.videoDuration}秒视频只检测到 1 个场景
    <br />
    可能原因：
    <ul>
      <li>场景检测阈值过高（当前 0.3），画面变化不明显</li>
      <li>视频为固定机位、单一场景</li>
      <li>ffmpeg scenecut 失败，已回退到首帧</li>
    </ul>
    建议：联系管理员调整 scene-detection-threshold 配置
  </Alert>
)}

// 显示场景列表
<Typography variant="subtitle2" gutterBottom>
  场景列表（共 {sceneCount} 个）
</Typography>
{sceneLines.map((line, i) => (
  <Typography key={i} variant="body2" color="text.secondary">
    {line}
  </Typography>
))}
```

### 显示视觉分析状态

```tsx
// 检查是否有视觉分析失败
const hasVisionFailure = deepParsed?.scenes?.some(
  scene => scene.environment?.includes('视觉分析无响应')
);

{hasVisionFailure && (
  <Alert severity="info" sx={{ mb: 2 }}>
    ℹ️ 部分场景的视觉分析未成功
    <br />
    可能原因：多模态模型未配置或调用失败
    <br />
    当前拆解结果基于口播文本和元数据推演
  </Alert>
)}
```

---

## 总结

### 核心问题
1. **场景检测阈值过高**（0.3）导致只检测到首帧
2. **回退逻辑不完善**：首帧 → 整个视频时长 → 只有 1 个场景
3. **多模态模型调用失败**：未配置或 API 异常

### 快速修复
```yaml
# application.yml
app:
  video-analysis:
    scene-detection-threshold: 0.15  # 降低阈值
    scene-max-count: 20
    vision-model-id: 123  # 配置多模态模型
    multi-round-analysis: true
```

### 长期优化
1. 增强场景检测失败时的回退逻辑（动态 FPS）
2. 改进错误日志，输出 ffmpeg 详细信息
3. 前端显示场景检测状态和警告
4. 添加场景检测质量监控指标
