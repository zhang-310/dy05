# 附表三 P2 · Epic 拆票（对照 [P0-P1-完成度对照表](./P0-P1-完成度对照表.md) 附表三）

> 目的：将 **31** 条 P2 从「粗扫 ❌/⚠️」落实为可排期 Epic；**不**在一次迭代内假完成。  
> **逐条执行表**（PR/验收勾选）：[`P2-附表三-逐条检查清单.md`](./P2-附表三-逐条检查清单.md)。  
> **与 P0+P1 的 ⚠️**：以 **`P0-P1-完成度对照表.md`** 为准——**§ 全部 ⚠️ 一览** 共 **15** 条（**P0+P1** 十条 **⚠️-MVP** + **附表三** 五条深化）；其中七条原 **⚠️-降级** 已经 [**ADR 006**](../adr/006-P0P1-规则版终局指标.md) 翻 ✅。**§ P0+P1 · ⚠️ 收口计划** 仅跟踪仍待指派的 **10** 条 MVP 边界（体量 S/M/L）。工程细节与 **`docs/development/04-文档代码同步清单.md`** §八 交叉引用。P2 与 P0/P1 并行排期、分 PR 交付。

## Epic-P2-01 · 行业大脑检索与图谱（01 区块 9 ❌）

- **S-4** 查询意图分类 → `QueryRewriteServiceImpl` + 检索路由配置  
- **E-4** 进化任务依赖 / DAG → `AiEvolveTask` + Flyway + 调度  
- **T-1（01）** 工具静态依赖图 → `LlmToolOrchestratorService` 扩展  
- **G-2 / G-5 / G-6** 图谱推理、子图 JSON、矛盾检测 → `IndustryKnowledgeGraphServiceImpl`  
- **F-2** 检索解释 UI → `KnowledgeSearchPage` + API  
- **F-4 / F-6** 进化 DAG 与 ROI 可视化 → `EvolutionTasksPage` + 指标 API  

## Epic-P2-02 · 直播话术深化（02 区块 1 ⚠️ + 12 ❌）

- **Q-5** 表演指导：已加 **`performance-cue-heuristics`**（规则提示）；后续由正则+规则 → 可选 LLM 混合（成本评估）→ `LiveScriptQualityServiceImpl`  
- **L-5 / L-7** 分段评分、异常检测 → 效果分表或扩展 `EffectivenessScoreServiceImpl`  
- **R-4 / R-6** 提词备注卡、多直播间并排 → 新字段 + 大屏数据源  
- **Q-3** 情感量化  
- **C-2 / C-3 / C-5** 竞品定价/份额/胜负 → 数据域立项  
- **M-3 / M-4 / M-6** 槽位轮换、热度重排、模板 A/B  
- **G-2（02）** 直播长任务 MQ → 对齐短视频 `VideoGenerationTask` 模式  

## Epic-P2-03 · 短视频差异化（03 区块 3 ⚠️ + 6 ❌）

- **T-5** Webhook DLQ：`sv_webhook_dlq` + API + **`MaterialProductionPage`** 对话框；Epic 内其余 H-1/H-5/V-5… 仍待排期  
- **H-1** 电影级知识库 → **`docs/modules/shortvideo/H-1-电影级知识库差距.md`** + `CinematicKnowledgeService` 迭代路线
- **H-5** 工作流真步骤 → **`H-5-工作流占位步骤.md`** 已披露；musicGen/sfxGen/digitalHuman **真实现**或编辑器 **隐藏节点** 仍待 Epic
- **V-5 / D-5 / A-6 / K-5 / N-5 / N-6** 分别单独立项（关键帧、短剧模板库、素材去重、日历预测、ML 报表、自定义报表）  

## 维护

关闭某 Epic 中的条目时，同步更新 `P0-P1-完成度对照表.md` 附表三对应行与 `docs/development/04-文档代码同步清单.md`。
