# 爆款深度分析系统升级计划

> 生成时间：2026-03-22
> 目标：全程可视化 + 二创闭环 + 稳定性增强

---

## 当前架构概览

### 后端管线（ViralDeepAnalyzeExecutor.runPipeline）

```
Step 0: 元数据提取（yt-dlp --dump-json / Playwright 兜底）
Step 1: 下载视频（PlaywrightDouyinDownloader）
Step 2: ASR 转写（Whisper / 讯飞）
Step 3: 场景检测 + 抽帧（FFmpeg）
Step 3.5: BOS 上传（视频 + 关键帧 + 封面）
Step 4: LLM 多轮分析（4 轮：Vision → Narrative → Hypotheses → Variables）
Step 5: 评论抓取（Playwright 滚动）
Step 6: 清理本地临时文件
```

### 关键文件

| 文件 | 说明 |
|------|------|
| `module/shortvideo/service/impl/ViralDeepAnalyzeExecutor.java` | 深度分析管线主逻辑 |
| `module/shortvideo/service/impl/ViralMetadataExtractor.java` | yt-dlp + Playwright 双源元数据 |
| `module/shortvideo/service/impl/ViralMetadataPlaywrightExtractor.java` | Playwright 元数据兜底 |
| `module/shortvideo/service/impl/ViralCommentExtractor.java` | Playwright 评论抓取 |
| `module/shortvideo/service/impl/ViralBosUploader.java` | BOS 上传（视频/关键帧/封面） |
| `module/shortvideo/service/ViralVideoDeepAnalysisService.java` | 深度分析 Service 接口 |
| `module/shortvideo/service/impl/ViralVideoDeepAnalysisServiceImpl.java` | 深度分析 Service 实现 |
| `module/shortvideo/service/ViralVideoService.java` | 爆款库 Service |
| `module/shortvideo/controller/ViralVideoController.java` | 爆款库 REST 端点 |
| `module/shortvideo/entity/SvViralVideo.java` | 爆款实体（含 15+ 元数据字段） |
| `module/shortvideo/entity/SvComment.java` | 评论实体（videoSource 区分爆款/普通） |
| `frontend-react/src/pages/shortvideo/ViralLibraryPage.tsx` | 前端爆款库页面 |
| `frontend-react/src/api/sv-material.ts` | 前端爆款 API 接口 |
| `frontend-react/src/api/viral-analysis.ts` | 前端深度分析 API |

### 当前数据库字段（sv_viral_video）

```
-- 基础字段
id, owner_id, video_url, title, platform, category, viral_score,
view_count, like_count, share_count, author_name, cover_url,
analysis_status, analysis_result, remake_status, remake_plan,

-- V109 新增的元数据字段
favorite_count, comment_count, video_duration, description,
author_id, author_followers, music_name, hashtags,
video_bos_key, video_bos_url, keyframe_bos_keys, keyframe_bos_urls,
cover_bos_key, cover_bos_url, metadata_json,

-- 分析结果字段
light_analysis_result, deep_analysis_result,
confirmed_remake_type, deep_analyze_steps
```

### 当前 deepAnalyzeSteps 结构

仅 4 个布尔字段，粒度不足：
```json
{
  "downloaded": true,
  "transcribed": true,
  "sceneDetected": true,
  "llmAnalyzed": true
}
```

### 当前 remakeStatus 状态机

```
0 = 未处理
1 = 已推荐（轻量分析后自动）
2 = 已确认（用户确认二创意向）
3 = 脚本已生成
4 = 拍摄中
5 = 已完成
```

无回退、无超时、无状态变迁历史。

---

## P0 — 核心体验（第 1 批实施）

### UP-01: SSE 实时进度推送

**问题**：前端 3s 轮询 `/deep-analyze/status`，延迟高、体验差。

**方案**：

1. **后端** 新增 SSE 端点：
   - 路径：`POST /api/v1/short-video/viral/deep-analyze-stream`
   - **路径必须含 `-stream`**（绕过 `OperationLogFilter` 的 `ContentCachingResponseWrapper` 缓冲，见 CLAUDE.md SSE 约定）
   - 返回 `SseEmitter`，超时 30 分钟
   - `ViralDeepAnalyzeExecutor.runPipeline()` 每完成一步通过 `SseEmitter.send()` 推送

2. **SSE 事件格式**：
```json
{
  "step": "metadata|download|asr|scene|bos|llm_round1|llm_round2|llm_round3|llm_round4|comments|cleanup",
  "status": "running|done|skipped|failed",
  "progress": 0-100,
  "message": "正在提取元数据...",
  "timestamp": "2026-03-22T10:00:00Z"
}
```

3. **前端**：
   - 使用 `ssePost`（`utils/request.ts` 中已有 SSE 工具）监听事件
   - `ViralPipelineStepper` 实时更新每步状态
   - SSE 断开时自动降级为 3s 轮询（保留现有逻辑作 fallback）

