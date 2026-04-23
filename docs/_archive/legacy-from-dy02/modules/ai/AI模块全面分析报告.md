# AI 模块全面分析报告

> 分析日期：2026-02-28 | 分析范围：后端 module/ai、前端 views/ai、API、文档  
> 更新：2026-02-28 已实施 P0/P1 修复（参数校验、错误提示、getErrorMessage、错误码补充）

---

## 一、代码质量

### 1.1 分层结构 ✅ 良好

| 层级 | 现状 | 说明 |
|------|------|------|
| Controller | 8 个 | 职责清晰：知识库、多媒体、进化引擎、运维、额度、日志等 |
| Service | 18+ 接口 / 16+ 实现 | 业务逻辑下沉到 Service，Controller 薄 |
| Entity/Repository | 14+ 实体 | 符合 JPA 规范，表前缀 `ai_` |
| VO | 3 个 | `VideoCompareRequestVO`、`VideoCompareResultVO`、`InfraSearchVO` |

### 1.2 命名规范 ✅ 基本符合

- 类名：大驼峰（`KnowledgeBaseController`、`AiQuotaServiceImpl`）
- 方法名：动词开头（`createKnowledgeBase`、`triggerViralAnalysis`）
- 错误码：`ErrorCode.AI_QUOTA_EXCEEDED` 等，与文档一致

### 1.3 重复代码 ⚠️ 可优化

- **Controller 层**：`userId` 校验重复出现在每个接口，可抽取 `getUserId(HttpServletRequest)` 或 AOP
- **MediaController**：已抽取 `getUserId()`、`withAiCall()`，模式可推广到其他 Controller
- **EvolutionController**：`AuthTokenFilter.getUserId(request)` 与 `KnowledgeBaseController` 的 `httpRequest.getAttribute("userId")` 来源不一致，建议统一

### 1.4 异常处理 ✅ 基本到位

- 业务异常统一使用 `BusinessException` + `ErrorCode`
- 全局异常处理器会转换为 `RESTResult` 返回
- **问题**：`KnowledgeBaseServiceImpl` 中 `createKnowledgeBase` 失败时软删除知识库并抛 `INTERNAL_ERROR`，但 Vector/ES 创建失败后的回滚逻辑依赖手动 `setDeleted(1)`，若中间抛异常可能留下脏数据

### 1.5 事务边界 ⚠️ 需统一

| 位置 | 现状 | 建议 |
|------|------|------|
| KnowledgeBaseServiceImpl | `@Transactional` | 建议 `@Transactional(rollbackFor = Exception.class)` |
| EvolutionServiceImpl | `@Transactional(rollbackFor = Exception.class)` | ✅ 已正确 |
| AiQuotaServiceImpl | `@Transactional(rollbackFor = Exception.class)` | ✅ 已正确 |

**建议**：所有写操作统一使用 `rollbackFor = Exception.class`，避免 `RuntimeException` 以外的异常不回滚。

---

## 二、边界条件

### 2.1 null / 空值处理 ⚠️ 存在风险

| 位置 | 问题 | 风险 |
|------|------|------|
| `EvolutionController.viralTrigger` | `Long.parseLong(body.get("videoId").toString())` | `videoId` 为 null 时 `NullPointerException` |
| `EvolutionController.liveReviewTrigger` | `Long.parseLong(body.get("sessionId").toString())` | 同上 |
| `KnowledgeBaseController.importFromPath` | 未校验 `sourcePath` 是否为空 | 直接传入 Service，由 Service 抛异常 |
| `KnowledgeBaseController.importFromPathAsync` | 同上 | 同上 |
| `VideoCompareRequestVO` | 无 `@NotNull`、`@NotBlank` 等校验注解 | 仅 `videoCompare` 使用 `@Valid`，其他接口无 VO 校验 |

**建议**：

- 为 `viralTrigger`、`liveReviewTrigger` 等增加 `videoId`/`sessionId` 非空校验，或使用专用 VO + `@Valid`
- `importFromPath` / `importFromPathAsync` 在 Controller 层校验 `sourcePath` 非空

### 2.2 分页上限 ✅ 有约束

- `BasicQueryDto` 定义 `MAX_ROWS = 1000`，`rows` 有 `@Max(1000)` 校验
- **EvolutionController**：`page`、`rows` 从 `Map` 解析，未显式限制 `rows` 上限，建议与 `BasicQueryDto` 对齐，限制 `rows ≤ 1000`

### 2.3 并发安全 ⚠️ 部分场景需关注

- **AiQuotaService.consume**：按用户/日扣减额度，需保证并发下不超扣，需确认实现是否有锁或原子操作
- **ImportJobStore**：多线程创建/更新 job，需确认 `createJob`、`get` 等操作的线程安全
- **KnowledgeBaseServiceImpl.hybridSearch**：使用 Redis 缓存，缓存 key 基于 `kbId + query + topK`，无并发写入冲突

