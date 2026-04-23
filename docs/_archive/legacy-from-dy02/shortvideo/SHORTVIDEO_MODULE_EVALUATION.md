# 短视频模块全面深度评估报告

> 评估日期：2026-03-02 | 评估维度：架构、API、数据模型、业务逻辑、前端、AI、安全与性能

---

## 一、架构与分层

### 1.1 模块结构

```
module/shortvideo/
├── controller/     — 17 个 Controller
├── entity/         — 20+ 个 Entity
├── repository/     — 20+ 个 Repository
├── service/        — 接口 + impl 实现
├── vo/             — SearchVO/SaveVO/VO
├── config/         — VideoGenerationTaskAmqpConfig
└── util/           — ShortVideoPathHelper
```

### 1.2 分层情况

| 层级 | 数量 | 说明 |
|------|------|------|
| Controller | 17 | 按资源拆分，职责清晰 |
| Service 接口 | 15+ | 与业务域对应 |
| ServiceImpl | 15+ | 实现完整 |
| Repository | 20+ | 继承 JpaRepository + JpaSpecificationExecutor |
| Entity | 20+ | 覆盖主要业务实体 |
| VO | 30+ | SearchVO/SaveVO/VO 齐全 |

### 1.3 依赖关系

- **依赖模块**：auth（用户）、ai（生成）、storage（BOS）、douyin（人设/账号）
- **被依赖**：无其他模块直接依赖 shortvideo
- **跨模块调用**：`VideoEditService`、`BosStorageService`、`LlmClient`、`ImageGenerationService`、`TtsService`、`KlingVideoService` 等

### 1.4 问题与建议

| 问题 | 级别 | 说明 |
|------|------|------|
| API 路径不统一 | B | 存在 `/api/v1/short-video/` 与 `/api/v1/shortvideo/` 两种前缀 |
| 工作流任务状态存内存 | **A** | `WorkflowExecutionService` 用 `ConcurrentHashMap` 存任务状态，重启丢失，应改为 Redis/DB |
| 工作流用 new Thread() | **A** | 异步执行未用线程池，建议 `@Async` 或专用 Executor |

---

## 二、API 设计

### 2.1 接口统计

| 资源域 | 路径前缀 | 接口数 | 说明 |
|--------|----------|--------|------|
| Dashboard | /short-video/dashboard | 4 | stats, trend, projects, cost-breakdown |
| Project | /short-video/project | 4 | list, get, save, delete |
| Script | /short-video/script | 6 | list, get, save, delete, generate, analyze-viral |
| ShotList | /short-video/shot-list | 5 | list, get, get-by-script, save, generate |
| Material | /short-video/material | 8 | 关键帧、配音、图生视频、运镜推荐、质量评估等 |
| Edit | /short-video/edit | 2 | auto-compose, generate-subtitles |
| Publish | /short-video/publish | 4 | generate-title, ai-review, douyin, publish |
| Upload | /short-video/upload | 10 | 关键帧、视频、音频、封面、参考图等 |
| Drama | /short-video/drama | 9 | 短剧 CRUD、剧集、角色、脚本生成 |
| Workflow | /short-video/workflow | 3 | execute, status, ai-assist |
| VideoTask | /short-video/video-task | 4 | submit, status, cancel, retry |
| Content/Category/Comment | /shortvideo | 15+ | 内容、分类、评论 CRUD |
| Viral | /shortvideo | 8 | 爆款库 CRUD、collect、replicate、recommended |
| ScriptTemplate | /shortvideo | 6 | search, get, save, delete, use-count, by-scene |
| AI | /shortvideo/ai | 4 | generate-copy, generate-script, generate-title, generate-plan |
| QualityDashboard | /short-video/quality-dashboard | 5 | overview, trend, model-ranking, camera-ranking, ai-reflections |
| Music | /short-video/music | 3 | generate-bgm, providers, generate-sfx |

**合计**：约 90+ 个 API 端点。

### 2.2 REST 规范

- **方法**：统一 POST（含查询），符合项目约定
- **响应体**：`RESTResult<T>`，含 status、message、data、traceId、timestamp
- **路径风格**：`/api/v1/<模块>/<资源>/<动作>`，整体一致

### 2.3 错误码

