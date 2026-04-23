# 商品 & 直播模块升级 — 完成度核查与前后端深度分析

> 依据：`PRODUCT-LIVE-UPGRADE-PLAN.md` 全阶段任务  
> 分析日期：2026-03-04

---

## 一、升级完成度总览

| 阶段 | 计划项 | 实现状态 | 说明 |
|------|--------|----------|------|
| **Phase 1** | #2 版本号行锁、#1 批量并行、#5 SSE、#15 乐观锁 | ✅ 代码已实现 | 仅剩 1.4/2.5/4.5 验证未勾选 |
| **Phase 2** | #3 拆分、#4 页面拆分、#6 Prompt、#10 解耦、#8 词库 | ✅ 部分达成 | LiveAiServiceImpl 仍 ~1017 行；LiveScriptBuilderPage 已拆子组件但仍 ~981 行 |
| **Phase 3** | #7 快照、#9 限流、#11 违规、#12–#14、#23/#24 | ✅ 已实现 | 引用快照、Redis 限流、ScriptEditor、人设/时长/情绪/来源 |
| **Phase 4** | #16–#22 版本历史/预设/槽位/评分/配额/ErrorBoundary/防抖 | ✅ 已实现 | 后端+前端版本历史、风格预设页、槽位类型、多维度评分、配额系数、ErrorBoundary、节流 |
| **Phase 5** | #25–#29 JSONB/归因/事件/A/B/智能推荐 | ✅ 已实现 | 迁移就绪、script_usage_log、事件发布、话术风格 A/B、风格推荐 API |

**结论**：功能与架构升级已按计划落地，仅 Phase 1 的 3 项「验证」未在文档中勾选，以及 Phase 2 的「单文件行数」目标未完全达到（见下文）。

---

## 二、后端深度分析

### 2.1 核心分层与依赖

```
Controller 层
  → Service 层（ProductScriptService / LiveAiService / AbTestService / StyleRecommendService）
  → Repository 层（DyProductRepository.findByIdForUpdate / LiveScriptRepository / ScriptUsageLogRepository）
  → Entity（DyProduct @Version、LiveScript referencedScriptSnapshot、ScriptUsageLog ab_experiment_id）
  → 事件：ApplicationEventPublisher → ProductScriptUpdatedEvent / LiveScriptGeneratedEvent
```

- **产品话术**：`ProductScriptServiceImpl` 依赖 `ProductAiService`（由 `LiveProductAiServiceImpl` 实现），不再直接依赖 `LiveAiService`，满足 Phase 2 解耦目标。
- **直播话术**：`LiveAiServiceImpl` 委托 `LivePromptBuilder` 构建 Prompt，引用产品话术时写入 `referenced_script_id` + `referenced_script_snapshot`（Phase 3 快照）。
- **限流**：`ProductScriptRateLimitServiceImpl` 使用 Redis 滑动窗口，Key `script_gen:rate:{userId}`，窗口 60s/5 次，Redis 不可用时回退 Caffeine（Phase 3）。
- **合规**：`ComplianceServiceImpl` 从 `ComplianceWordService` 加载词库（表 `sc_compliance_word`），支持运营配置（Phase 2）。

### 2.2 并发与一致性

| 能力 | 实现位置 | 要点 |
|------|----------|------|
| 版本号原子性 | `DyProductRepository.findByIdForUpdate` + `ProductScriptServiceImpl.saveProductScriptWithLock` | 产品行 `PESSIMISTIC_WRITE`，在事务内先锁产品再写 script，避免并发生成版本号重复 |
| 库存乐观锁 | `DyProduct @Version`，`ProductServiceImpl.updateInventory` 捕获 `OptimisticLockException` | 扣减前 load 实体，保存时由 JPA 递增 version，冲突时重试或返回 |
| 批量并行 | `ProductScriptServiceImpl.generateBatchWithProgress` | `CompletableFuture.runAsync(..., aiTaskExecutor)`，`allOf().join()` 控制完成，进度用 `AtomicInteger` |

### 2.3 话术链路（生成 → 归因 → A/B）