### 2.4 超时与重试 ⚠️ 待完善

- **request.ts**：`timeout: 30000`（30 秒），多媒体生成（图像/视频/TTS）可能超时
- **后端**：调用 Ollama、Milvus、ES 等外部服务时，未见统一超时与重试配置
- **建议**：对 AI 生成类接口单独配置更长超时（如 120s），并考虑异步任务 + 轮询模式

---

## 三、文案对齐

### 3.1 错误提示与文档一致性 ✅ 基本一致

| 错误码 | 后端 message 示例 | 错误码文档 | 前端 error-codes.ts |
|--------|-------------------|------------|----------------------|
| 4001 | "当日 AI 调用额度已用完" | AI 调用额度已用完 | AI 调用额度已用完 ✅ |
| 1001 | "知识库名称不能为空" | 参数校验失败 | 参数校验失败 ✅ |
| 1005 | "知识库不存在" | 数据不存在 | 数据不存在 ✅ |

### 3.2 成功提示 ⚠️ 不统一

- 部分接口返回 `RESTResult.success("删除成功", null)`，部分用 `RESTResult.getSuccess(data)` 无 message
- 前端 `ElMessage.success()` 文案多为硬编码（如「创建成功」「图像生成成功」），与后端 message 未强绑定

### 3.3 前端错误码映射 ⚠️ 未充分使用

- `frontend/src/utils/error-codes.ts` 已定义 `getErrorMessage(code)` 及 4001、4002 等 AI 错误码
- **request 拦截器**：仅使用 `res.message`，未根据 `res.status` 调用 `getErrorMessage(status)` 做兜底
- **建议**：在响应拦截器中，当 `res.message` 为空或为技术性文案时，用 `getErrorMessage(res.status)` 作为用户提示

### 3.4 错误码文档覆盖 ⚠️ 前端缺基础设施段

- 后端 `ErrorCode` 有 4050–4063（Milvus、ES、Redis、RabbitMQ 等）
- `error-codes.ts` 仅覆盖到 4013，缺少 4050–4063
- **建议**：补充 AI 基础设施错误码映射

---

## 四、产品经理立场

### 4.1 用户路径 ✅ 基本清晰

- 知识库：创建 → 上传/导入文档 → 搜索
- 多媒体：选择能力（图像/TTS/视频）→ 填写参数 → 生成 → 查看结果
- 进化引擎：选择视频/场次 → 触发分析 → 查看报告

### 4.2 反馈时机 ⚠️ 可优化

- **加载态**：`v-loading`、`generating` 等有使用，但部分列表加载（如 `loadViralList`）失败时无 `ElMessage` 提示
- **EvolutionEngine.vue**：`loadStats`、`loadViralList`、`loadLiveList` 无 try-catch，API 失败时用户无明确反馈
- **建议**：所有异步请求统一 try-catch，失败时 `ElMessage.error(error?.message || '加载失败')`

### 4.3 降级策略 ⚠️ 部分存在

- **KnowledgeBaseServiceImpl.hybridSearch**：`rerankerService` 可选，无重排时仍可返回向量+关键词融合结果 ✅
- **VectorService / SearchService**：Milvus/ES 不可用时直接抛异常，无降级到纯关键词或纯向量
- **建议**：在文档中明确各依赖不可用时的降级策略（如仅 ES、仅 Milvus、或提示用户）

### 4.4 可观测性 ✅ 有基础

- AI 调用日志：`AiCallLogService` 记录 callType、modelCode、耗时、成功/失败
- 进化任务：`AiViralAnalysis`、`AiLiveReview` 有 status、tokensUsed 等字段
- **建议**：补充 traceId 在日志中的传递，便于前后端联调排查

---

## 五、前端

### 5.1 API 对接 ⚠️ 存在不一致

| 问题 | 说明 |
|------|------|
| 方法不一致 | `ai.ts` 中 `listKnowledgeBases` 使用 `request.get`，与文档一致；`evolution.ts` 使用 `request.post`，符合项目「统一 POST」规范 |
| 响应结构 | `request` 拦截器成功时返回 `res.data`，调用方需适配 `{ list, total }` 等结构；`EvolutionEngine` 使用 `(res as any).data?.list || res.list` 做兼容，说明存在历史差异 |
| 路径 | 知识库为 `/ai/knowledge-base/*`，进化为 `/ai/evolution/*`，与后端一致 ✅ |

### 5.2 错误处理 ⚠️ 不完整

- **request 拦截器**：对 2001、2003 跳转登录，2002、1001 等 `Promise.reject(new Error(res.message))`
- **业务组件**：部分有 try-catch + `ElMessage.error`，部分（如 `loadViralList`）无，错误会向上冒泡
- **4001 额度超限**：拦截器未特殊处理，用户看到的是后端 message，可接受；若需更友好，可对 4001 单独提示「今日额度已用完，请明日再试」

### 5.3 加载态 ✅ 基本覆盖

