# AI 模块：容量与依赖 SLA 演练 Runbook（阶段 D）

本文档供运维/研发在发布前或季度演练使用，与 [ADR 007](../../adr/007-dy05-deep-upgrade-roadmap.md) 阶段 D 对齐。

## 1. 依赖基线

| 依赖 | 默认端口 | 健康检查参考 |
|------|-----------|----------------|
| PostgreSQL | 5433 | `/actuator/health` 含 db |
| Redis | 6380 | 业务缓存 |
| Elasticsearch | 9200 | `ImportRequirementsService`、混合检索全文 |
| Milvus | 19530 | 可选；`app.milvus.enabled=false` 时可关 |
| RabbitMQ | 5672 | 索引入队通知等 |

本地一键：`docker compose -f docker/docker-compose.yml up -d postgres redis rabbitmq elasticsearch`（按需加 Milvus）。

## 2. 可观测指标（Prometheus）

- `ai.search.duration` / `ai.search.total` / `ai.search.slo.hybrid.violation`：混合检索延迟与 SLO 超标次数（阈值 `app.ai.kb.rag.slo-hybrid-search-p95-ms-target`）。
- `ai.index.queue.pending` / `ai.index.queue.pending_lag_ms_max` / `ai.index.queue.gauge_scrape_error`：索引队列积压与最老任务等待时长。

## 3. 配额与成本（应用层）

- 配置：`app.ai.quota.daily-max`（环境变量 `AI_QUOTA_DAILY_MAX`）。
- 实现：`AiQuotaService` / `ai_call_quota` 表；管理端 `POST /api/v1/ai/admin/quota/*`。
- **与 payment 关系**：订阅/套餐侧配额以 `douyin-operations-payment` 的 `SubscriptionController` 等为准；AI 日配额为 intelligence 侧独立计数，产品层如需统一需在集成层做映射（本 Runbook 仅记入口）。

## 4. 建议演练步骤（每季度或发版前）

1. 冷启动应用，确认 `ddl-auto: validate` 与 Flyway 迁移与目标库一致。
2. 执行一次知识库混合检索与索引队列消费，观察上述指标无异常尖峰。
3. 模拟 ES 或 Milvus 不可用：确认用户可见错误与门闸行为（`EvolutionInfrastructureGate`、Milvus 提示）。
4. 记录演练时间、参与人、发现问题与工单链接。

## 5. 修订

- 与代码不一致时以代码与 ADR 007 为准；修订本文件时可在 `docs/SSOT.md` 修订记录中加一行链接。
