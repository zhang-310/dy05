# dy05 单一事实来源（SSOT）

本文是 **「dy02 → dy05、文档怎么治、代码边界往哪收」** 的叙事总入口。  
**构建命令、Maven 模块树、迁移 P0～P4**：见 **[BUILD.md](./BUILD.md)**。  
**文档地图**：见 **[README.md](./README.md)**。

---

## 1. 文档治理（反碎片化）

| 类型 | 现行位置 | 说明 |
|------|----------|------|
| **索引 + SSOT + 构建** | `docs/README.md`、`docs/SSOT.md`、`docs/BUILD.md` | **唯一**主动维护的说明三角 |
| **历史全文** | `docs/_archive/legacy-from-dy02/` | 自 dy02 迁入，**只读考古**；与代码冲突时以 **代码 + 本三角** 为准 |
| **新模块设计** | `docs/modules/<模块>/` | 仅新增内容写到这里 |
| **新 ADR** | `docs/adr/`（推荐在仓库根 `docs/adr` 新建/维护） | 与归档内旧 ADR 区分：以根下为准 |

**规则**：

- 禁止在 `docs/` 根目录再增加大量平行「升级总览」「复盘」；必要留痕用 **ADR** 或 **短更新 SSOT/BUILD**。
- 需要旧稿时只去 **`_archive/legacy-from-dy02`** 搜索，**不要**把归档内容复制回根目录造成第二套真相。

---

## 2. dy05 技术事实（现行）

- **构建**：根工程 `douyin-operations`（聚合 POM，`packaging=pom`）+ 多个 **`douyin-operations-*`** 子模块；**可执行 Jar 仅 `douyin-operations-app`**（`DouyinOperationsApplication`、Flyway、`application*.yml`）。业务代码按域分布在 **platform / integration / asset / content / intelligence / douyin / payment / live / shortvideo** 等子模块的 `cn.gaifan.douyinOperations.module.*` 下；**`douyin-operations-common`** 承载 `common` 包主体；**`douyin-operations-contract`** 承载契约接口。
- **栈**：Spring Boot 3.3.7、JDK 17、JPA、PostgreSQL、Redis、RabbitMQ、ES，可选 Milvus/Neo4j（详见 **`CLAUDE.md`**）。
- **前端**：`front/`（React + Vite + MUI）。
- **业务包**：`module` 下多域并列；**旧文档「27 模块」与代码可能不一致**（如 **`benchmark`**），以代码与 **`docs/BUILD.md`** 为准。

---

## 3. 重建时要收口的问题

| 问题 | dy05 方向 |
|------|-----------|
| 单 artifact，域边界仅靠包名 | Maven **多模块**，依赖 DAG（见 BUILD.md） |
| `common` 引用 `module.auth`等 | 迁至 **platform** / **intelligence** 或 **contract** 接口，打破环 |
| `module.common` 与根 `common` 同名 | 合并或改名，减少歧义 |
| 短视频竞品 vs 对标两套模型 | 抓取进 **integration** 或共享子模块；产品层统一或显式同步 |
| 文档散、多份「总方案」 | **本 SSOT + README + BUILD**；其余归档 |

---

## 4. Maven 逻辑模块与 Java 包对应（目标态）

与 **[BUILD.md](./BUILD.md)** 一致，摘要如下：