- `v-loading`、`generating`、`docImporting`、`evolveStatusLoading` 等有使用
- 列表、表单提交均有 loading 状态

### 5.4 空态 ✅ 有使用

- `el-empty`：知识库列表、进化概览图表、报告内容等
- 文案如「暂无知识库，点击右上角创建」「暂无评分数据」等

### 5.5 权限控制 ✅ 有配置

- 路由 `meta.roles`：`admin`、`institution`、`talent` 等
- 知识库、多媒体、进化引擎等按角色配置，与后端鉴权配合

### 5.6 i18n 覆盖 ❌ 未使用

- `frontend/src/views/ai/` 下无 `i18n`、`t()`、`useI18n` 等调用
- 所有文案为中文硬编码，若未来需要多语言，需引入 i18n 并替换

---

## 六、Controller 参数校验专项

### 6.1 使用 @Valid 的接口

| 接口 | VO | 校验 |
|------|-----|------|
| `EvolutionController.videoCompare` | `VideoCompareRequestVO` | `@Valid @RequestBody` ✅ |

### 6.2 使用 Map 且手动校验的接口

| 接口 | 校验内容 |
|------|----------|
| `KnowledgeBaseController.createKnowledgeBase` | name 非空 |
| `KnowledgeBaseController.uploadDocument` | title、content 非空 |
| `KnowledgeBaseController.search` | query 非空 |
| `KnowledgeBaseController.importFromPath` | 无 Controller 层校验 |
| `EvolutionController.viralTrigger` | 无，直接 `Long.parseLong` |
| `EvolutionController.liveReviewTrigger` | 无，直接 `Long.parseLong` |

### 6.3 使用 Record 的接口（MediaController）

- `ImageGenerationService.TextToImageRequest` 等为 Java Record，无校验注解
- 若 prompt 为空，由 Service 层或下游抛错，建议在 Record 或 Controller 层做非空校验

---

## 七、Service 层业务边界

### 7.1 数据隔离 ✅ 到位

- `KnowledgeBaseService`：所有操作按 `userId` 过滤，`owner_id` 校验
- `EvolutionService`：`searchViralAnalysis`、`searchLiveReviews` 按 `ownerId` 过滤
- `getViralAnalysis`、`getLiveReview`：未显式校验 `ownerId`，若 id 可被猜测，存在越权风险，建议补充 owner 校验

### 7.2 业务校验 ✅ 基本完整

- 知识库/文档：存在性、归属校验
- 爆款拆解：视频存在性、重复触发（已有时返回原 id）
- 额度：`ensureQuota` 超限抛 `AI_QUOTA_EXCEEDED`

---

## 八、改进建议汇总

| 优先级 | 类别 | 建议 |
|--------|------|------|
| P0 | 边界条件 | `EvolutionController.viralTrigger` / `liveReviewTrigger` 增加 videoId/sessionId 非空校验，避免 NPE |
| P0 | 前端 | `EvolutionEngine` 的 `loadStats`、`loadViralList`、`loadLiveList` 增加 try-catch 与错误提示 |
| P1 | 参数校验 | 为知识库创建、文档上传、导入等接口定义 VO + `@Valid`，替代 Map + 手动校验 |
| P1 | 事务 | 统一写操作使用 `@Transactional(rollbackFor = Exception.class)` |
| P1 | 文案 | 在 request 拦截器中集成 `getErrorMessage(status)` 作为 message 兜底 |
| P2 | 错误码 | `error-codes.ts` 补充 4050–4063（AI 基础设施） |
| P2 | 分页 | EvolutionController 的 rows 限制为 ≤1000 |
| P2 | 超时 | 多媒体生成类接口配置更长超时或改为异步任务 |
| P3 | i18n | 若需多语言，为 AI 模块引入 i18n |
| P3 | 降级 | 文档化 Milvus/ES 不可用时的降级策略 |

---

## 九、文件清单（分析范围）

### 后端

- Controller：`AiController`、`KnowledgeBaseController`、`MediaController`、`EvolutionController`、`EvolveController`、`AiCallLogController`、`AiQuotaController`、`AiAdminInfraController`
- Service：`KnowledgeBaseService`、`EvolutionService`、`AiQuotaService`、`ImageGenerationService`、`TtsService`、`VideoEditService`、`VideoGenerationService` 等
- Entity：`AiKnowledgeBase`、`AiKbDocument`、`AiViralAnalysis`、`AiLiveReview` 等

### 前端

- 视图：`KnowledgeBase.vue`、`MediaStudio.vue`、`EvolutionEngine.vue`、`ModelManagement.vue`、`PromptTemplate.vue`、`TaskList.vue`、`InfraManagement.vue`
- API：`ai.ts`、`evolution.ts`
- 工具：`request.ts`、`error-codes.ts`

### 文档

- `docs/modules/ai/`：00–14 号文档、ai-模块设计文档、接口实现对照表等