- **号段**：3200–3299（shortvideo）
- **已定义**：VIDEO_NOT_FOUND(3201)、SYNC_FAILED(3202)、PLAN_NOT_FOUND(3203)、HOT_VIDEO_NOT_FOUND(3204)、SCRIPT_TEMPLATE_NOT_FOUND(3205)、VIDEO_PRODUCE_FAILED(3206)
- **实际使用**：多使用 UNAUTHORIZED、VALIDATION_FAIL、DATA_NOT_FOUND、FORBIDDEN、INTERNAL_ERROR 等通用码，模块专属码使用较少

### 2.4 参数校验

- **@Valid 使用**：SvProjectSaveVO、SvScriptSaveVO、SvScriptTemplateSaveVO、SvCategorySaveVO、SvCommentSaveVO、SvVideoSaveVO
- **手动校验**：多数 Controller 对 userId、id、url 等做 null/blank 检查
- **建议**：更多 SaveVO 使用 @Valid，减少 Controller 内重复校验

---

## 三、数据模型

### 3.1 表结构概览

| 表名 | 说明 | owner_id | deleted | 索引 |
|------|------|----------|---------|------|
| sv_video | 短视频 | ✓ | ✓ | owner+时间、account、douyin_id、viral、category |
| sv_video_data | 每日快照 | — | — | video+date、date |
| sv_comment | 评论 | — | ✓ | video、douyin_id、sentiment |
| sv_plan | 创作方案 | ✓ | ✓ | owner、account、ai、linked_video、publish |
| sv_plan_asset | 方案素材 | — | ✓ | plan、asset_type、version |
| sv_viral_video | 爆款库 | ✓ | ✓ | industry、video、source、viral_score |
| sv_hot_topic | 热点话题 | — | — | status、douyin_id、expiry、category |
| sv_category | 分类 | ✓ | ✓ | owner+name、sort |
| sv_video_generation | 视频生成任务 | ✓ | — | owner、plan、status |
| sv_publish_time_analysis | 发布时间分析 | — | — | account+day+hour、recommend |
| sv_project | 项目 | ✓ | ✓ | — |
| sv_script | 脚本 | ✓ | ✓ | — |
| sv_shot_list | 分镜 | ✓ | ✓ | — |
| sv_shot | 镜头 | — | ✓ | — |
| sv_material | 素材 | ✓ | ✓ | — |
| sv_script_template | 脚本模板 | ✓ | ✓ | — |
| sv_drama | 短剧 | ✓ | ✓ | — |
| sv_drama_episode | 剧集 | — | ✓ | — |
| sv_drama_character | 角色 | — | ✓ | — |
| sv_video_generation_task | 异步任务 | ✓ | — | owner、status、create_time |

### 3.2 数据隔离（owner_id）

- **有 owner_id**：sv_video、sv_plan、sv_viral_video、sv_category、sv_video_generation、sv_project、sv_script、sv_shot_list、sv_material、sv_script_template、sv_drama、sv_video_generation_task
- **Service 层**：SvProjectServiceImpl、DramaServiceImpl、MaterialLibraryServiceImpl、VideoGenerationTaskServiceImpl 等均做 ownerId 校验
- **Repository**：如 `findByOwnerIdAndDeleted`、`findByOwnerIdOrderByCreateTimeDesc` 等，按 owner 过滤

### 3.3 逻辑删除

- 大部分业务表有 `deleted` 字段
- Entity 使用 `@SQLRestriction("deleted = 0")`
- sv_video_data、sv_hot_topic、sv_publish_time_analysis、sv_video_generation 等无 deleted，按设计为不可删除或按时间管理

### 3.4 建议

- sv_viral_video 的 schema.sql 与 Entity 字段不完全一致（如 success_factors、replicable_methods、analysis_report_id 等），需核对
- 部分迁移表（sv_project、sv_script 等）的建表 SQL 需在 schema 中统一

---

## 四、业务逻辑

### 4.1 工作流执行（WorkflowExecutionService）

**步骤**：script → shotList → keyframe → videoGen → postProcess → compose → publish

| 步骤 | 实现状态 | 说明 |
|------|----------|------|
| script | ✅ | 调用 SvScriptService.generate + save，更新 project.scriptId |
| shotList | ✅ | 调用 shotListService.generateWithResult，更新 project.shotListId |
| keyframe | ✅ | 调用 materialService.generateKeyframes |
| videoGen | ✅ | 调用 materialService.img2videoBatch |
| postProcess | ✅ | 直接通过（videoGen 已含后期） |
| compose | ✅ | 调用 VideoEditService.autoCompose，上传 BOS，更新 project.finalVideoUrl |
| publish | ⚠️ | 占位，仅 sleep 2 秒 |

