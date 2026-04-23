# 短视频电影级质量升级 - 实施清单 v3.3 (行业旗舰版)

> 配合主文档: [SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md](./SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md)

---

## 输出规格标准

| 参数 | 抖音竖屏 (默认) | 横屏 (可选) |
|------|-----------------|------------|
| 关键帧图片 | 768x1344 (9:16) | 1344x768 (16:9) |
| 视频分辨率 | 1080x1920 | 1920x1080 |
| 帧率 | 24fps | 24fps |
| 单镜时长 | 5秒 (可选 10秒) | 5秒 |
| 短视频成片 | 15-60秒, 3-12镜 | - |
| 短剧每集 | 60-180秒, 10-30镜 | - |
| 编码 | H.264, CRF 18, preset slow | - |
| 音频 | AAC 128kbps | - |
| 容器 | MP4 (movflags +faststart) | - |

## API 响应时间预期

| 模型 | 5秒视频 | 10秒视频 | 说明 |
|------|---------|---------|------|
| Kling 3.0 | 2-5分钟 | 3-8分钟 | 国内直连, 最长3分钟 |
| Seedance 2.0 | 2-4分钟 | 3-6分钟 | 国内直连, 含音频 |
| MiniMax Hailuo 2.3 | 1-3分钟 | 2-5分钟 | 国内直连 |
| Veo 3.1/3.2 | 2-5分钟 | 3-8分钟 | 海外中转, 含音频, 3.2 支持 4K 原生 |
| Runway Gen-4 Turbo | 30s-2分钟 | 1-3分钟 | 海外中转, 仅 I2V |
| Luma Ray2/3 | 1-3分钟 | 2-5分钟 | 海外中转 |
| Wan 2.6 | 1-3分钟 | 2-4分钟 | 国内直连/自部署 |
| Suno V5 (BGM) | 1-3分钟 | - | BGM 生成 |
| ElevenLabs (SFX) | 5-15秒 | - | 音效生成 |
| ElevenLabs (克隆) | 30-60秒 | - | 声音克隆 |
| HeyGen (数字人) | 3-8分钟 | - | 数字人口播 |
| FFmpeg 后期处理 | 5-15秒 | 10-30秒 | 本地处理 |
| FFmpeg 质量评分 | 3-10秒 | 5-15秒 | 本地处理 |

> 注意: 所有 AI 视频生成均为异步任务 (RabbitMQ 队列)，不阻塞用户操作

---

## Phase 1: 核心升级 - 后端

### 1.1 域模型
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/domain/CameraType.java`
  - 24 种运镜类型枚举，每种包含 code + 中文名 + 英文 prompt 片段
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/domain/QualityLevel.java`
  - FAST_SD(480p), STANDARD_HD(720p), PREMIUM_FHD(1080p), CINEMA_4K(2160p)
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/domain/VideoAspectRatio.java`
  - VERTICAL_9_16, HORIZONTAL_16_9, SQUARE_1_1

### 1.2 Prompt 引擎
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/CinematicPromptEngine.java`
  - `generatePrompt(sceneDescription, cameraType, mood, action, quality)` → 组合 prompt
  - `generateNegativePrompt(quality)` → 负向 prompt (仅 Runway/Pika 使用)
  - `mapMoodToPrompt()` → 中文情绪映射为英文光影描述
  - 不依赖外部 NLP/Vision 服务，纯模板+规则

### 1.3 统一视频接口
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/AiVideoProvider.java`
  - 接口定义: `name()`, `isConfigured()`, `generateVideo(request)`
  - `VideoGenerationRequest` record: imageUrl, endFrameUrl, prompt, negativePrompt, duration, aspectRatio, quality
  - `VideoGenerationResult` record: videoUrl, provider, durationMs, isRemoteUrl

### 1.4 多模型服务
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/MultiModelVideoService.java`
  - `generateWithFallback(request, quality)` → 智能降级
  - 降级链: FHD/4K: Kling → MiniMax → Runway → Luma → Pika
  - Spring 自动注入所有 `AiVideoProvider` 实现
  - 注: v3.2 中由 `IntelligentModelRouter` 升级替代，保留此类作为兼容入口
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/IntelligentModelRouter.java` (v3.2)
  - 内容感知路由: 分析场景 → 选择最优模型
  - 兼容旧版 generateWithFallback 方法

### 1.5 模型提供者实现
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/impl/KlingVideoProvider.java`
  - 适配现有 `KlingVideoServiceImpl`，包装为 `AiVideoProvider`
  - 注意: Kling 不支持 negative_prompt / aspectRatio / seed
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/impl/MiniMaxVideoProvider.java`
  - API: `POST https://api.minimax.io/v1/video_generation`
  - 支持首尾帧: `first_frame_image` + `last_frame_image`
  - model: `MiniMax-Hailuo-2.3`
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/impl/RunwayVideoProvider.java`
  - API: `POST {baseUrl}/image_to_video`
  - 支持 9:16 竖屏 ratio 参数
  - model: `gen4_turbo`
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/impl/LumaVideoProvider.java`
  - API: `POST {baseUrl}/generations`
  - 支持首尾帧 keyframes (frame0 + frame1)

