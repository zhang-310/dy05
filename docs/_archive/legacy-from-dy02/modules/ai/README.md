# AI 模块

## 模块概述

AI 模块是平台的核心智能引擎，提供知识库管理、RAG 检索增强生成、LLM 调用管理、知识自进化引擎、爆款分析、数字人等能力。

**第三方推理**：火山引擎火山方舟（OpenAI 兼容 Chat）对接说明见 [`docs/ai/00-火山方舟对接.md`](../ai/00-火山方舟对接.md)（与 `LlmClient` / `ai_model` / Flyway V065 对齐）。

## 行业大脑（Industry Brain）

面向**护肤/彩妆带货**的认知与决策能力层：因果推理、知识图谱、趋势、账号诊断、战略规划、风险预警、主播人设与协同等。与 [`docs/development/04-文档代码同步清单.md`](../development/04-文档代码同步清单.md) 中 **因果引擎、业务范围、合规** 等约定一致。

### API 与前端

| 项 | 说明 |
|----|------|
| 基路径 | `POST /api/v1/ai/brain/*`（统一 POST，见 ADR 001） |
| Controller | `IndustryBrainController.java` |
| 前端 API | `frontend-react/src/api/brain.ts`（含知识图谱、因果、趋势、诊断、战略、风险、增长路径、协同等） |
| 页面 | `IndustryBrainPage` → `/admin/ai/brain`；`IndustryBrainDiagnosisPage` → `/admin/ai/diagnosis` |
| 直播联动 | `TrendingTopicsPanel` 等调用 `brainTrendsCurrent` / `brainTrendsForHost` |

### 配置（`application.yml`）

总开关与子能力均在 `app.ai.brain` 下，可通过环境变量覆盖，例如：

| 配置键 / 环境变量 | 作用 |
|-------------------|------|
| `app.ai.brain.enabled` / `AI_BRAIN_ENABLED` | 总开关 |
| `app.ai.brain.knowledge-graph.neo4j-uri` / `NEO4J_URI` | 设置后优先 Neo4j；否则知识库/JPA 图回退 |
| `app.ai.brain.causal-engine` / `AI_BRAIN_CAUSAL_ENABLED` | 因果引擎 |
| `app.ai.brain.account-diagnosis` / `AI_BRAIN_DIAGNOSIS_ENABLED` | 账号诊断 |
| `app.ai.brain.strategic-planning` / `AI_BRAIN_STRATEGIC_ENABLED` | 战略规划（LLM+数据+模板） |
| `app.ai.brain.risk-warning` / `AI_BRAIN_RISK_ENABLED` | 风险预警（含合规词库） |

完整键名见根目录 `application.yml` `app.ai.brain` 段。

### 服务实现（`service/brain` + `service/impl/brain`）

| 服务 | 说明 |
|------|------|
| `IndustryCausalEngine` | 贝叶斯 + `BusinessParamConfig` 因子 + 主播因子 + 效果自适应 + 可选 LLM 融合 |
| `IndustryKnowledgeGraphService` | Neo4j / KB / `AiGraphNode`·`AiGraphEdge` |
| `TrendMonitorService` | 趋势信号 |
| `UserCognitiveProfileService` | `ai_user_cognitive_profile` + 直播行为聚合 |
| `AccountDiagnosisService` | 抖音账号、短视频、效果等多源诊断 |
| `StrategicPlanningService` | LLM + 上下文 + `skincare` 等模板 |
| `GrowthPathService` | 委托战略或默认三阶段 |
| `RiskWarningService` | 内容扫描 + `ComplianceWordService` |
| `HostPersonaService` / `HostStyleConsistencyService` | 人设、风格向量相似度 |
| `FiveHostsSynergyService` / `IpGrowthStageService` | 五主播协同、IP 阶段 |

升级路线与**代码对齐后的现状表**见 [`docs/upgrade-plan-brain/00-最强大脑升级方案总览.md`](../upgrade-plan-brain/00-最强大脑升级方案总览.md)。

## 核心架构

```
                    ┌──────────────┐
                    │   用户查询   │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  查询改写    │ QueryRewriteService
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │                         │
       ┌──────▼──────┐          ┌──────▼──────┐
       │ Elasticsearch│          │   Milvus    │
       │  BM25 检索   │          │  向量检索   │
       └──────┬──────┘          └──────┬──────┘
              │                         │
              └────────────┬────────────┘
                           │
                    ┌──────▼───────┐
                    │  RRF 融合    │ KbHybridRetrieveService
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │ Cross-Encoder│ 重排
                    │   重排序     │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  LLM 生成    │ LlmClient
                    └──────────────┘
```