### 4.2 脚本生成

- SvScriptService.generate：支持 type、theme、viralVideoId、productInfo、style、duration
- ShortVideoAiService：generateCopy、generateScript、generateTitles、generateVideoPlan
- 模型配置：`ai_task_model_config.short_video_script`，有 fallback 链

### 4.3 分镜

- SvShotListService.generateWithResult：基于脚本内容 + shotCount + style 生成分镜
- 返回 shotListId 和 shots 列表

### 4.4 视频生成

- **同步**：ShortVideoMaterialServiceImpl.img2videoBatch，支持 Kling、VideoGenerationService
- **异步**：VideoGenerationTaskService.submitTask，RabbitMQ 消费
- **SSE**：generate-keyframes-stream、img2video-batch-stream 支持实时进度

### 4.5 合成流程

- VideoEditService.autoCompose：合并视频、配音、字幕、BGM
- 成片上传 BOS，更新 project.finalVideoUrl、duration

### 4.6 问题

- **aiAssistNode**：固定返回占位文案，未真正调用 AI 或更新参数
- 工作流任务状态存内存，重启丢失
- 使用 new Thread() 而非线程池

---

## 五、前端集成

### 5.1 页面覆盖

| 页面 | 路由 | 功能 |
|------|------|------|
| ShortVideoDashboardPage | /shortvideo/dashboard | 数据看板 |
| QuickGeneratePage | /shortvideo/quick | 快速生成 |
| ProjectManagementPage | /shortvideo/project | 项目管理 |
| ScriptPlanningPage | /shortvideo/script | 脚本策划 |
| ShotListDesignPage | /shortvideo/shot-list | 分镜设计 |
| MaterialPreparationPage | /shortvideo/prepare | 素材准备 |
| MaterialProductionPage | /shortvideo/material | 素材生产 |
| MaterialLibraryPage | /shortvideo/library | 素材库 |
| ViralLibraryPage | /shortvideo/viral | 爆款库 |
| DramaEditorPage | /shortvideo/drama | 短剧编辑 |
| VideoEditingPage | /shortvideo/edit | 视频剪辑 |
| PublishManagementPage | /shortvideo/publish | 发布管理 |
| DataAnalysisPage | /shortvideo/analytics | 数据分析 |
| QualityDashboardPage | /shortvideo/quality-dashboard | 质量仪表板 |
| WorkflowEditorPage | /shortvideo/workflow | 工作流编辑 |

共 15 个专用页面，覆盖创作全流程。

### 5.2 API 调用

- **封装**：`frontend-react/src/api/shortvideo.ts`，约 70+ 个函数
- **baseURL**：`/api/v1`，与后端一致
- **超时**：默认 120s，分镜生成 30 分钟
- **SSE**：generateKeyframesWithProgress、img2videoBatchWithProgress 使用 fetch + AbortController

### 5.3 状态管理

- React Query：用于数据请求与缓存
- Zustand：useUserStore 等
- 无专门 shortvideo store，以页面级 state + React Query 为主

### 5.4 错误处理

- request 拦截器：按 status 判断，401/2001/2003 清 token 并跳转登录
- getErrorMessage：按错误码映射文案
- 各页面通过 try/catch、onError 回调处理错误

### 5.5 路径不一致

- 前端部分接口用 `/short-video/`，部分用 `/shortvideo/`，与后端混用一致，建议统一为 `/short-video/`。

---

## 六、AI 能力

### 6.1 AI 脚本生成

| 能力 | 实现 | 说明 |
|------|------|------|
| 文案生成 | ✅ | ShortVideoAiServiceImpl.generateCopy |
| 脚本生成 | ✅ | generateScript，基于文案 + 人设 + 爆款 |
| 标题生成 | ✅ | generateTitles |
| 制作方案 | ✅ | generateVideoPlan |
| 模型配置 | ✅ | ai_task_model_config.short_video_script，支持主备 |

### 6.2 AI 微调

- 无专门微调逻辑，模型来自 ai 模块配置
- 通过 prompt 注入人设、爆款、风格等上下文

### 6.3 aiAssist 实现

- WorkflowController.aiAssist：调用 `WorkflowExecutionService.aiAssistNode`
- **当前实现**：固定返回 `"已理解您的需求，参数已更新"`，`updatedParams` 为空
- **结论**：占位实现，未接入 LLM 或参数更新逻辑