### 1.6 改造现有代码
- [ ] 改造 `ShortVideoMaterialServiceImpl.img2videoBatch()`
  - 注入 `IntelligentModelRouter` (v3.2) + `CinematicPromptEngine`
  - 替换 Kling 直连调用为 `intelligentModelRouter.generateWithSmartRouting()`
  - 删除 `buildMotionPrompt()` 方法，改用 `promptEngine.generatePrompt()`
- [ ] 改造 `ShortVideoMaterialServiceImpl.generateOneKeyframe()`
  - 分辨率 512x512 → 768x1344 (竖屏)
- [ ] 改造 `ShortVideoMaterialController.img2videoBatch()`
  - 新增请求参数: quality, aspectRatio, cameraType, mood, action

### 1.7 数据库 & 配置
- [ ] 执行 SQL: `ALTER TABLE sv_shot ADD COLUMN camera_type, quality_level, ai_model, quality_score`
- [ ] 执行 SQL: `ALTER TABLE sv_material ADD COLUMN post_processing_config, ai_provider`
- [ ] 更新 `application.yml`: 新增 minimax/runway/luma/pika 配置段
- [ ] 更新 `.env`: 新增 MINIMAX_API_KEY, RUNWAY_API_KEY, LUMA_API_KEY, PIKA_API_KEY

---

## Phase 2: 后期处理 + 前端