**改动文件**：
- `ViralDeepAnalyzeExecutor.java` — 注入 `SseEmitter`，每步 `send()`
- `ViralVideoController.java` — 新增 `-stream` 端点
- `ViralLibraryPage.tsx` — SSE 监听 + 降级轮询
- `viral-analysis.ts` — 新增 SSE API 函数

---

### UP-02: 细粒度步骤跟踪

**问题**：`deepAnalyzeSteps` 仅 4 个布尔，无法区分 LLM 4 轮各自状态，无法表达 BOS/评论/元数据步骤。

**方案**：

1. **数据库**（Flyway V110）：
```sql
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS deep_analyze_progress TEXT;
-- JSON 格式，替代 deep_analyze_steps 的 4 布尔
```

2. **JSON 结构**：
```json
{
  "currentStep": "llm_round2",
  "totalSteps": 10,
  "completedSteps": 5,
  "startedAt": "2026-03-22T10:00:00Z",
  "steps": {
    "metadata":   { "status": "done",    "at": "...", "detail": "yt-dlp成功，Playwright跳过" },
    "download":   { "status": "done",    "at": "...", "detail": "12.3MB, 15s" },
    "asr":        { "status": "done",    "at": "...", "detail": "340字, Whisper" },
    "scene":      { "status": "done",    "at": "...", "detail": "8帧" },
    "bos":        { "status": "done",    "at": "...", "detail": "10文件已上传" },
    "llm_round1": { "status": "done",    "at": "...", "detail": "视觉结构分析" },
    "llm_round2": { "status": "running", "at": "...", "progress": 60 },
    "llm_round3": { "status": "pending" },
    "llm_round4": { "status": "pending" },
    "comments":   { "status": "pending" }
  },
  "error": null
}
```

3. **后端**：`ViralDeepAnalyzeExecutor` 新增 `updateProgress(viralId, stepName, status, detail)` 方法，每步开始/完成/失败时调用。

4. **前端**：`ViralPipelineStepper` 渲染 `deep_analyze_progress`，每步显示状态图标、耗时、详情。

**改动文件**：
- 新建 `src/main/resources/db/migration/V110__viral_deep_analyze_progress.sql`
- `SvViralVideo.java` — 新增 `deepAnalyzeProgress` 字段
- `ViralDeepAnalyzeExecutor.java` — 新增 `updateProgress()` + 每步调用
- `ViralLibraryPage.tsx` — `ViralPipelineStepper` 适配新结构
- `sv-material.ts` — 接口新增 `deepAnalyzeProgress`

---

### UP-03: LLM 多轮分析部分成功处理

**问题**：4 轮 LLM 分析任一轮失败则整体标 `llmAnalyzed=false`，已完成轮次结果丢失。

**方案**：

1. **`deepAnalysisResult` JSON 改为按轮存储**：
```json
{
  "round1_vision":     { "status": "done",    "result": { "scenes": [...], ... } },
  "round2_narrative":  { "status": "done",    "result": { "hooks": [...], ... } },
  "round3_hypotheses": { "status": "failed",  "error": "API timeout after 60s" },
  "round4_variables":  { "status": "skipped", "reason": "round3 failed" },
  "evidenceLevel": "partial",
  "completedRounds": 2,
  "totalRounds": 4
}
```

2. **后端**：
   - `ViralDeepAnalyzeExecutor` 中 LLM 4 轮改为独立 try-catch
   - 每轮结果立即写入 `deepAnalysisResult`（追加式更新）
   - 前一轮失败不阻塞后续轮次（可配置 `app.viral-analysis.llm-continue-on-failure: true`）
   - `evidenceLevel` 根据完成轮次数和 ASR 数据自动计算

3. **新增重试端点**：
```java
@PostMapping("/deep-analyze/retry-round")
public RESTResult<Map<String, Object>> retryRound(
    @CurrentUserId Long userId,
    @RequestBody Map<String, Object> body) {
    // body: { "id": 123, "round": "round3_hypotheses" }
}
```

4. **前端**：
   - 已完成轮次正常展示结果
   - 失败轮次显示错误信息 + 「重试」按钮
   - 跳过轮次显示原因说明

**改动文件**：
- `ViralDeepAnalyzeExecutor.java` — LLM 轮次独立 try-catch + 追加写入
- `ViralVideoDeepAnalysisServiceImpl.java` — 新增 `retryRound()` 方法
- `ViralVideoController.java` — 新增 `/deep-analyze/retry-round` 端点
- `ViralLibraryPage.tsx` — 按轮展示 + 重试按钮
- `viral-analysis.ts` — 新增 `retryAnalysisRound()` API
- `application.yml` — `app.viral-analysis.llm-continue-on-failure`

