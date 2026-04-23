# 直播模块（live）

## 模块概述

直播话术全生命周期管理：场次创建 → 商品绑定 → 话术生成（AI）→ 话术编辑 → 实时面板 → 效果分析 → 历史复盘。是平台最复杂的业务模块。

### LIVE-01 弹幕情绪与采集边界

- **抖音开放平台**：`DouyinDataCollector` 拉取的是直播间**聚合指标**（在线、点赞、评论总数等），**不提供单条弹幕文本**，因此情绪窗口无法从 Collector 自动填充。
- **ingest**：`POST /api/v1/live/realtime-panel/danmaku-ingest`（单条）、`.../danmaku-ingest-batch`（批量，默认上限见 `app.live.danmaku-sentiment.batch-max-lines`）。需登录且场次 `user_id` 与当前用户一致；可由中间件/Webhook 携带 Bearer 转发。
- **多实例**：`DanmakuSentimentServiceImpl` 为进程内窗口；多副本部署时每节点独立聚合（与升级计划中 P1 技术债一致）。

## 后端结构

```
module/live/
├── config/
│   ├── LiveAutoSyncScheduler.java            # 直播数据自动同步
│   ├── LiveMonitorArchiveScheduler.java      # 监控数据归档
│   ├── LivePromptConfig.java                 # 话术 Prompt 配置
│   ├── LiveScriptTemplateScheduler.java      # 话术模板调度
│   └── ScriptEventListeners.java             # 话术事件监听器
│
├── controller/（35+ 个）
│   ├── LiveSessionController.java            # 直播场次 CRUD
│   ├── LiveScriptController.java             # 话术 CRUD
│   ├── LiveProductController.java            # 直播商品管理
│   ├── LiveAiController.java                 # AI 话术生成
│   ├── LiveRealtimePanelController.java      # 实时面板
│   ├── LiveMonitorController.java            # 直播监控
│   ├── EffectivenessScoreController.java     # 效果评分
│   ├── LiveScriptVersionController.java      # 话术版本
│   ├── LiveAnalysisController.java           # 数据分析
│   ├── LiveApprovalController.java           # 话术审批
│   ├── LiveTemplateController.java           # 话术模板
│   ├── LivePlatformRuleController.java       # 平台合规
│   ├── LiveMonitorSseController.java         # [GET SSE] 监控流
│   ├── LiveScriptNavigationController.java   # 话术导航
│   ├── LiveCollaborationPresenceController.java # 协作在线状态
│   ├── LiveAbTestAnalysisController.java     # A/B 测试分析
│   ├── LiveScriptPipelineController.java     # 话术流水线
│   ├── LiveGenerationTaskController.java     # 生成任务
│   ├── LiveScriptCustomTemplateController.java # 自定义模板
│   ├── LiveStyleController.java              # 风格预设
│   ├── LiveDataSyncController.java           # 数据同步
│   ├── DanmakuAnalysisController.java        # 弹幕分析
│   ├── ScriptQualityController.java          # 话术质量
│   ├── LiveRhythmController.java             # 直播节奏
│   ├── ContentMaterialController.java        # 内容素材
│   ├── GenerationPresetController.java       # 生成预设
│   └── ...
│
├── entity/（30+ 个）
│   ├── LiveSession.java                      # 直播场次（live_session）
│   ├── LiveScript.java                       # 话术脚本（live_script）
│   ├── LiveProduct.java                      # 直播商品（live_product）
│   ├── LiveMonitor.java                      # 直播监控数据
│   ├── LiveScriptVersion.java                # 话术版本
│   ├── LiveSessionScriptSlot.java            # 话术卡槽（live_session_script_slot）
│   ├── LiveGenerationTask.java               # 生成任务
│   ├── LiveGenerationPreset.java             # 生成预设
│   ├── LiveEffectivenessConfig.java          # 效果配置
│   ├── LiveScriptTemplate.java              # 话术模板
│   ├── LiveScriptApproval.java               # 话术审批
│   ├── LiveScriptComment.java                # 话术评论
│   ├── LiveSessionRealtimeData.java          # 实时数据（live_session_realtime_data）
│   └── ...
│
├── service/（40+ 个）
│   ├── LiveSessionService.java               # 场次管理
│   ├── LiveScriptService.java                # 话术管理
│   ├── LiveProductService.java               # 商品管理
│   ├── LiveScriptGenerationService.java      # AI 话术生成（核心）
│   ├── LiveRealtimeService.java              # 实时面板
│   ├── LiveMonitorService.java               # 监控
│   ├── LiveEffectivenessService.java         # 效果评分
│   ├── LiveVersionService.java               # 版本管理
│   ├── LiveAnalysisService.java              # 数据分析
│   ├── LiveSlotService.java                  # 卡槽管理
│   ├── LiveTemplateService.java              # 模板管理
│   ├── LiveApprovalService.java              # 审批
│   ├── LiveRhythmService.java                # 节奏管理
│   ├── ScriptQualityEvaluator.java           # 话术质量评估
│   └── ...
│
└── vo/（70+ 个）
    ├── LiveSessionSaveVO / SearchVO / VO
    ├── LiveScriptSaveVO / SearchVO / VO
    ├── LiveProductSaveVO / SearchVO / VO
    ├── GenerationRequestVO / ResultVO
    └── ...
```