### 2.1 后期处理
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/VideoPostProcessingService.java`
  - 单通道 FFmpeg: 所有滤镜合并为一条 `-vf` 链
  - 滤镜: hqdn3d(降噪) → unsharp(锐化) → eq(色彩) → lut3d(调色) → pad(黑边)
  - 编码: libx264 CRF 18 preset slow

### 2.2 异步任务队列
- [ ] 创建 `VideoGenerationTaskService.java` (利用现有 RabbitMQ)
  - 提交任务 → 返回任务 ID → 后台 Worker 处理 → WebSocket/SSE 推送进度
  - 支持长时间任务 (30分钟+ 的 4K 批量生成)
  - 任务状态: pending → processing → completed / failed
  - 每个任务可暂停/取消/重试

### 2.3 前端组件
- [ ] 创建 `frontend-react/src/components/shortvideo/CameraControlPanel.tsx`
  - 24 种运镜分 3 类展示 (基础/专业/电影)
- [ ] 创建 `frontend-react/src/components/shortvideo/QualitySelector.tsx`
  - SD/HD/FHD/4K 四档选择
- [ ] 改造 `frontend-react/src/pages/shortvideo/MaterialProductionPage.tsx`
  - 视频生成区添加运镜控制 + 质量选择
  - 传递 cameraType, mood, action, quality, aspectRatio
- [ ] 更新 `frontend-react/src/api/shortvideo.ts`
  - `img2videoBatch` 新增: quality, aspectRatio, cameraType, mood, action
  - 返回新增: aiProvider

### 2.4 分镜微调 UI
- [ ] 改造 `ShotCard.tsx` / `SortableShotCard.tsx`
  - 每个分镜卡片可独立设置: 运镜类型、时长、情绪
  - 点击展开详细编辑面板 (参考文档 12.3 UI 原型)
  - 显示 AI 推荐运镜 (置信度 + 历史评分)
- [ ] 任务进度面板
  - SSE 实时进度 (复用 generateKeyframesWithProgress 机制)
  - 显示当前使用的 AI 模型、预计剩余时间
  - 失败重试按钮

---

## Phase 3: 短剧模块

### 3.1 后端
- [ ] 创建实体类:
  - `SvDrama.java` (短剧: id, title, genre, totalEpisodes, status)
  - `SvDramaEpisode.java` (剧集: dramaId, episodeNumber, projectId, synopsis, cliffhanger)
  - `SvDramaCharacter.java` (角色: dramaId, characterName, referenceImageUrl, voiceId)
- [ ] 创建 Repository 接口:
  - `SvDramaRepository`, `SvDramaEpisodeRepository`, `SvDramaCharacterRepository`
- [ ] 创建 `DramaService.java` 接口 + `DramaServiceImpl.java` 实现
  - CRUD 短剧/剧集/角色
  - AI 生成多集剧本
  - 角色参考图注入到关键帧生成
- [ ] 创建 `DramaController.java`
  - REST API: /api/v1/short-video/drama/*
- [ ] 执行 SQL: 建表 sv_drama, sv_drama_episode, sv_drama_character

### 3.2 前端
- [ ] 创建 `frontend-react/src/pages/shortvideo/DramaEditorPage.tsx`
  - 短剧信息管理 (标题、类型、封面)
  - 角色管理面板 (上传参考图、设置配音音色)
  - 剧集列表 (每集关联项目)
  - 一键生成整部剧本
- [ ] 路由注册 + 侧边栏菜单项

---

## Phase 4: 性能 + 质量

### 4.1 并发优化
- [ ] 创建 `ParallelVideoGenerationService.java`
  - CompletableFuture + Semaphore (3路并发)
  - 10分钟超时

### 4.2 缓存
- [ ] Redis 缓存: `video:` + md5(imageUrl + prompt + quality) → videoUrl, TTL 7天
  - 在 IntelligentModelRouter 中添加 Redis 缓存逻辑

---

## Phase 5: 可视化工作流 + 知识库

### 5.1 可视化工作流编辑器 (前端)
- [ ] 创建 `frontend-react/src/pages/shortvideo/WorkflowEditorPage.tsx`
  - 参考文档 9.2.1
  - 使用 `@xyflow/react` (React Flow v12) 构建节点图
  - 7 个基础工作流节点: 脚本 → 分镜 → 关键帧 → 视频生成 → 后期处理 → 合成 → 发布
  - Phase 8 新增 3 节点: BGM 生成 (musicGen) + 音效生成 (sfxGen) + 数字人 (digitalHuman) = 共 10 节点
  - 双击节点打开 AI 对话框
  - 支持从任意节点开始执行
- [ ] 创建 `frontend-react/src/components/workflow/VideoGenNode.tsx`
  - 参考文档 9.2.2
  - 工作流节点组件 (通用模板，各节点复用)
  - 状态展示: idle / running / completed / error
  - 双击触发 AI 对话
- [ ] 创建以下节点组件 (同 VideoGenNode 结构):
  - `ScriptNode.tsx` (脚本生成节点)
  - `ShotListNode.tsx` (分镜设计节点)
  - `KeyframeNode.tsx` (关键帧生成节点)
  - `PostProcessNode.tsx` (后期处理节点)
  - `ComposeNode.tsx` (合成成片节点)
  - `PublishNode.tsx` (发布评估节点)
- [ ] 创建 `frontend-react/src/components/workflow/AiAssistDialog.tsx`
  - 参考文档 9.2.3
  - 每个节点的 AI 辅助对话框
  - 快捷操作按钮 (按节点类型不同)
  - 对话历史 + 流式响应
  - "重新执行" 按钮

### 5.2 工作流后端
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/WorkflowExecutionService.java`
  - 参考文档 9.2.4
  - RabbitMQ 异步执行工作流
  - `executeFrom(projectId, startStep, params, userId)` → 从指定步骤开始
  - `aiAssistNode(projectId, nodeId, userInput, userId)` → AI 辅助微调
  - 步骤定义: script → shotList → keyframe → videoGen → postProcess → compose → publish
- [ ] 创建 `WorkflowController.java`
  - `POST /api/v1/short-video/workflow/execute` → 执行工作流
  - `POST /api/v1/short-video/workflow/ai-assist` → AI 对话辅助
  - `GET /api/v1/short-video/workflow/status/{projectId}` → 查询状态

