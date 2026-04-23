# 短视频电影级质量升级 - Cursor AI 使用指南 v3.3

## 文档说明

| 文档 | 用途 |
|------|------|
| [技术升级方案](./SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md) | 完整技术设计，含代码实现 (5800+ 行) |
| [实施清单](./IMPLEMENTATION_CHECKLIST.md) | 任务清单 + 规格标准 + 配置 |
| 本文档 | Cursor AI 执行引导 |

---

## v3.3 相比 v3.2 的改进

1. **模型信息同步**: Runway Gen-4 Turbo (统一替换 Gen-3), Veo 3.1/3.2 (新增 4K 原生), Sora 2 Pro (参考标注)
2. **Suno V5 API 说明**: 明确需第三方中间件接入 (PiAPI/Kie.ai), 补充输出质量和版权信息
3. **前后端对齐**: CameraControlPanel CAMERA_TYPES 补齐 24 种运镜 (新增过肩/缓推/缓拉)
4. **PikaVideoProvider**: 补充完整实现代码 (此前仅有路由引用无 Provider)
5. **VideoGenerationException**: 统一异常类替代分散的 RuntimeException
6. **BPM 检测增强**: analyzeBgmBpm 从硬编码升级为 FFmpeg/aubio 检测 + 降级方案
7. **Kling 3.0 功能补充**: Motion Brush (动作笔刷), 6-cut Storyboard 单次生成
8. **Seedance 2.0 @ 引用系统**: 补充核心差异化功能说明
9. **ElevenLabs Music**: 新增音乐生成能力作为 BGM 备选
10. **HeyGen 计费模型**: 补充 credit 计费和 LiveAvatar 实时交互
11. **实施适配指南**: 新增附录 A (AiChatService 映射、知识库可选注入等 6 项)
12. **v3.3 路线图**: 成本预估、图片超分、批量定时、V2V 风格迁移、Sora 2 预留
13. **命名一致性**: MultiModelVideoService → IntelligentModelRouter 统一 (改造代码/注入/实施计划)
14. **文档行数同步**: README "3000+ 行" → "5800+ 行"
15. **CHECKLIST 修正**: 节点数 7→10 说明、验收标准章节重命名、Runway 版本统一

## v3.2 相比 v3.1 的改进

1. **最新 AI 模型**: Seedance 2.0 (字节, 音视频联合), Kling 3.0 (快手, 3分钟+跨镜头一致), Veo 3.1 (Google, 照片级), Wan 2.6 (阿里, 低成本)
2. **内容感知智能路由**: 分析场景类型自动选择最优模型 (替代简单降级链)
3. **音视频联合生成**: 支持联合模型一体化生成对话+音效+背景音
4. **AI BGM 生成**: Suno V5 / Udio 自动生成匹配场景的 BGM
5. **AI 音效 (SFX)**: 从场景描述自动提取关键词并生成音效 (ElevenLabs)
6. **声音克隆**: 角色固定音色, 5秒样本克隆 (ElevenLabs/CosyVoice2)
7. **角色身份管理**: 三层方法 (LoRA+加权Prompt+多参考图), 90%+ 一致性
8. **智能合成**: 节奏卡点 (BeatSync) + 智能转场 + 张力曲线 + 开头钩子
9. **数字人口播**: HeyGen Avatar IV, 口播类短视频支持
10. **抖音 SEO**: 智能标签/最佳封面/发布时间/A/B 测试
11. **LLM 增强 Prompt**: FHD/4K 级别调用 LLM 精炼为电影级英文 Prompt
12. **FFmpeg 质量评分修复**: 5 个测量函数完整 FFmpeg 实现 (不再返回硬编码值)
13. **工作流扩展**: 新增 BGM/音效/数字人节点 (7→10 节点)
14. **步骤间数据流定义**: 明确每步输入/输出格式

## v3.1 相比 v3.0 的改进