---

## P1 — 二创闭环（第 2 批实施）

### UP-04: 二创状态机完善

**问题**：remakeStatus 无回退、无超时、无历史记录。

**方案**：

1. **新建状态变迁日志表**（Flyway V111）：
```sql
CREATE TABLE sv_viral_remake_log (
    id BIGSERIAL PRIMARY KEY,
    viral_video_id BIGINT NOT NULL,
    from_status INTEGER,
    to_status INTEGER NOT NULL,
    operator_id BIGINT,
    remark TEXT,
    create_time TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0
);
CREATE INDEX idx_remake_log_viral ON sv_viral_remake_log(viral_video_id);
```

2. **状态机校验**：
```
合法正向转换：0→1, 1→2, 2→3, 3→4, 4→5
合法回退转换：3→2, 4→3, 2→1
禁止跳跃：0→3, 1→4 等
```

3. **后端**：
   - `ViralVideoService` 新增 `updateRemakeStatus(viralId, targetStatus, userId, remark)` 带状态机校验
   - 新增 `rollbackRemakeStatus(viralId, userId, remark)` 回退到上一状态
   - 每次变迁写入 `sv_viral_remake_log`

4. **新增端点**：
```java
@PostMapping("/remake/update-status")  // { "id": 123, "status": 2, "remark": "确认二创" }
@PostMapping("/remake/rollback")       // { "id": 123, "remark": "脚本不满意，重新确认" }
@PostMapping("/remake/log")            // { "id": 123 } → 返回状态变迁历史
```

5. **前端**：
   - 状态按钮动态显示：当前状态 + 可用的正向/回退操作
   - 状态变迁历史 Timeline 组件
   - 回退时弹出确认对话框 + 备注输入

**改动文件**：
- 新建 `V111__viral_remake_log.sql`
- 新建 `entity/SvViralRemakeLog.java` + `repository/SvViralRemakeLogRepository.java`
- `ViralVideoService.java` + `ViralVideoServiceImpl.java` — 状态机方法
- `ViralVideoController.java` — 3 个新端点
- `ViralLibraryPage.tsx` — 状态按钮 + 历史 Timeline
- `sv-material.ts` — 新增 API

---

### UP-05: 二创变量表 UI

**问题**：LLM Round 4 输出的 `replicableVariables`（可复刻变量表）仅存 JSON，无专门 UI。

**方案**：

1. **新组件 `ViralRemakeVariableTable.tsx`**：
   - 从 `deepAnalysisResult.round4_variables.result.replicableVariables` 解析数据
   - 表格列：变量名 | 原视频值 | 我的适配（可编辑） | 操作难度（Tag） | 优先级（星级）
   - 底部「保存适配方案」按钮 → 写入 `sv_viral_video.remake_variables`（新字段）
   - 底部「生成二创脚本」按钮 → 携带变量表 + 原分析结果调 LLM

2. **数据库**（V111 中一并添加）：
```sql
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS remake_variables TEXT; -- 用户编辑后的变量表 JSON
```

3. **集成位置**：详情弹窗新增「二创变量」Tab

**改动文件**：
- 新建 `frontend-react/src/components/shortvideo/ViralRemakeVariableTable.tsx`
- `ViralLibraryPage.tsx` — 新增 Tab + 集成组件
- `SvViralVideo.java` — 新增 `remakeVariables` 字段
- `sv-material.ts` — 接口新增字段

---

### UP-06: 二创脚本 → 项目管理闭环

**问题**：`replicateViral()` 生成二创方案后没有后续流程。

**方案**：

1. **后端**：
   - `replicateViral()` 执行后自动创建 `SvScriptPlan` 记录（关联 `viralVideoId`）
   - `remakeStatus` 自动推进到 3（脚本已生成）
   - `SvScriptPlan` 新增 `source_viral_id BIGINT` 字段（V111 中添加）

2. **前端**：
   - 二创脚本生成成功后，显示「进入脚本编辑器」按钮
   - 点击跳转 ScriptPlanningPage 并通过 URL 参数预填充 `viralId`
   - ScriptPlanningPage 检测到 `viralId` 时加载二创方案
   - ProjectManagementPage 列表显示「源自爆款」标签（通过 `sourceViralId` 判断）

**改动文件**：
- `ViralVideoServiceImpl.java` — 生成后创建 SvScriptPlan + 推进状态
- `SvScriptPlan.java`（如已有）— 新增 `sourceViralId`
- `ViralLibraryPage.tsx` — 脚本编辑器跳转按钮
- `ScriptPlanningPage.tsx` — URL 参数预填充
- `ProjectManagementPage.tsx` — 爆款来源标签

---

## P2 — 稳定性与体验（第 3 批实施）

### UP-07: BOS 上传失败重试

