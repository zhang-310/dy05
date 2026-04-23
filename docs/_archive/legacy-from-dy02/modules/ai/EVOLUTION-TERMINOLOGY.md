# 知识进化引擎术语词汇表

本文件是代码中 `EvolveEngineServiceImpl` 等进化相关类注释所引用的术语定义文档。

## 核心概念

### 进化主题（EvolveTopic）

存储在 `ai_evolve_topic` 表中。每个主题代表一个待进化的知识领域，如"玻尿酸成分分析"、"直播开场话术技巧"。

**来源（source 字段）**：
- `initial`：系统初始化预置（`EvolveDataInitializer`）
- `sync`：`EvolveTopicSync` 启动同步补充
- `import`：管理员手动导入
- `discovered`：`TopicDiscoveryScheduler` 基于搜索日志自动发现（status=0 待审核）
- `manual`：管理员手动创建

**状态（status 字段）**：
- `0`：待审核（discovered 主题默认，需管理员审核激活）
- `1`：已激活（可被进化引擎采样）

### 进化任务（EvolveTask）

由 `EvolveScheduler` 触发，`EvolveEngineService` 编排。每个任务对应一个主题的一次进化尝试，产出一篇进化报告。

**生命周期**：pending → running → completed / failed

### 进化报告（EvolveReport）

LLM 生成的结构化知识产出。包含以下标准章节：

**通用报告（general）**：
- `## 方法论提炼`：可操作的方法与步骤
- `## 待深化问题`：需进一步研究的问题
- `## 可迭代建议`：改进建议

**话术报告（huashu）**：
- 以直播场景话术为核心，覆盖开场/卖点/互动/收尾
- 话术片段需包含 `##话术片段` 章节

**知识库报告（zhishi）**：
- `## 技术要点提炼`：成分/功效技术细节
- `## 待深化问题`：技术盲区
- `## 实践检查清单`：落地步骤

### 深度进化（PendingDeepen）

低质量进化报告中提取的"待深化问题"，存储在 `ai_evolve_pending_deepen`，由 `DeepEvolveScheduler`（周三/周日 06:30）专项消化。

## 进化 Agent 分类（7 类）

| Agent | 调度器 | 默认 Cron | 职责 | 实现 |
|-------|--------|-----------|------|------|
| 知识缺口检测 | `EvolveScheduler` | 每 60s 检查（按 ROI 自适应间隔） | 从主题池采样，LLM 补全知识缺口 | `EvolveEngineServiceImpl` |
| 时效性检查 | `FreshnessCheckScheduler` | `0 0 4 * * SUN`（周日 04:00） | 识别过期文档，触发重建 | `FreshnessCheckService` |
| 质量评分 | `KnowledgeEvolutionScheduler` | 每日 06:00 UTC | 基于 LLM 或启发式评估文档质量分层 | `KnowledgeEvolutionService` + `LlmJudgeService` |
| **主题自发现** | `TopicDiscoveryScheduler` | `0 0 7 * * MON`（周一 07:00）| **新增**：基于搜索日志热点自动发现候选主题（待审核） | `TopicDiscoveryScheduler` |
| 跨域共享 | `CrossDomainShareScheduler` | `0 0 5 * * SAT`（周六 05:00） | 优质文档跨 KB 复制共享 | `CrossDomainShareService` |
| 深度进化 | `DeepEvolveScheduler` | `0 30 6 * * WED,SUN`（周三/日 06:30） | 消化低分报告的"待深化问题" | `DeepEvolveService` |
| 冷文档检测 | `ColdDocDetectionScheduler` | `0 0 3 * * SUN`（周日 03:00） | 识别长期未访问文档，触发归档评估 | 内部实现 |

> 注：旧文档中"自动分类"（`EvolveTopicSync`）实际是**应用启动时**增量同步 canonical 主题到 DB，不是定时分类 Agent。真正的"主题自发现"由 2026-03 新增的 `TopicDiscoveryScheduler` 实现。

## 质量评分体系

### 进化报告质量分（0–100）

双层评分机制：
1. **LLM 裁判**（优先）：`LlmJudgeService` 调用可用模型，按报告类型（general/huashu/zhishi）使用不同 System Prompt 评分
2. **启发式兜底**：`EvolveReportProcessor` 按章节存在性、Bullet 数量、关键词等计分

### 文档质量分层（Tier）

`KbDocumentQualityService` 将 KB 文档分为：
- `TIER_HEALTHY (1)`：高质量，检索权重正常
- `TIER_NEUTRAL (0)`：中等，不加惩罚
- `TIER_LOW (2)`：低质量，`KbHybridRetrieveService` 降低检索权重

**分层触发**：
- 定时批量扫描（`KbDocumentQualityScheduler`）
- 用户反馈（`KbFeedbackVO.rating` < 0 → TIER_LOW；> 1 → TIER_HEALTHY）

## 人类反馈闭环（RLHF-lite）

2026-03 引入的反馈驱动权重更新机制：

1. **进化报告审核**（`EvolutionReviewService`）：
   - 审核通过 → 关联主题 `priority` 降低（更频繁进化）、`scoreAvg` 上调
   - 审核拒绝 → 关联主题 `priority` 升高（减少无效进化）

2. **KB 反馈**（`/api/v1/ai/knowledge-base/feedback`）：
   - `rating < 0` → 文档降级为 TIER_LOW
   - `rating > 1` → 文档升级为 TIER_HEALTHY

3. **搜索日志驱动主题发现**（`TopicDiscoveryScheduler`）：
   - 每周一分析近 7 天高频搜索词
   - LLM 判断是否值得进化 → 写入待审核主题

## LLM 工具（Tool Calling）

注册到 `LlmToolOrchestratorService` 的内置工具：

| 工具名 | 类 | 说明 | 缓存 TTL |
|--------|-----|------|---------|
| `kb_rag_search` | `KnowledgeBaseRagLlmTool` | 知识库混合检索 | 默认 |
| `product_search` | `ProductSearchLlmTool` | 商品搜索（名称/分类/价格） | 5 min |
| `live_session_stats` | `LiveSessionStatsTool` | 直播场次 GMV/观看数等指标 | 2 min |
| `competitor_analysis` | `CompetitorAnalysisLlmTool` | 竞品洞察与差异化建议 | 30 min |
| `trend_query` | `TrendQueryLlmTool` | 行业热点趋势 | 5 min |