1. **可视化工作流编辑器**: ComfyUI 风格的节点式编辑器 (@xyflow/react)，7 个工作流节点
2. **AI 辅助对话框**: 每个工作流节点可展开 AI 对话框进行微调，含快捷操作
3. **运镜/脚本知识库**: sv_cinematic_preset + sv_generation_log + sv_scene_camera_mapping
4. **智能运镜推荐**: 根据场景描述自动推荐运镜方案 (关键词匹配 + 历史成功率)
5. **视频质量自动评分**: FFmpeg 5 维评分 (清晰度/运动/色彩/噪点/曝光)，A+~D 评级
6. **发布后数据回流**: 抖音数据 → 内容效果评分 → 周报/反思报告 → 写回知识库
7. **质量仪表板**: 模型排名 + 运镜排名 + 趋势图 + AI 反思
8. **产品 UX 完整设计**: 三种使用模式 (快速/工作流/手动)，异步任务管理，分镜级微调
9. **初始预设数据**: 10 条运镜 Prompt 预设 + 12 条场景-运镜映射

## v3.0 相比 v2.0 的改进

1. **修复 Kling API 参数假设**: 明确标注 Kling 仅支持 image_url/duration/prompt/mode/fps，不支持 negative_prompt/seed/aspectRatio
2. **9:16 竖屏适配**: 所有输出默认抖音竖屏，关键帧 768x1344，视频 1080x1920
3. **修复分辨率**: 512x512 → 768x1344
4. **多模型支持**: Kling + MiniMax Hailuo + Runway + Luma + Pika，通过海外中转 API
5. **去除过度设计**: 删除 OpenColorIO、MLT、WebRTC、GPT-4V 等不实际的组件
6. **单通道后期**: FFmpeg 滤镜合并为一条命令，避免多次编解码
7. **短剧模块**: 多集管理、角色一致性、剧情连续性
8. **异步任务**: 长时间 AI 生成通过 RabbitMQ 异步化
9. **完整域模型**: 所有枚举和接口定义完整

---

## 分阶段执行

### Phase 1: 核心升级

```
Cursor AI Prompt:

我需要实施短视频电影级质量升级 Phase 1。

参考文档: docs/SHORT_VIDEO_CINEMATIC_QUALITY_UPGRADE.md 第 4.1-4.3 节

请按顺序创建以下文件:

1. src/main/java/cn/gaifan/douyinOperations/module/ai/domain/CameraType.java
   - 参考文档 4.1.1 的完整代码

2. src/main/java/cn/gaifan/douyinOperations/module/ai/domain/QualityLevel.java
   - 参考文档 4.1.2 的完整代码

3. src/main/java/cn/gaifan/douyinOperations/module/ai/domain/VideoAspectRatio.java
   - 参考文档 4.1.3 的完整代码

4. src/main/java/cn/gaifan/douyinOperations/module/ai/service/CinematicPromptEngine.java
   - 参考文档 4.2 的完整代码

5. src/main/java/cn/gaifan/douyinOperations/module/ai/service/AiVideoProvider.java
   - 参考文档 4.3.1 的完整代码

6. src/main/java/cn/gaifan/douyinOperations/module/ai/service/MultiModelVideoService.java
   - 参考文档 4.3.2 的完整代码
   - 注: v3.2 中由 IntelligentModelRouter 升级替代，此类保留作为兼容入口

请完全按照文档中的代码实现，不要修改。
```

### Phase 1 (续): 模型接入

```
Cursor AI Prompt:

继续 Phase 1，创建 AI 模型提供者:

1. src/main/java/.../impl/KlingVideoProvider.java
   - 参考文档 4.3.3，适配现有 KlingVideoServiceImpl

2. src/main/java/.../impl/MiniMaxVideoProvider.java
   - 参考文档 4.3.4，MiniMax Hailuo API

3. src/main/java/.../impl/RunwayVideoProvider.java
   - 参考文档 4.3.5，Runway Gen-4 Turbo API

4. src/main/java/.../impl/LumaVideoProvider.java
   - 参考文档 4.3.6，Luma Ray API

然后改造现有代码:
5. ShortVideoMaterialServiceImpl - 参考文档 4.5
6. ShortVideoMaterialController - 参考文档 4.7
7. generateOneKeyframe 分辨率 512→768x1344 - 参考文档 4.6

最后更新配置:
8. application.yml - 参考文档 4.9
9. 执行 SQL 变更 - 参考文档 4.8
```

### Phase 2: 后期处理 + 前端