## 数据库表

| 表名 | 说明 |
|------|------|
| live_session | 直播场次 |
| live_script | 话术脚本（`generation_prompt_hash`：末次 AI 请求 system+user 的 SHA-256） |
| live_product | 直播商品 |
| live_monitor | 监控数据 |
| live_script_version | 话术版本历史 |
| live_session_script_slot | 话术卡槽 |
| live_generation_task | 生成任务 |
| live_generation_preset | 生成预设 |
| live_effectiveness_config | 效果配置 |
| live_script_template | 话术模板 |
| live_script_approval | 话术审批 |
| live_script_comment | 话术评论 |
| live_session_realtime_data | 实时数据 |
| script_usage_log | 使用日志 |
| live_script_pipeline | 流水线 |
| live_ab_test_result | A/B 测试结果 |
| live_approval_log | 审批日志 |
| live_violation_rule | 违规规则 |
| live_platform | 平台规则 |
| live_style_preset | 风格预设 |
| live_script_effectiveness | 话术效果 |
| live_session_data | 场次数据 |
| live_product_data | 商品数据 |
| live_slot_type | 卡槽类型 |

SQL 文件：`sql/live/`

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 场次列表 | `pages/live/LiveSessionPage.tsx` | `/admin/live/sessions` |
| 新建场次 | `pages/live/LiveSessionFormPage.tsx` | `/admin/live/sessions/create` |
| 编辑场次 | `pages/live/LiveSessionFormPage.tsx` | `/admin/live/sessions/:id/edit` |
| 场次详情 | `pages/live/LiveSessionDetailPage.tsx` | `/admin/live/sessions/:id` |
| 话术工作台 | `pages/live/SessionWorkspacePage.tsx` | `/admin/live/sessions/:id/workspace` |
| 实时面板 | `pages/live/LiveRealtimePanel` | `/admin/live/realtime`（sessionId 通过 query 传递） |
| 直播商品 | `pages/live/LiveProductPage.tsx` | `/admin/live/product` |
| 历史对比 | `pages/live/LiveHistoryComparePage.tsx` | `/admin/live/history` |
| 话术排行 | `pages/live/LiveScriptRankingPage.tsx` | `/admin/live/script-ranking` |
| 直播节奏 | `pages/live/LiveRhythmPage.tsx` | `/admin/live/rhythm` |
| 机构场次 | `pages/live/OrgLiveSessionPage.tsx` | `/org/live/sessions` |
| 机构审核 | `pages/live/OrgLiveReviewsPage.tsx` | `/org/live/reviews` |

## 前端组件

直播模块有大量专属子组件（`pages/live/components/`）：

| 组件 | 说明 |
|------|------|
| ScriptPanel | 话术面板 |
| ProductPanel | 商品面板 |
| ScriptTab / ScriptSection | 话术标签/区块 |
| GenerationFlowPanel | 生成流程面板 |
| GenerationPreviewDialog | 生成预览 |
| GenerationProgressFab | 生成进度悬浮按钮 |
| AiAssistTab / AiAnalystPanel / AiChatPanel | AI 辅助 |
| DataAnalysisTab | 数据分析 |
| ReadinessTab | 准备度检查 |
| VersionHistoryPanel | 版本历史 |
| ScriptFlowCanvas / CanvasTabContent | 话术流程画布 |
| ScriptTimelinePanel | 话术时间线 |
| ProductEditDrawer / ProductGridView | 商品编辑/网格 |
| BatchProductDialog | 批量商品 |
| TemplateLibraryDialog | 模板库 |
| TrendingTopicsPanel | 热门话题 |
| HotKeywordsSelector | 热门关键词 |
| SortStrategyPanel | 排序策略 |
| ScriptCommentPopover | 评论弹窗 |
| AbExperimentSummary | A/B 实验摘要 |