1. **生成**  
   - 产品话术：`ProductScriptController` → `ProductScriptService.save` / `generateMultiStyleScripts` / `generateBatchWithProgress`，内部走 `ProductAiService`（Live 实现）或直接保存。  
   - 直播话术：`LiveAiController.generateFull` / `generate-full-sse` → `LiveAiService.generateFullWithProgress`，槽位内可引用产品话术并写入快照；生成完成后发布 `LiveScriptGeneratedEvent`。

2. **归因**  
   - `ScriptEventListeners.onLiveScriptGenerated` 异步写 `script_usage_log`（script_source=live, script_id, session_id, script_type, user_id）。  
   - 表已扩展 `ab_experiment_id`、`ab_variant_id`，便于后续按 A/B 实验归因。

3. **A/B 话术风格**  
   - `ScriptStyleAbService.assignStyle(ownerId, targetEntityType, targetEntityId, userFingerprint)`：按目标查运行中 `script_style` 实验，随机返回 A/B 变体及 `style_code`，并记录一次 view（`ab_event`）。  
   - `recordConversion` 记录转化，复用现有 `AbTestService.recordEvent`，更新变体 view/click/conversion 计数。  
   - 实验表 `ab_experiment` 支持 `target_entity_type` / `target_entity_id`，变体表 `ab_variant` 支持 `style_code`。

4. **智能风格推荐**  
   - `StyleRecommendService.recommendStyles(ownerId, productId, limit)`：按用户场次查 `live_script`，过滤 `effectiveness_score` 与 `style` 非空（可选按 productId），按 style 聚合平均分与使用量，按效果降序返回。

### 2.4 AI 配额与计费

- `AiCallQuota` 增加 `used_units`（BigDecimal），`AiQuotaService.consume(userId, coefficient)` 按系数累加 used_units 并换算 used_count。  
- 直播全场生成：`LiveAiController` 在 `generateFull` / SSE 流结束后，根据 `LiveAiFullResultVO.consumption` 调用 `aiQuotaService.consume(userId, consumption)`，实现引用 0、模板 0.5、生成 1.0 等细粒度计费。

### 2.5 槽位与效果评分

- `live_slot_type` 表 + `LiveSlotTypeRepository`：`ensureScriptSlotsForSession` 从配置读取槽位类型（含 interaction、promotion 等），`createPlaceholder` 使用 `defaultRequirement`。  
- `LiveScript` 含 `conversion_delta`、`effectiveness_score`；`LiveScriptAttributionServiceImpl` 按 MonitorPoint（viewer/interaction/orders）与权重计算多维度评分。

### 2.6 未完全达标的点

- **LiveAiServiceImpl 行数**：当前约 **1017 行**。计划 5.5 写的是「降至 ~960」，Phase 2 验收写的是「< 400 行」。实际只完成了 Prompt 抽到 `LivePromptBuilder`，SlotManager/Generator/Refiner 未再拆，故行数仍大，若需达标需进一步拆类。  
- **Phase 1 验证**：文档中 1.4（版本号并发）、2.5（批量耗时）、4.5（库存并发）未勾选，建议补自动化或手工验证并更新计划。

---

## 三、前端深度分析

### 3.1 页面与路由

- **直播话术构建**：`LiveScriptBuilderPage`（约 981 行）为容器页，已拆子组件：`ProductPanel`、`ScriptPanel`、`AiChatPanel`、`AiAnalystPanel`、`QualityCheckDialogs`，并从 `./components` 统一导出。  
- **直播场次详情**：`LiveSessionDetailPage` 使用 `generateFull` 同步接口，具备节流（`useThrottledCallback(500)`）。  
- **商品库**：`ProductPage`、`ProductScriptManageDialog`（单品话术管理 + 生成）、`BatchScriptGenerateDialog`（批量生成）、`ScriptVersionHistoryDialog`（历史/对比/回滚）、`StylePresetPage`（风格预设管理）。  
- **路由**：关键路由已包裹 `ErrorBoundary`（如 `live/script`、`live/sessions/:id`、`product`、`product/style-preset` 等），满足 Phase 4。

### 3.2 与升级相关的 API 使用