### 6.4 其他 AI 能力

- 关键帧：ImageGenerationService.textToImage / imageToImage
- 配音：TtsService.textToSpeech
- 图生视频：KlingVideoService、VideoGenerationService
- 运镜推荐：CinematicKnowledgeService（知识库）
- 质量评估：evaluate-video-quality
- BGM/SFX：AiMusicController

---

## 七、安全与性能

### 7.1 权限

- **认证**：Controller 通过 `AuthTokenFilter.getUserId(request)` 获取 userId
- **鉴权**：Service 层校验 ownerId，如 SvProjectServiceImpl、DramaServiceImpl、VideoGenerationTaskServiceImpl
- **跨资源**：DramaServiceImpl.getDrama 校验 drama.ownerId

### 7.2 并发

- 工作流：new Thread() 异步执行，无线程池
- 视频任务：RabbitMQ 异步消费
- **建议**：工作流改为 `@Async` 或专用线程池

### 7.3 分页

- 多数列表接口支持 page、rows
- 分页上限：SvShotListServiceImpl、MaterialLibraryServiceImpl 使用 `Math.min(rows, 100)`
- SvProjectSearchVO、SvScriptSearchVO 等未显式限制 rows，建议统一上限（如 100）

### 7.4 缓存

- 未发现 Redis 等缓存
- 工作流任务状态用内存 Map，应迁移到 Redis/DB

---

## 八、总评与优化迭代建议

### 8.1 评分概览

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构与分层 | 8/10 | 分层清晰，路径与工作流实现有改进空间 |
| API 设计 | 7/10 | 接口完整，路径与错误码可统一 |
| 数据模型 | 8/10 | 表设计合理，owner_id 隔离到位，部分 schema 需对齐 |
| 业务逻辑 | 7/10 | 工作流主链路完整，aiAssist 与任务持久化待完善 |
| 前端集成 | 8/10 | 页面与 API 覆盖好，路径与错误处理可优化 |
| AI 能力 | 7/10 | 文案/脚本/标题/方案已实现，aiAssist 为占位 |
| 安全与性能 | 6/10 | 权限校验到位，分页有上限，工作流与缓存需加强 |

### 8.2 优先改进项（按优先级）

#### P0 — 必须修复

| # | 项 | 说明 |
|---|-----|------|
| 1 | 工作流任务状态持久化 | 用 Redis 或 DB 替代 `ConcurrentHashMap`，重启不丢任务 |
| 2 | 工作流异步执行改造 | 用 `@Async` 或 `ThreadPoolTaskExecutor` 替代 `new Thread()` |

#### P1 — 重要迭代

| # | 项 | 说明 |
|---|-----|------|
| 3 | aiAssistNode 真实实现 | 接入 LLM，解析用户意图，更新 workflowParams 并返回 |
| 4 | 统一 API 路径 | 全部改为 `/api/v1/short-video/`，废弃 `/shortvideo/` |
| 5 | publish 步骤实现 | 发布评估节点从占位改为真实逻辑（标题生成、审核、发布） |

#### P2 — 优化增强

| # | 项 | 说明 |
|---|-----|------|
| 6 | 扩展 3200 段错误码 | 为 shortvideo 专属错误增加 ErrorCode 常量 |
| 7 | 分页 rows 统一上限 | 所有 SearchVO 统一 `rows <= 100` |
| 8 | 工作流进度细化 | getStatus 返回真实进度（按步骤计算 0–100） |
| 9 | schema 与 Entity 对齐 | 核对 sv_viral_video 等表结构 |

#### P3 — 长期规划

| # | 项 | 说明 |
|---|-----|------|
| 10 | 抖音数据同步 | 对接抖音 API，同步视频列表与数据 |
| 11 | 发布到抖音 | 真实发布到抖音平台 |
| 12 | 数据回流 | 发布后数据回写、复盘分析 |
| 13 | 热点缓存 | 热点话题、爆款推荐等可加 Redis 缓存 |

### 8.3 与既有评估的对照

`docs/modules/shortvideo/shortvideo-模块设计文档.md` 中提到的抖音数据同步、发布到抖音、数据回流等问题，本次评估仍成立。本报告更侧重代码实现、架构与工程化维度的评估。

---

**文档版本**：v1.0  
**维护**：Claude