```
Cursor AI Prompt:

实施 Phase 2: 后期处理和前端组件

后端:
1. VideoPostProcessingService.java - 参考文档 4.4
   - 单通道 FFmpeg，所有滤镜合并为一条 -vf 链

前端:
2. CameraControlPanel.tsx - 参考文档 5.1
3. QualitySelector.tsx - 参考文档 5.2
4. 改造 MaterialProductionPage.tsx - 参考文档 5.4
5. 更新 shortvideo.ts API 类型 - 参考文档 5.3
```

### Phase 3: 短剧模块

```
Cursor AI Prompt:

实施 Phase 3: 短剧模块

参考文档第 6 章:

后端:
1. SvDrama.java - 参考 6.2.1
2. SvDramaEpisode.java - 参考 6.2.2
3. SvDramaCharacter.java - 参考 6.2.3
4. Repository 接口 (参考现有 SvShotRepository 风格)
5. DramaService.java 接口 - 参考 6.2.4
6. DramaServiceImpl.java 实现
7. DramaController.java

前端:
8. DramaEditorPage.tsx

SQL:
9. 执行文档 4.8 中的 CREATE TABLE 语句
```

### Phase 4: 性能优化

```
Cursor AI Prompt:

实施 Phase 4: 性能优化

参考文档第 7 章:

1. ParallelVideoGenerationService.java - 参考文档 7.1
   - CompletableFuture + Semaphore 并发控制 (3路并发, 10分钟超时)

2. Redis 缓存 - 参考文档 7.2
   - 在 IntelligentModelRouter 中添加 Redis 缓存逻辑
   - key: "video:" + md5(imageUrl + prompt + quality)
   - TTL: 7天
```

### Phase 5: 可视化工作流 + 知识库

```
Cursor AI Prompt:

实施 Phase 5: 可视化工作流编辑器和运镜知识库

参考文档第 9-10 章。

先安装前端依赖:
npm install @xyflow/react @radix-ui/react-dialog @radix-ui/react-popover

前端 (工作流编辑器):
1. WorkflowEditorPage.tsx - 参考文档 9.2.1
   - 使用 @xyflow/react 构建 7 基础节点工作流 (Phase 8 扩展至 10 节点)
2. VideoGenNode.tsx (通用节点组件) - 参考文档 9.2.2
3. 其他 6 个节点组件 (同 VideoGenNode 结构):
   ScriptNode, ShotListNode, KeyframeNode, PostProcessNode, ComposeNode, PublishNode
4. AiAssistDialog.tsx - 参考文档 9.2.3
   - AI 辅助对话框，每个节点不同的快捷操作
5. 路由注册: /shortvideo/workflow → WorkflowEditorPage

后端 (工作流执行):
6. WorkflowExecutionService.java - 参考文档 9.2.4
   - RabbitMQ 异步执行 + AI 辅助微调
7. WorkflowController.java
   - POST /api/v1/short-video/workflow/execute
   - POST /api/v1/short-video/workflow/ai-assist

后端 (知识库):
8. CinematicKnowledgeService.java - 参考文档 10.3
   - 智能推荐 + 数据积累 + 效果排名

SQL:
9. 建表 sv_cinematic_preset - 参考文档 10.2
10. 建表 sv_generation_log - 参考文档 10.2
11. 建表 sv_scene_camera_mapping - 参考文档 10.2
12. 插入初始预设数据 - 参考文档 10.4

请完全按照文档中的代码实现，不要修改。
```

### Phase 6: 质量评估 + 发布反馈

```
Cursor AI Prompt:

实施 Phase 6: 质量评估与发布反馈闭环

参考文档第 11 章。

后端:
1. VideoQualityScoreService.java - 参考文档 11.1
   - FFmpeg 5 维评分: 清晰度(30%) + 运动(25%) + 色彩(20%) + 噪点(15%) + 曝光(10%)
   - 评级: A+ (90+), A (80+), B (70+), C (60+), D (<60)

2. PublishFeedbackService.java - 参考文档 11.2
   - 内容效果评分: 完播率(30%) + 互动率(25%) + 涨粉率(20%) + 播放量(15%) + 时长适配(10%)
   - 周报、反思报告、知识库回流

3. 改造 IntelligentModelRouter.generateWithSmartRouting()
   - 生成完成后调用 knowledgeService.logGeneration() 记录
   - 生成完成后调用 qualityScoreService.evaluateVideo() 评分

前端:
4. QualityDashboardPage.tsx - 参考文档 11.3
   - 概览卡片 + 趋势图 + 模型排名 + 运镜排名 + AI 反思
5. 路由注册: /shortvideo/quality-dashboard → QualityDashboardPage
```

