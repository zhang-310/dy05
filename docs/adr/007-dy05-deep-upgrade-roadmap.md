# ADR 007：dy05 深度升级路线（阶段 A–D）与实现落点

## 状态

已采纳（2026-04-15）

## 背景

计划在 Maven 多模块已落地的前提下，分阶段稳定契约、提升 RAG 可观测与 SLO、建立进化适应度与基础设施门闸，并明确规模化时的配额与演练入口。完整叙事见用户侧规划「dy05 深度分析与升级计划」；**本 ADR 仅记录仓库内事实与配置键，避免在 `docs/` 根重复长文。**

## 决策摘要

| 阶段 | 范围 | 本仓库落点 |
|------|------|------------|
| A | JPA/DB 与 API 契约、依赖降级提示、索引队列可追踪 | `ai_kb_document.metadata` 等与 Flyway/sql 对齐；Milvus 用户提示 `MilvusUserHint`；`IndexQueueConsumerServiceImpl` 结构化 error 日志 |
| B | RAG SLO 基准、改写/重排开关、指标 | `KbRagProperties` 绑定 `slo-hybrid-search-p95-ms-target`；`SearchMetricsCollector` 记 `ai.search.slo.hybrid.violation`；Gauge `ai.index.queue.pending` / `ai.index.queue.pending_lag_ms_max` / `ai.index.queue.gauge_scrape_error`（`IndexQueueMetrics` + `AiIndexQueueRepository`） |
| C | 适应度、血缘、门闸 | 表 `evolution_fitness_record`（`V145`）；可选列 `experiment_id`（`V146`）；`ai_evolve_task.ab_experiment_id`（`V147`）；`EvolutionFitnessService.record`；索引消费 `index_succeeded` / `index_failed`（`IndexQueueConsumerServiceImpl`，与进化行共享 `experiment_id`）；只读 `POST .../knowledge-base/{kbId}/evolution-fitness/list`；高分入队前 `EvolutionInfrastructureGate`；执行中 `pending_review` 时不再覆盖为 `completed`、且不发布 `EvolveTaskCompletedEvent`；**人工审核通过/修订通过**（`EvolutionReviewServiceImpl`）将 `pending_review` → `completed` 并发布 `EvolveTaskCompletedEvent`；**拒绝/超时** → `failed` + `errorMessage`，不发布完成事件；可选适应度 `review_approved` / `review_rejected` |
| D | 配额、合规、SLA 演练 | `app.ai.quota.daily-max` → `AiQuotaService` / `ai_call_quota`；与 payment 订阅关系见 **[docs/modules/ai/sla-capacity-runbook.md](../modules/ai/sla-capacity-runbook.md)** |

## 配置键（摘录）

- `app.ai.kb.rag.slo-hybrid-search-p95-ms-target`：混合检索单次耗时超过该阈值（毫秒）时递增 `ai.search.slo.hybrid.violation` 并打 warn 日志（见 `KbRagProperties` / `SearchMetricsCollector`）。
- `app.ai.evolution-fitness.record-enabled`：是否写入 `evolution_fitness_record`。
- `app.ai.evolution-fitness.auto-experiment-from-context`：为 `true` 时从进化主题 `category` 前缀 `ab:{experimentId}` 解析并写入 `ai_evolve_task.ab_experiment_id`，并贯通 `evolve_completed` / 索引回调适应度的 `experiment_id` 与 payload。
- `app.ai.evolution-fitness.auto-merge-enabled`：是否允许自动将高分进化报告推入索引队列（默认 `false`）。
- `app.ai.evolution-fitness.infra-check-enabled`：自动合并前是否调用 `ImportRequirementsService` 校验 ES（及在 `app.milvus.enabled=true` 时校验 Milvus）。
- `app.ai.quota.daily-max`：每用户每日 AI 调用上限（与 payment/产品层配额策略衔接）。

## 后果

- 进化「自动入生产索引」默认关闭；开启后仍受基础设施检查结果约束，避免 ES/Milvus 故障时积压无效队列任务。
- Flyway `V145` / `V146` / `V147` 在开发环境若禁用 Flyway，需在目标库手工执行或短期关闭 `ddl-auto: validate` 直至表/列存在（与项目既有迁移策略一致）。
- 依赖 DAG 的后继任务在「灰区待审」场景下，仅当审核通过或修订通过并完成 `AiEvolveTask` 终态 `completed` 后才会收到 `EvolveTaskCompletedEvent` 解锁；审核拒绝不会误发完成事件。

## 参考

- `docs/BUILD.md`、`docs/SSOT.md`（修订记录）
- `douyin-operations-intelligence`：`KnowledgeBaseServiceImpl`、`IndexQueueConsumerServiceImpl`、`EvolveEngineServiceImpl`