### 5.3 运镜/脚本知识库
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/CinematicKnowledgeService.java`
  - 参考文档 10.3
  - `recommendCamera(sceneDescription)` → 场景关键词匹配 + 历史成功率推荐
  - `getBestPresets(cameraType, category)` → 获取最佳 Prompt 预设
  - `logGeneration(...)` → 记录生成结果到知识库
  - `getModelRanking(cameraType, category)` → 模型效果排名

### 5.4 知识库数据库
- [ ] 执行 SQL: 建表 `sv_cinematic_preset` (运镜 Prompt 知识库)
  - 参考文档 10.2
  - 字段: name, category, camera_type, prompt_template, negative_prompt, best_model, success_rate, avg_quality_score, use_count, sample_video_url
- [ ] 执行 SQL: 建表 `sv_generation_log` (生成历史记录)
  - 字段: project_id, shot_id, camera_type, quality_level, prompt, ai_provider, success, quality_score, generation_time_ms
- [ ] 执行 SQL: 建表 `sv_scene_camera_mapping` (场景-运镜推荐)
  - 字段: scene_keyword, recommended_camera, confidence, source(manual/auto)
- [ ] 执行 SQL: 插入初始预设数据
  - 参考文档 10.4
  - 10 条运镜 Prompt 预设 (电影慢推/动作跟踪/纪录片手持等)
  - 12 条场景-运镜映射 (打斗→handheld, 追逐→tracking 等)

### 5.5 前端路由 & 菜单
- [ ] 注册 `/shortvideo/workflow` 路由 → WorkflowEditorPage
- [ ] 侧边栏新增"工作流编辑器"菜单项

---

## Phase 6: 质量评估 + 发布反馈闭环

### 6.1 视频质量自动评分
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/VideoQualityScoreService.java`
  - 参考文档 11.1
  - 5 维评分 (0-100): 清晰度(30%) + 运动流畅度(25%) + 色彩质量(20%) + 噪点水平(15%) + 曝光合理性(10%)
  - 全部基于 FFmpeg 实现，不依赖 GPU/OpenCV
  - `evaluateVideo(videoPath)` → QualityReport (overallScore, grade A+/A/B/C/D, issues, suggestions)
  - FFmpeg signalstats 滤镜提取亮度方差 (清晰度)
  - 帧间 SSIM 分析 (运动流畅度)
  - 色彩饱和度统计 + 直方图分析 (色彩/曝光)