## 前端 API

文件：`api/live.ts`、`api/live-realtime.ts`、`api/live.versions.ts`、`api/approval.ts`

## API 接口清单（非 POST）

| 接口 | 说明 |
|------|------|
| GET /api/v1/live/monitor/stream/{sessionId} | [GET SSE] SSE 监控流 |
| GET /api/v1/live/realtime-panel/stream/{sessionId} | [GET SSE] SSE 实时面板 |

## 核心业务流程

### 话术生成流程

```
1. 创建直播场次 (LiveSession)
2. 绑定直播商品 (LiveProduct) ← 从商品库选择
3. 初始化话术卡槽 (LiveSessionScriptSlot)
4. AI 生成话术 (LiveScriptGenerationService)
   ├── 开场话术 generateOpening()
   ├── 产品话术 generateProduct()
   ├── 全量话术 generateFull() / generateFullStream() [SSE]
   └── 单卡槽话术 generateForSlot() / generateForSlotStream() [SSE]
5. 话术编辑/优化
   ├── refineScript() / refineScriptStream() [SSE]
   ├── chatForScript() / chatForScriptStream() [SSE]
   └── suggestImprovement()
6. 话术质量评估 (ScriptQualityEvaluator)
7. **话术违规检测**（`LiveScriptQualityServiceImpl.checkViolation`）：合并 `violation_word`（`ViolationWordService`）+ `sc_compliance_word`（`ComplianceWordService`，含 V067/V108）+ `IndustryComplianceService`（**全业通用 + 垂直行业包 + `DOUYIN_PUBLIC_RULE_PATTERNS`**）；配置 `app.live.compliance.industry-code` 或 `APP_LIVE_COMPLIANCE_INDUSTRY_CODE`（默认 `cosmetics`）；垂直编码列表见 `POST /api/v1/script/compliance/industry-codes` 与 `docs/compliance/01-多行业合规规则.md`
8. 话术审批 (LiveApproval)
9. 实时面板使用 (LiveRealtime)
10. 效果分析 (LiveEffectiveness)
```

### 版本管理

话术支持自动版本记录，可回滚、对比、查看历史。

### 效果评分

基于转化率、停留时长、互动率等维度评估话术效果，支持 A/B 测试。

## Zustand Store

| Store | 文件 | 职责 |
|-------|------|------|
| useLiveEditStore | `stores/liveEditStore.ts` | 话术编辑状态 |
| useLiveGenStore | `stores/liveGenStore.ts` | 生成流程步骤 |
| useLiveProductStore | `stores/liveProductStore.ts` | 商品管理状态 |

## 升级计划

| 文档 | 说明 |
|------|------|
| [2小时聊天式直播话术需求.md](./2小时聊天式直播话术需求.md) | 2 小时全场话术（聊家常+利润品）需求说明 |
| [2小时聊天式直播话术-升级计划.md](./2小时聊天式直播话术-升级计划.md) | 2 小时聊天式 — 分阶段实施计划（含任务、文件、验收） |
| [2小时聊天式-方案深度分析与升级建议.md](./2小时聊天式-方案深度分析与升级建议.md) | 深度分析：技术缺口、补充任务、架构决策、风险缓解 |
| [2小时聊天式-补充升级计划.md](./2小时聊天式-补充升级计划.md) | P0–P3 补充任务：槽位触发、sessionType、产品绑定、Timeline、人气策略 |
| [李阳阳-2小时拉自然流带货-深度分析与升级建议.md](./李阳阳-2小时拉自然流带货-深度分析与升级建议.md) | 李阳阳人设、拉自然流策略、待升级项深度分析 |
| [李阳阳-2小时拉自然流-全量升级计划.md](./李阳阳-2小时拉自然流-全量升级计划.md) | 李阳阳拉自然流全量升级：人设对齐、策略注入、文档对齐 |
| [李阳阳-2小时拉自然流-知识库素材清单.md](./李阳阳-2小时拉自然流-知识库素材清单.md) | huashu 知识库素材导入清单（歇后语、名言、古诗等） |
| [李阳阳-2小时拉自然流-深度分析补充.md](./李阳阳-2小时拉自然流-深度分析补充.md) | 深度分析补充：剩余缺口、P2–P4 补充建议、行业升级衔接 |
| [话术生成与微调升级计划.md](./话术生成与微调升级计划.md) | 生成后微调：单句/单段修改 API 与 UI |