## 后端结构

```
module/ai/
├── config/                                   # 配置与调度
│   ├── AiCircuitBreakerConfig.java           #   熔断器配置
│   ├── AiRuntimeConfig.java                  #   运行时配置
│   ├── ImportJobStore.java                   #   导入任务存储
│   ├── IndexQueueAmqpConfig.java             #   索引队列 AMQP 配置
│   ├── IndexQueueAmqpConsumer.java           #   索引队列消费者
│   ├── KnowledgeBaseInitializer.java         #   知识库初始化
│   ├── SearchMetricsCollector.java           #   搜索指标采集
│   ├── VectorCacheWarmup.java                #   向量缓存预热
│   ├── IndustryBrainNeo4jConfig.java         #   行业大脑 Neo4j（可选）
│   ├── EvolveScheduler.java                  #   进化调度
│   ├── FreshnessCheckScheduler.java          #   时效性检查
│   ├── ColdDocDetectionScheduler.java        #   冷文档检测
│   ├── CrossDomainShareScheduler.java        #   跨域共享
│   ├── DeepEvolveScheduler.java              #   深度进化
│   ├── KnowledgeEvolutionScheduler.java      #   知识演化
│   ├── LiveScriptToKbImportScheduler.java    #   话术入库
│   ├── AiViralDetectionScheduler.java        #   爆款检测
│   └── DualWriteCompensationScheduler.java   #   双写补偿
│
├── controller/
│   ├── AiController.java                     #   AI 生成（文本/图片/TTS）
│   ├── KnowledgeBaseController.java          #   知识库 CRUD
│   ├── KnowledgeSourceController.java        #   知识源管理
│   ├── KnowledgeEvolutionController.java     #   知识演化
│   ├── EvolutionController.java              #   进化机会/报告
│   ├── EvolveController.java                 #   进化任务/主题
│   ├── IndustryBrainController.java          #   行业大脑
│   ├── DigitalHumanController.java           #   数字人
│   ├── PromptTemplateController.java         #   提示词模板
│   ├── MediaController.java                  #   媒体（图片/音频/视频）
│   ├── AiDashboardController.java            #   AI 仪表盘
│   ├── AiCallLogController.java              #   调用日志
│   ├── AiAdminCallLogController.java         #   管理端调用日志
│   ├── AiAdminInfraController.java           #   基础设施管理
│   ├── AiQuotaController.java                #   配额管理
│   └── TaskModelConfigController.java        #   任务模型配置
│
├── entity/（28 个实体）
│   ├── AiKnowledgeBase.java                  #   知识库
│   ├── AiKbDocument.java                     #   知识库文档
│   ├── AiKnowledgeSource.java                #   知识源
│   ├── AiCallLog.java                        #   调用日志
│   ├── AiCallQuota.java                      #   调用配额
│   ├── AiModel.java                          #   模型配置
│   ├── AiModelBenchmark.java                 #   模型基准测试
│   ├── AiPromptTemplate.java                 #   提示词模板
│   ├── AiEvolveTopic.java                    #   进化主题
│   ├── AiEvolveTask.java                     #   进化任务
│   ├── AiEvolveReport.java                   #   进化报告
│   ├── AiHostPersona.java                    #   主播人设
│   ├── AiGenerationTask.java                 #   生成任务
│   ├── AiImageGeneration.java                #   图片生成
│   ├── AiTtsGeneration.java                  #   TTS 生成
│   ├── AiViralAnalysis.java                  #   爆款分析
│   ├── AiIndexQueue.java                     #   索引队列
│   ├── AiQueryLog.java                       #   查询日志
│   ├── AiTaskModelConfig.java                #   任务模型映射
│   ├── AiLiveReview.java                     #   直播复盘
│   ├── AiEvolvePendingDeepen.java            #   待深化进化
│   ├── KnowledgeQualityScore.java            #   质量评分
│   ├── KnowledgeEvolutionLog.java            #   演化日志
│   ├── KnowledgeDeduplicationGroup.java      #   去重组
│   ├── EvolutionRule.java                    #   进化规则
│   ├── KbFeedback.java                       #   反馈
│   ├── KbImportCheckpoint.java               #   导入检查点
│   └── KbImportReport.java                   #   导入报告
│
├── service/（50+ 个服务）
│   ├── RagService.java                       #   RAG 检索增强生成
│   ├── LlmClient.java                        #   LLM 调用
│   ├── VectorService.java                    #   向量操作
│   ├── KbHybridRetrieveService.java          #   混合检索
│   ├── KbSearchCacheService.java             #   搜索缓存
│   ├── QueryRewriteService.java              #   查询改写
│   ├── KnowledgeBaseService.java             #   知识库管理
│   ├── KnowledgeBaseImportService.java       #   知识库导入
│   ├── KnowledgeEvolutionService.java        #   知识演化
│   ├── KnowledgeQualityScoringService.java   #   质量评分
│   ├── EvolveEngineService.java              #   进化引擎
│   ├── EvolutionReportService.java           #   进化报告
│   ├── EvolutionRuleEngineService.java       #   进化规则引擎
│   ├── EvolutionStrategyService.java         #   进化策略
│   ├── DeepEvolveService.java                #   深度进化
│   ├── ColdDocDetectionService.java          #   冷文档检测
│   ├── CrossDomainShareService.java          #   跨域共享
│   ├── ExpiryUpdateService.java              #   时效更新
│   ├── PromptTemplateService.java            #   提示词模板
│   ├── ModelChatStreamService.java           #   模型流式对话
│   ├── IntelligentModelRouter.java           #   智能模型路由
│   ├── IntelligentComposeService.java        #   智能编排
│   ├── CharacterIdentityService.java         #   角色身份
│   ├── ContentEffectivenessService.java      #   内容效果
│   ├── ModelBenchmarkService.java            #   模型基准测试
│   ├── AiQuotaService.java                   #   配额管理
│   ├── TaskModelConfigService.java           #   任务模型配置
│   ├── LiveScriptToKbImportService.java      #   话术入库
│   ├── TtsService.java                       #   TTS 语音合成
│   ├── VoiceCloneService.java                #   声音克隆
│   ├── VideoEditService.java                 #   视频编辑
│   ├── KlingVideoService.java                #   可灵视频
│   └── IndexQueueAmqpPublisher.java          #   索引队列发布
│
├── service/brain/ + service/impl/brain/     #   行业大脑（因果/图谱/诊断/战略/风险等）
│
└── vo/
    ├── KbCreateVO / KbSearchVO               #   知识库
    ├── KbDocumentSearchVO / UploadVO         #   文档
    ├── RagRetrieveItemVO                     #   RAG 检索结果
    ├── EvolutionReportVO / OpportunityVO     #   进化
    ├── CallLogSearchVO                       #   调用日志
    └── VideoCompareRequestVO / ResultVO      #   视频对比
```