### 6.2 发布后数据回流
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/shortvideo/service/PublishFeedbackService.java`
  - 参考文档 11.2
  - `ContentScore` record: overallScore, completionRate(30%), engagementRate(25%), followerGrowth(20%), viewsVsAvg(15%), durationFit(10%)
  - `analyzePerformance(videoId)` → 分析单个视频发布效果
  - `generateWeeklyReport(userId)` → 周报 (汇总数据 + 对比上周 + AI 建议)
  - `generateReflectionReport(videoId)` → 反思报告 (对比成功视频 + 失败原因分析)
  - `feedbackToKnowledge(videoId, score)` → 写回知识库 (更新成功率/置信度)

### 6.3 质量仪表板 (前端)
- [ ] 创建 `frontend-react/src/pages/shortvideo/QualityDashboardPage.tsx`
  - 参考文档 11.3
  - 概览卡片: 本周发布数 / 平均评分 / 最佳模型
  - 趋势图: 视频质量评分 (近30天)
  - 模型效果排名 (MiniMax vs Kling vs Runway vs Luma)
  - 运镜效果排名
  - 本周 AI 反思 (AI 生成的改进建议)
- [ ] 注册 `/shortvideo/quality-dashboard` 路由
- [ ] 侧边栏新增"质量仪表板"菜单项

### 6.4 知识库闭环集成
- [ ] 改造 `IntelligentModelRouter.generateWithSmartRouting()`
  - 生成完成后调用 `knowledgeService.logGeneration()` 记录结果
  - 生成完成后调用 `qualityScoreService.evaluateVideo()` 自动评分
- [ ] 改造发布流程
  - 发布成功后创建定时任务，定期拉取抖音播放数据
  - 24h / 72h / 7d 三次数据采集
  - 数据采集完成后调用 `feedbackService.feedbackToKnowledge()` 回流

---

## 配置清单

### application.yml 新增

```yaml
app:
  ai:
    minimax:
      api-key: ${MINIMAX_API_KEY:}
      api-url: ${MINIMAX_API_URL:https://api.minimax.io/v1/video_generation}
    runway:
      api-key: ${RUNWAY_API_KEY:}
      api-url: ${RUNWAY_API_URL:https://api.dev.runwayml.com/v1}
    luma:
      api-key: ${LUMA_API_KEY:}
      api-url: ${LUMA_API_URL:https://api.lumalabs.ai/dream-machine/v1}
    pika:
      api-key: ${PIKA_API_KEY:}
      api-url: ${PIKA_API_URL:https://queue.fal.run/fal-ai/pika/v2.2}

  video-analysis:
    ffmpeg-path: ${FFMPEG_PATH:ffmpeg}
    work-dir: ${VIDEO_ANALYSIS_WORK_DIR:/tmp/video-analysis}
    post-processing:
      enabled: true
      default-lut:
      enable-denoising: true
      enable-sharpen: true
```

### .env 新增

```bash
# MiniMax Hailuo
MINIMAX_API_KEY=your_minimax_api_key

# Runway (通过中转)
RUNWAY_API_KEY=your_runway_api_key
RUNWAY_API_URL=https://api.dev.runwayml.com/v1  # 或中转地址

# Luma (通过中转)
LUMA_API_KEY=your_luma_api_key
LUMA_API_URL=https://api.lumalabs.ai/dream-machine/v1  # 或中转地址

# Pika (通过 fal.ai)
PIKA_API_KEY=your_fal_api_key

# v3.2 新增
SEEDANCE_API_KEY=your_seedance_api_key
KLING_API_VERSION=v3
VEO_API_KEY=your_veo_api_key
WAN_API_KEY=your_wan_api_key
SUNO_API_KEY=your_suno_api_key
UDIO_API_KEY=your_udio_api_key
ELEVENLABS_API_KEY=your_elevenlabs_api_key
HEYGEN_API_KEY=your_heygen_api_key
```

---

## 依赖更新

### pom.xml (无新增)

> 当前项目已有: Jackson, HttpClient, Redis, RabbitMQ, FFmpeg 命令行调用
> 不需要额外 Maven 依赖。所有 AI 模型通过 HTTP API 调用。

### package.json 新增

```json
{
  "dependencies": {
    "@xyflow/react": "^12.0.0",
    "@radix-ui/react-dialog": "^1.0.0",
    "@radix-ui/react-popover": "^1.0.0",
    "video.js": "^8.10.0",
    "hls.js": "^1.5.7"
  }
}
```

> `@xyflow/react`: Phase 5 可视化工作流编辑器 (React Flow v12)
> `@radix-ui/*`: Phase 5 AI 对话弹窗组件

---

## 数据库变更汇总

### Phase 1 (ALTER TABLE)
```sql
ALTER TABLE sv_shot ADD COLUMN camera_type VARCHAR(50) DEFAULT 'zoom-in';
ALTER TABLE sv_shot ADD COLUMN quality_level VARCHAR(20) DEFAULT 'premium-fhd';
ALTER TABLE sv_shot ADD COLUMN ai_model VARCHAR(50);
ALTER TABLE sv_shot ADD COLUMN quality_score DECIMAL(5,2);
ALTER TABLE sv_material ADD COLUMN post_processing_config TEXT;
ALTER TABLE sv_material ADD COLUMN ai_provider VARCHAR(50);
```

### Phase 3 (短剧)
```sql
CREATE TABLE sv_drama (...);
CREATE TABLE sv_drama_episode (...);
CREATE TABLE sv_drama_character (...);
```

### Phase 5 (知识库)
```sql
CREATE TABLE sv_cinematic_preset (...);   -- 运镜 Prompt 知识库
CREATE TABLE sv_generation_log (...);     -- 生成历史记录
CREATE TABLE sv_scene_camera_mapping (...); -- 场景-运镜推荐
INSERT INTO sv_cinematic_preset ...;      -- 10 条初始预设
INSERT INTO sv_scene_camera_mapping ...;  -- 12 条场景映射
```

### Phase 7/8 (v3.2 新增)
```sql
-- 角色身份管理
ALTER TABLE sv_drama_character ADD COLUMN reference_images JSON;
ALTER TABLE sv_drama_character ADD COLUMN lora_model_path VARCHAR(500);
ALTER TABLE sv_drama_character ADD COLUMN prompt_tags TEXT;
ALTER TABLE sv_drama_character ADD COLUMN voice_sample_url VARCHAR(500);
ALTER TABLE sv_drama_character ADD COLUMN cloned_voice_id VARCHAR(100);

-- 项目类型
ALTER TABLE sv_project ADD COLUMN project_type VARCHAR(20) DEFAULT 'short_video';

-- 分镜音频
ALTER TABLE sv_shot ADD COLUMN dialogue_text TEXT;
ALTER TABLE sv_shot ADD COLUMN sfx_hints VARCHAR(500);
ALTER TABLE sv_shot ADD COLUMN tts_url VARCHAR(500);
ALTER TABLE sv_shot ADD COLUMN bgm_url VARCHAR(500);

-- 生成日志扩展
ALTER TABLE sv_generation_log ADD COLUMN content_type VARCHAR(50);
ALTER TABLE sv_generation_log ADD COLUMN route_reason VARCHAR(200);
ALTER TABLE sv_generation_log ADD COLUMN cost_cents INT;
ALTER TABLE sv_generation_log ADD COLUMN has_audio BOOLEAN DEFAULT FALSE;
```

> 完整 SQL 语句见主文档: 4.8 (Phase 1/3), 10.2 + 10.4 (Phase 5), 14.4 (Phase 7/8)

---

## Phase 7: 音频全链路 (v3.2 新增)

### 7.1 AI BGM 生成
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/AiMusicProvider.java`
  - 统一接口: generateMusic(request) → MusicGenerationResult
- [ ] 创建 `src/main/java/.../impl/SunoMusicProvider.java`
  - Suno V5 API: 提交 → 轮询 → 返回音乐 URL
- [ ] 创建 `src/main/java/.../impl/UdioMusicProvider.java`
  - 同结构，API 差异: output_url

### 7.2 AI 音效 (SFX)
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/SfxGenerationService.java`
  - 场景关键词提取 → ElevenLabs SFX API → 音效文件
  - 20+ 关键词映射表 (雨/风/打斗/奔跑/城市...)

### 7.3 声音克隆
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/VoiceCloneService.java`
  - cloneVoice(name, audioSample) → voice_id
  - synthesizeWithClonedVoice(voiceId, text) → audio URL
  - 支持 ElevenLabs API

### 7.4 音视频联合生成
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/AudioVideoJointService.java`
  - 智能选择联合/分离管线
  - 联合: Seedance 2.0 / Kling 3.0 / Veo 3.1

### 7.5 智能合成
- [ ] 创建 `src/main/java/cn/gaifan/douyinOperations/module/ai/service/IntelligentComposeService.java`
  - 节奏卡点 (BeatSync): BGM BPM → 节拍点切换
  - 智能转场: 情绪差异 → 转场类型 (溶解/硬切/淡入黑)
  - 张力曲线: LLM 标注情绪强度 → 调整镜头时长
  - 开头钩子: 前3秒高吸引力画面

### 7.6 前端
- [ ] 创建 `frontend-react/src/components/shortvideo/BgmPanel.tsx`
  - AI 生成 / 曲库选择 / 上传 / 不添加
  - 情绪自动推荐风格

### 7.7 改造
- [ ] 改造 `TtsService.java` - 支持克隆 voice_id 合成
- [ ] 改造 `VideoEditServiceImpl.java` - 集成三轨混音 (TTS+BGM+SFX)

---

## Phase 8: 最新模型 + 智能路由 + 扩展能力 (v3.2 新增)

### 8.1 新模型接入
- [ ] 创建 `src/main/java/.../impl/Seedance2VideoProvider.java`
  - 音视频联合生成, 12 参考图, 导演级控制
- [ ] 创建 `src/main/java/.../impl/Kling3VideoProvider.java`
  - v3 API, 3分钟, 多角色原生音频, 跨镜头一致性
- [ ] 创建 `src/main/java/.../impl/VeoVideoProvider.java`
  - 照片级真实感, 业界最佳音效, Google API/中转
- [ ] 创建 `src/main/java/.../impl/WanVideoProvider.java`
  - 阿里 DashScope, $0.05/秒, 低成本批量

### 8.2 智能路由
- [ ] 创建 `IntelligentModelRouter.java` (替代简单降级链)
  - 内容类型关键词分析 (人物/全景/动作/特效/写实/动漫)
  - 内容类型 → 最佳模型映射
  - 知识库数据辅助决策

### 8.3 角色身份管理
- [ ] 创建 `CharacterIdentityService.java`
  - 三层方法: LoRA + 加权 Prompt + 多参考图
  - ComfyUI LoRA 训练管线
- [ ] 创建 `frontend-react/src/components/shortvideo/CharacterIdentityPanel.tsx`

### 8.4 数字人
- [ ] 创建 `DigitalHumanProvider.java` 接口
- [ ] 创建 `HeyGenProvider.java` 实现
- [ ] 创建 `frontend-react/src/components/shortvideo/DigitalHumanRecorder.tsx`

### 8.5 抖音 SEO
- [ ] 创建 `DouyinSeoService.java`
  - generateHashtags / selectBestCovers / optimizePublishTime / abTestTitles

### 8.6 配置 & 数据库
- [ ] 更新 `application.yml`: 新增 seedance/kling-v3/veo/wan/suno/udio/elevenlabs/heygen 配置
- [ ] 更新 `.env`: 新增 SEEDANCE_API_KEY, VEO_API_KEY, WAN_API_KEY, SUNO_API_KEY, ELEVENLABS_API_KEY, HEYGEN_API_KEY
- [ ] 执行 v3.2 SQL 变更 (见主文档 14.4)

---

## 验收标准

### 功能验收
- [ ] 支持 24 种专业运镜类型
- [ ] 支持 4 种质量级别 (SD/HD/FHD/4K)
- [ ] 关键帧默认竖屏 768x1344 (9:16)
- [ ] 多模型智能切换 (至少 2 个模型可用)
- [ ] 所有 AI 任务异步化，不阻塞前端
- [ ] 单通道 FFmpeg 后期处理可用
- [ ] 可视化工作流编辑器可用 (10 节点 + AI 对话)
- [ ] 运镜知识库自动推荐可用
- [ ] 视频质量自动评分可用 (A+~D 五档, FFmpeg 完整实现)
- [ ] 发布后数据回流 + 周报可用

### v3.2 新增功能验收
- [ ] 内容感知智能路由: 不同场景自动选择不同模型
- [ ] AI BGM 生成: Suno/Udio 自动生成匹配 BGM
- [ ] AI 音效: 场景关键词自动提取并生成音效
- [ ] 声音克隆: 角色固定音色 (克隆+合成)
- [ ] 音视频联合生成: Seedance 2.0/Kling 3.0/Veo 3.1 一体化
- [ ] 角色身份管理: 多参考图 + LoRA 训练 + 加权 Prompt
- [ ] 智能合成: 节奏卡点 + 智能转场 + 开头钩子
- [ ] 数字人口播: HeyGen 数字人视频生成
- [ ] 抖音 SEO: 标签/封面/发布时间优化
- [ ] LLM 增强 Prompt: FHD/4K 级别 LLM 精炼 Prompt

### 性能验收
- [ ] 单镜视频生成 < 5分钟 (P95, FHD 级别)
- [ ] 多模型降级成功率 > 95%
- [ ] Redis 缓存命中时秒返回
- [ ] 50镜头批量任务可在后台稳定完成
- [ ] 3路并发生成无资源泄漏

### 短剧验收
- [ ] 角色参考图跨集复用 (三层方法)
- [ ] 固定角色配音音色 (声音克隆)
- [ ] 每集可独立编辑和生成
- [ ] LoRA 训练后角色一致性 > 90%

### 知识库验收
- [ ] 初始 10 条预设 + 12 条场景映射可用
- [ ] 每次生成自动记录到 sv_generation_log
- [ ] 知识库推荐置信度随使用自动调整
- [ ] 场景描述输入后返回推荐运镜方案

### 质量闭环验收
- [ ] 视频生成后自动评分 (FFmpeg 5 维完整实现)
- [ ] 发布后 24h/72h/7d 数据自动采集
- [ ] 周报包含: 汇总 + 对比 + AI 建议
- [ ] 反思报告可针对单个视频生成
- [ ] 评分数据写回知识库优化推荐

---

**文档版本**: v3.3 (行业旗舰版)
**最后更新**: 2026-03-02