### Phase 7: 音频全链路 (v3.2 新增)

```
Cursor AI Prompt:

实施 Phase 7: 音频全链路

参考文档第 4.10-4.13 章和第 13 章。

后端 (音乐):
1. AiMusicProvider.java - 参考文档 4.11
2. SunoMusicProvider.java - 参考文档 4.11
3. UdioMusicProvider.java - 同 Suno 结构

后端 (音效+声音克隆):
4. SfxGenerationService.java - 参考文档 4.12
5. VoiceCloneService.java - 参考文档 4.13

后端 (联合生成+智能合成):
6. AudioVideoJointService.java - 参考文档 4.10
7. IntelligentComposeService.java - 参考文档 4.15

前端:
8. BgmPanel.tsx - 参考文档 5.5

改造:
9. TtsService.java - 支持克隆 voice_id
10. VideoEditServiceImpl.java - 三轨混音
```

### Phase 8: 最新模型 + 智能路由 + 扩展能力 (v3.2 新增)

```
Cursor AI Prompt:

实施 Phase 8: 最新模型 + 智能路由 + 扩展能力

参考文档第 4.3.7-4.3.10 章和第 4.14-4.17 章。

新模型接入:
1. Seedance2VideoProvider.java - 参考文档 4.3.7
2. Kling3VideoProvider.java - 参考文档 4.3.8
3. VeoVideoProvider.java - 参考文档 4.3.9
4. WanVideoProvider.java - 参考文档 4.3.10

智能路由:
5. IntelligentModelRouter.java - 参考文档 4.3.2 (替代 MultiModelVideoService 的简单降级链)

角色身份管理:
6. CharacterIdentityService.java - 参考文档 4.14
7. CharacterIdentityPanel.tsx - 参考文档 5.6

数字人:
8. DigitalHumanProvider.java + HeyGenProvider.java - 参考文档 4.16
9. DigitalHumanRecorder.tsx - 参考文档 5.7

抖音 SEO:
10. DouyinSeoService.java - 参考文档 4.17

改造:
11. CinematicPromptEngine.java - LLM 增强模式 (参考文档 4.2)

配置:
12. application.yml - 新增 8 个服务配置 (参考文档 4.9)
13. 执行 v3.2 SQL 变更 (参考文档 14.4)
```

---

## 关键注意事项

### Kling API 限制
当前代码 `KlingVideoServiceImpl.submitTask()` 发送:
```json
{"image_url":"...", "duration":5, "prompt":"...", "mode":"pro", "fps":24}
```
Kling 不支持 negative_prompt、cfg_scale、seed、aspectRatio。视频宽高比由输入图片决定。

### 竖屏优先
所有输出默认 9:16 竖屏 (抖音/TikTok):
- 关键帧: 768x1344
- 视频: 1080x1920

### 异步任务
高质量视频生成可能 2-10 分钟/镜头。必须异步化:
- 前端提交 → 返回任务 ID → 后台 RabbitMQ Worker 处理
- SSE/WebSocket 推送进度 (复用现有 keyframe 进度机制)
- 50 镜头批量任务可在数小时内后台完成

### 海外中转
Runway/Luma/Pika 通过中转 API 访问:
- 配置 `app.ai.runway.api-url` 为中转地址
- 只需改配置，代码不变

### 前端新增依赖
Phase 5 需要安装:
```bash
npm install @xyflow/react @radix-ui/react-dialog @radix-ui/react-popover
```

### 知识库冷启动
Phase 5 提供 10 条初始预设 + 12 条场景映射。随着使用量增长，系统会自动积累数据并优化推荐。

---

## 完整文件清单 (按 Phase)

### Phase 1 - 新建文件 (10)
1. `module/ai/domain/CameraType.java`
2. `module/ai/domain/QualityLevel.java`
3. `module/ai/domain/VideoAspectRatio.java`
4. `module/ai/service/CinematicPromptEngine.java`
5. `module/ai/service/AiVideoProvider.java`
6. `module/ai/service/MultiModelVideoService.java`
7. `module/ai/service/impl/KlingVideoProvider.java`
8. `module/ai/service/impl/MiniMaxVideoProvider.java`
9. `module/ai/service/impl/RunwayVideoProvider.java`
10. `module/ai/service/impl/LumaVideoProvider.java`