- `ViralBosUploader` 添加 `@Retryable(maxAttempts=3, backoff=@Backoff(delay=2000))`
- 区分临时错误（网络超时 → 重试）和永久错误（权限不足 → 跳过并标记）
- `deep_analyze_progress.steps.bos` 中记录重试次数和最终状态
- 前端 BOS 上传失败时显示「手动重试上传」按钮

### UP-08: 批量深度分析聚合进度

- 新增 `POST /deep-analyze/batch/status` 端点
- 请求：`{ "ids": [1,2,3,...] }`
- 响应：`{ "total": 10, "completed": 6, "failed": 1, "running": 3, "items": [...] }`
- 前端批量操作后显示整体进度条 + 各条目缩略状态

### UP-09: 评论情感分析

- 评论抓取完成后，批量调 LLM 做情感分析
- `sv_comment` 新增字段：
```sql
ALTER TABLE sv_comment ADD COLUMN IF NOT EXISTS sentiment VARCHAR(16); -- positive/neutral/negative
ALTER TABLE sv_comment ADD COLUMN IF NOT EXISTS key_point TEXT;         -- LLM 提取的关键观点
```
- 前端评论列表：每条评论显示情感 Chip（绿/灰/红）
- 评论区顶部：情感分布饼图 + 关键观点 Word Cloud

### UP-10: 封面多尺寸缩略图

- BOS 上传封面时，使用 BOS 图片处理生成 3 个尺寸：原图、300px、600px
- `cover_bos_url` 改存 JSON：`{ "original": "...", "thumb_300": "...", "thumb_600": "..." }`
- 列表页使用 `thumb_300`，详情页使用 `thumb_600`，放大查看用 `original`

### UP-11: 爆款对比分析

- 新组件 `ViralCompareDialog.tsx`
- 选择 2-3 个爆款视频并排对比
- 对比维度：
  - 互动数据雷达图（播放/点赞/收藏/评论/分享）
  - 内容结构差异（时长/场景数/音乐/标签）
  - 可复刻变量对比表
- 底部「选择此视频二创」快捷按钮

### UP-12: 分析历史版本

- 新建 `sv_viral_analysis_history` 表
- 每次重新分析时，将当前结果归档到历史表
- 前端「分析历史」Tab：时间线展示，可对比不同版本的分析结果差异

---

## 实施顺序与依赖

```
第 1 批（P0 核心体验）—— 建议 2-3 天
├── UP-02 细粒度步骤跟踪（基础，UP-01 依赖）
├── UP-01 SSE 实时进度（依赖 UP-02 的 progress 结构）
└── UP-03 LLM 部分成功（独立）

第 2 批（P1 二创闭环）—— 建议 2-3 天
├── UP-04 状态机完善（基础）
├── UP-05 变量表 UI（依赖 UP-03 的按轮结果结构）
└── UP-06 脚本闭环（依赖 UP-04 的状态推进）

第 3 批（P2 体验增强）—— 按需选做
├── UP-07 BOS 重试（独立）
├── UP-08 批量进度（独立）
├── UP-09 情感分析（依赖评论数据）
├── UP-10 封面缩略图（独立）
├── UP-11 对比分析（独立）
└── UP-12 分析历史（独立）
```

## Flyway 迁移汇总

```sql
-- V110__viral_deep_analyze_progress.sql
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS deep_analyze_progress TEXT;

-- V111__viral_remake_enhancements.sql
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS remake_variables TEXT;
ALTER TABLE sv_script_plan ADD COLUMN IF NOT EXISTS source_viral_id BIGINT;

CREATE TABLE sv_viral_remake_log (
    id BIGSERIAL PRIMARY KEY,
    viral_video_id BIGINT NOT NULL,
    from_status INTEGER,
    to_status INTEGER NOT NULL,
    operator_id BIGINT,
    remark TEXT,
    create_time TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0
);
CREATE INDEX idx_remake_log_viral ON sv_viral_remake_log(viral_video_id);

-- V112__comment_sentiment.sql (P2)
ALTER TABLE sv_comment ADD COLUMN IF NOT EXISTS sentiment VARCHAR(16);
ALTER TABLE sv_comment ADD COLUMN IF NOT EXISTS key_point TEXT;
```

## 配置项汇总

```yaml
app:
  viral-analysis:
    # 已有
    metadata-extraction-enabled: true
    bos-upload-enabled: true
    comment-extraction-enabled: true
    max-comments: 200
    comment-scroll-timeout-ms: 60000
    # UP-03 新增
    llm-continue-on-failure: true        # LLM 某轮失败后是否继续后续轮次
    # UP-07 新增
    bos-upload-max-retries: 3
    bos-upload-retry-delay-ms: 2000
    # UP-09 新增
    comment-sentiment-enabled: false     # P2 评论情感分析（默认关闭）
```