| Maven 模块 | Java 包（包名可跨模块复用，以 JAR 边界为准） |
|--------------------|------------------------|
| `douyin-operations-contract` | API 契约与跨域接口（如 `contract.auth`） |
| `douyin-operations-common` | `common.*`（配置、过滤器、工具、REST 基类等） |
| `douyin-operations-platform` | auth, config, log, system；**`module.common.service`** 下 W-09 运维向服务（压测/缓存/异步/DB 优化）已迁入 |
| `douyin-operations-integration` | douyinapi, wecom, sms, tianapi, guiguiya, messaging（接口与部分实现；**Webhook 业务处理实现**在 intelligence，见 BUILD） |
| `douyin-operations-asset` | storage |
| `douyin-operations-content` | script, copy, **workflow**（定义/仓储/控制器；**工作流执行器**在 live） |
| `douyin-operations-douyin` | douyin；**`DouyinOAuthController`**；**`QueryRewriteServiceImpl`**（账号画像 + AI 配置 + Redis） |
| `douyin-operations-payment` | payment |
| `douyin-operations-live` | **live, product, slangdict**；**`DouyinDataCollector`**、直播向 AI/归因实现；**`WorkflowExecutorImpl`**；GMV 对账依赖 **payment** |
| `douyin-operations-shortvideo` | shortvideo, benchmark, **search**；**`CopyAiController`**（文案 AI 调短视频域服务） |
| `douyin-operations-intelligence` | **ai, agent, abtest, attribution**；**`ViolationWordServiceImpl` / `LlmClientAiServiceImpl`**；**`MessagingWebhookHandlerImpl`**；**`TianApiMaterialImportServiceImpl`**（与 copy/知识库编排） |
| `douyin-operations-app` | **仅入口与跨域 glue**：`DouyinOperationsApplication`、ES/Milvus 等全局配置、Flyway；**`module.dashboard`**（经营驾驶舱聚合）；**`auth/DashboardController`**（与 `module.dashboard` 不同包）；**`SystemServiceImpl`**（依赖 BOS/ES/Milvus 等）；**`LiveSessionShortVideoExportServiceImpl`**、**`LiveCompetitorMonitorBridgeController`** 等桥接 |

---

## 5. 修订记录

| 日期 | 说明 |
|------|------|
| 2026-04-13 | dy05：合并原「00-dy02分析与dy05重建-SSOT」叙事并改路径 |
| 2026-04-13 | 数据库增量以 Flyway（`douyin-operations-app/src/main/resources/db/migration`）为 SSOT；`sql/` 说明见 `sql/README.md` |
| 2026-04-13 | Maven P0：聚合根 POM + `douyin-operations-app` 为可执行模块（业务代码后续已拆至多子模块） |
| 2026-04-13 | P1部分：`douyin-operations-common`；integration 全量迁入待解耦鉴权 |
| 2026-04-13 | 增补 **`douyin-operations-payment`**（`module.payment`）；**`douyin-operations-douyin`** 入表；live 与 payment 关系见 BUILD 依赖图 |
| 2026-04-13 | **`module.benchmark`** 实现已置于 **`douyin-operations-shortvideo`**（与 SSOT 表「shortvideo, benchmark」一致） |
| 2026-04-13 | **`module.product`** 迁入 **`douyin-operations-live`**；**`ProductSearchLlmTool`** 同迁 live，避免 intelligence 与 live 的 Maven 依赖环 |
| 2026-04-13 | **`slangdict`→live**，**`search`→shortvideo**；**`DouyinOAuthController`→douyin**（避免 integration→douyin 与 douyin→integration 成环）、**`DouyinDataCollector`→live**；**`app/module/ai`** 按 live/sv 切片迁出 |
| 2026-04-13 | 文档对齐：多模块落地后 **`QueryRewriteServiceImpl`→douyin**；**`WorkflowExecutorImpl`→live**；**`CopyAiController`→shortvideo**；**Violation/LlmClient/MessagingWebhook/TianApiMaterialImport`→intelligence**；**`module.common` 四服务→platform**；SSOT/BUILD/README/AGENTS/交接说明同步 |
| 2026-04-14 | **Phase 2（审核解锁 DAG + 实验 ID 贯通）**：`EvolutionReviewServiceImpl` 在 `pending_review` 终态与 `EvolveTaskCompletedEvent`；`V147` `ai_evolve_task.ab_experiment_id` 与 `app.ai.evolution-fitness.auto-experiment-from-context`；详见 **[ADR 007](./adr/007-dy05-deep-upgrade-roadmap.md)**。 |
| 2026-04-15 | **深度升级路线（阶段 A–D）** 的单一事实来源为 **[ADR 007](./adr/007-dy05-deep-upgrade-roadmap.md)**：契约与降级、RAG SLO 基准与索引队列指标、进化适应度表与自动合并门闸、配额入口说明；不在 `docs/` 根再增平行「总升级方案」。 |
| 2026-04-15 | **深度升级可执行版**（SLO 绑定、索引队列滞后、适应度 API/索引回调、前端 `KnowledgeBasePage` 适应度 Tab、`experiment_id` V146、演进状态机 `pending_review`）细节已同步修订 **ADR 007**；容量与配额演练见 **[docs/modules/ai/sla-capacity-runbook.md](./modules/ai/sla-capacity-runbook.md)**。 |