### Phase 1 - 改造文件 (3)
11. `ShortVideoMaterialServiceImpl.java` (img2videoBatch + generateOneKeyframe)
12. `ShortVideoMaterialController.java` (img2videoBatch 新参数)
13. `application.yml` (新增 AI 配置)

### Phase 2 - 新建文件 (4)
14. `module/ai/service/VideoPostProcessingService.java`
15. `components/shortvideo/CameraControlPanel.tsx`
16. `components/shortvideo/QualitySelector.tsx`
17. `module/ai/service/VideoGenerationTaskService.java`

### Phase 2 - 改造文件 (3)
18. `MaterialProductionPage.tsx`
19. `shortvideo.ts`
20. `ShotCard.tsx` / `SortableShotCard.tsx`

### Phase 3 - 新建文件 (8)
21. `SvDrama.java`
22. `SvDramaEpisode.java`
23. `SvDramaCharacter.java`
24. `SvDramaRepository.java`
25. `SvDramaEpisodeRepository.java`
26. `SvDramaCharacterRepository.java`
27. `DramaService.java` + `DramaServiceImpl.java`
28. `DramaController.java`
29. `DramaEditorPage.tsx`

### Phase 4 - 新建文件 (1)
30. `ParallelVideoGenerationService.java`

### Phase 5 - 新建文件 (12)
31. `WorkflowEditorPage.tsx`
32. `ScriptNode.tsx`
33. `ShotListNode.tsx`
34. `KeyframeNode.tsx`
35. `VideoGenNode.tsx`
36. `PostProcessNode.tsx`
37. `ComposeNode.tsx`
38. `PublishNode.tsx`
39. `AiAssistDialog.tsx`
40. `WorkflowExecutionService.java`
41. `WorkflowController.java`
42. `CinematicKnowledgeService.java`

### Phase 6 - 新建文件 (3)
43. `VideoQualityScoreService.java`
44. `PublishFeedbackService.java`
45. `QualityDashboardPage.tsx`

### Phase 6 - 改造文件 (1)
46. `IntelligentModelRouter.java` (集成知识库记录 + 质量评分)

### Phase 7 - 新建文件 (v3.2, 8)
47. `AiMusicProvider.java` (BGM 生成接口)
48. `SunoMusicProvider.java` (Suno V5 实现)
49. `UdioMusicProvider.java` (Udio 实现)
50. `SfxGenerationService.java` (音效生成)
51. `VoiceCloneService.java` (声音克隆)
52. `AudioVideoJointService.java` (音视频联合生成)
53. `IntelligentComposeService.java` (智能合成)
54. `BgmPanel.tsx` (BGM 面板)

### Phase 7 - 改造文件 (v3.2, 2)
55. `TtsService.java` (支持克隆 voice_id)
56. `VideoEditServiceImpl.java` (三轨混音)

### Phase 8 - 新建文件 (v3.2, 10)
57. `Seedance2VideoProvider.java` (Seedance 2.0)
58. `Kling3VideoProvider.java` (Kling 3.0)
59. `VeoVideoProvider.java` (Veo 3.1)
60. `WanVideoProvider.java` (Wan 2.6)
61. `IntelligentModelRouter.java` (内容感知路由)
62. `CharacterIdentityService.java` (角色身份管理)
63. `DigitalHumanProvider.java` + `HeyGenProvider.java` (数字人)
64. `DouyinSeoService.java` (抖音 SEO)
65. `CharacterIdentityPanel.tsx` (角色面板)
66. `DigitalHumanRecorder.tsx` (数字人组件)

### Phase 8 - 新增节点 (v3.2, 3)
67. `MusicGenNode.tsx` (BGM 生成节点)
68. `SfxGenNode.tsx` (音效生成节点)
69. `DigitalHumanNode.tsx` (数字人节点)

### Phase 8 - 改造文件 (v3.2, 1)
70. `CinematicPromptEngine.java` (LLM 增强模式)

**总计: 70 个文件 (新建 59 + 改造 11)**

---

**文档版本**: v3.3 (行业旗舰版)
**创建日期**: 2026-03-02