## 数据库表

| 表名 | 说明 |
|------|------|
| ai_knowledge_base | 知识库 |
| ai_kb_document | 知识库文档 |
| ai_knowledge_source | 知识源 |
| ai_call_log | AI 调用日志 |
| ai_call_quota | 调用配额 |
| ai_model | 模型配置 |
| ai_model_benchmark | 模型基准测试 |
| ai_prompt_template | 提示词模板 |
| ai_evolve_topic | 进化主题 |
| ai_evolve_task | 进化任务 |
| ai_evolve_report | 进化报告 |
| ai_evolve_pending_deepen | 待深化任务 |
| ai_host_persona | 主播人设 |
| ai_user_cognitive_profile | 用户认知画像（行业大脑） |
| ai_graph_node / ai_graph_edge | 知识图谱节点与边（Flyway V048，无 Neo4j 时 JPA 图） |
| ai_generation_task | 生成任务 |
| ai_image_generation | 图片生成记录 |
| ai_tts_generation | TTS 生成记录 |
| ai_viral_analysis | 爆款分析 |
| ai_index_queue | 索引队列 |
| ai_query_log | 查询日志 |
| ai_task_model_config | 任务模型配置 |
| ai_live_review | 直播复盘 |
| kb_import_report | 导入报告 |
| kb_import_checkpoint | 导入检查点 |
| kb_feedback | 反馈 |
| ai_knowledge_evolution_rule | 知识进化规则 |
| ai_knowledge_deduplication_group | 知识去重组 |
| ai_knowledge_quality_score | 知识质量评分 |
| ai_knowledge_evolution_log | 知识进化日志 |