| 能力 | API / 调用处 | 说明 |
|------|----------------|------|
| 全场生成 SSE | `fetch('/api/v1/live/ai/generate-full-sse', ...)`，`ReadableStream` 解析 SSE | 进度条、槽位名展示（Phase 1 #5） |
| 产品话术版本历史 | `listScriptVersionHistory(scriptId)`、`rollbackToVersion(historyId)` | `ScriptVersionHistoryDialog`（Phase 4 #16） |
| 风格预设 | `listStylePresets()`、风格管理页 | `ProductScriptManageDialog` / `BatchScriptGenerateDialog` 展示说明；`StylePresetPage` 管理（Phase 4 #17） |
| 人设 / 时长 | 生成参数带 `personaId`；多处展示「约 X 秒」 | Phase 3 #13/#14 |
| 统一编辑器 | `ScriptEditor`（`@/components/script/ScriptEditor`） | 在 `ScriptSection` 中使用，支持字数、违规高亮（Phase 3 #12） |
| 节流 | `useThrottledCallback(handle, 500)` | 开场/产品/全场生成、相似度、骨架等（Phase 4 #22） |

### 3.3 数据流与状态

- **LiveScriptBuilderPage**：单页内大量 `useState`（sessions、sessionId、products、scripts、弹窗、编辑态、违规结果等），子组件通过 props 与回调通信。计划 6.6 提到的「Zustand 或 Context 共享 session/scripts」未做，若后续再拆或加功能，可考虑抽成 store 减轻 props。  
- **产品话术**：列表/弹窗内请求 `listScriptVersionHistory`、`listStylePresets`，回滚与生成流程与后端一致。

### 3.4 尚未对接的前端

- **A/B 话术风格**：后端已提供 `POST /api/v1/abtest/script-style/assign`、`/script-style/record-conversion`。前端生成前未调用 assign 获取风格，转化时未调用 record-conversion，若要做「随机风格 + 转化归因」需在生成流程与转化上报处接这两接口。  
- **智能风格推荐**：后端已提供 `POST /api/v1/live/style/recommend`。前端未在生成对话框或推荐位展示「推荐风格」，可在此处接入返回的 `styleCode` 列表做默认选项或推荐标签。  
- **引用快照展示**：计划 10.4「前端展示『引用自产品话术 v3』及快照内容」为可选，当前未发现展示 `referenced_script_snapshot` 的 UI。

---

## 四、数据库与迁移

- **已执行迁移**（按前期对话）：  
  - `sql/abtest/migration-script-style.sql`（script_style 实验类型、target_entity_*、style_code、索引）  
  - `sql/live/migration-script-usage-ab.sql`（script_usage_log 增加 ab_experiment_id、ab_variant_id、索引）  
- **其他迁移**（计划中已存在或已执行）：product version、script_version_history、style_preset、live_slot_type、effectiveness conversion_delta、ai used_units、product style_tags 等，需与当前库表状态一致即可。

---

## 五、总结与建议

- **升级完成度**：功能上 Phase 1～5 的规划项均已实现或迁移就绪；差异主要在「验证勾选」与「单文件行数」两项。  
- **后端**：并发安全（行锁/乐观锁）、批量并行、SSE、事件驱动、归因日志、A/B 与风格推荐、配额细粒度、多维度评分、槽位配置等均已落地；可选项为将 `LiveAiServiceImpl` 再拆到 <400 行。  
- **前端**：SSE 进度、版本历史与回滚、风格预设管理与展示、统一 ScriptEditor、ErrorBoundary、节流均已落地；可选项为接入 A/B assign/record-conversion、风格推荐 API，以及引用快照的展示。  
- **建议**：  
  1. ~~在计划文档中补勾 Phase 1 的 1.4、2.5、4.5~~ → 已补勾并注明「待压测」。  
  2. 若需满足「LiveAiServiceImpl < 400 行」，再拆 SlotManager/Generator/Refiner（可选，未做）。  
  3. ~~产品/直播生成流程中按需接入 A/B 分配与转化上报、风格推荐接口~~ → 已接入：产品话术生成前 assign + 风格推荐「推荐」标签；直播全场生成前 assign；recordConversion API 已暴露供业务调用。