SQL 文件：`sql/ai/`

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| AI 仪表盘 | `pages/ai/AiDashboardPage.tsx` | `/admin/ai/dashboard` |
| 行业大脑 | `pages/ai/IndustryBrainPage.tsx` | `/admin/ai/brain` |
| 行业诊断 | `pages/ai/IndustryBrainDiagnosisPage.tsx` | `/admin/ai/diagnosis` |
| 知识库列表 | `pages/ai/KnowledgeBaseListPage.tsx` | `/admin/ai/knowledge` |
| 知识搜索 | `pages/ai/KnowledgeSearchPage.tsx` | `/admin/ai/knowledge/:kbId/search` |
| 文档管理 | `pages/ai/KnowledgeDocumentsPage.tsx` | `/admin/ai/knowledge/:kbId/documents` |
| 模型配置 | `pages/ai/ModelsConfigPage.tsx` | `/admin/ai/model` |
| 模型基准 | `pages/ai/ModelBenchmarkPage.tsx` | `/admin/ai/model-benchmark` |
| 进化任务 | `pages/ai/EvolutionTasksPage.tsx` | `/admin/ai/evolution` |
| 进化主题 | `pages/ai/EvolutionTopicPage.tsx` | `/admin/ai/evolution/topics` |
| 爆款分析 | `pages/ai/ViralAnalysisPage.tsx` | `/admin/ai/evolution/viral` |
| 创意工作室 | `pages/ai/CreativeStudioPage.tsx` | `/admin/ai/creative` |
| AI 监控 | `pages/ai/MonitoringPage.tsx` | `/admin/ai/monitoring` |
| 调用日志 | `pages/ai/CallLogPage.tsx` | `/admin/ai/call-log` |
| 基础设施 | `pages/ai/AdminInfraPage.tsx` | `/admin/ai/infra` |
| 提示词实验室 | `pages/ai/PromptLabPage.tsx` | `/admin/ai/prompt-lab` |
| 提示词模板 | `pages/ai/PromptTemplatePage.tsx` | `/admin/ai/prompt-template` |
| 数字人 | `pages/ai/DigitalHumanPage.tsx` | `/admin/ai/digital-human` |

## 前端 API

文件：`api/ai.ts`、`api/brain.ts`、`api/prompt-template.ts`、`api/evolution.ts`

> **备注**：前端 prompt 相关有两套 API：`ai.ts` 的 `/ai/prompt` 与 `prompt-template.ts` 的 `/ai/prompt-template`。

## API 接口清单（非 POST）

| 接口 | 说明 |
|------|------|
| DELETE /api/v1/ai/knowledge-base/{kbId} | [DELETE] 删除知识库 |
| DELETE /api/v1/ai/knowledge-base/document/{docId} | [DELETE] 删除文档 |
| DELETE /api/v1/ai/admin/evolve/topic/{id} | [DELETE] 删除进化主题 |
| DELETE /api/v1/ai/admin/evolve/task/{id} | [DELETE] 删除进化任务 |

## 进化引擎

7 类 Agent 定时巡检知识库（详细术语定义见 [`EVOLUTION-TERMINOLOGY.md`](./EVOLUTION-TERMINOLOGY.md)）：

| Agent | 调度器 | 默认 Cron | 职责 |
|-------|--------|-----------|------|
| 知识缺口检测 | `EvolveScheduler` | 每 60s 检查（ROI 自适应） | 从主题池采样，LLM 补全知识缺口 |
| 时效性检查 | `FreshnessCheckScheduler` | 周日 04:00 | 识别过期文档，触发重建 |
| 质量评分 | `KnowledgeEvolutionScheduler` | 每日 06:00 UTC | LLM 裁判 + 启发式兜底评分 |
| **主题自发现** | `TopicDiscoveryScheduler` | 周一 07:00 | 基于搜索日志热词发现候选主题（待审核） |
| 跨域共享 | `CrossDomainShareScheduler` | 周六 05:00 | 优质文档跨 KB 复制 |
| 深度进化 | `DeepEvolveScheduler` | 周三/周日 06:30 | 消化低分报告的待深化问题 |
| 冷文档检测 | `ColdDocDetectionScheduler` | 周日 03:00 | 长期未访问文档归档评估 |

> 注：旧文档中的"自动分类"对应的 `EvolveTopicSync` 是**启动时**增量同步 canonical 主题到 DB，不是定时分类 Agent。
