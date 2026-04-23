# dy02 前端差距分析与升级方案

> 基准日期：2026-03-29
> 参照系：dy01（frontend-react/）完整实现
> 目标：dy02/front 达到与 dy01 对等的功能覆盖率
> **最后更新：2026-03-29（Phase 1-4 全部完成 + 本轮补充17页，103 个页面，对齐率 ~100%）**

---

## 一、现状概览

### dy02 当前页面（83 个）

| 模块 | 页面 | 状态 |
|------|------|------|
| 根 | LoginPage / NotFoundPage / DashboardPage | ✅ |
| auth | UsersPage / RolesPage / LoginLogsPage / ResourcesPage | ✅ |
| douyin | AccountsPage / PersonasPage / VideosPage | ✅ |
| live | SessionsPage / ScriptsPage / LiveWorkbenchPage / SessionWorkspacePage / ScriptRankingPage / RhythmPage / HistoryComparePage | ✅ |
| product | ProductsPage / EffectivenessScorePage / StylePresetPage / ProductReadinessPage | ✅ |
| script | ScriptListPage / ViolationWordPage / ScriptTemplatePage / ViolationCheckPage / ScriptGenerationPage / ScriptOptimizationPage | ✅ |
| shortvideo | ProjectsPage / AccountCollectPage / RemakeTemplatePage / ShotListPage / MaterialPage / VideoEditingPage / PublishPage / QualityPage / DailyContentPage / ViralChainPage / HotTopicPage / ContentCalendarPage / DramaPage / ViralVideoPage / ShortVideoDashboardPage / QuickGeneratePage / ScriptPlanningPage / MaterialPreparationPage / MaterialProductionPage / SvProjectWorkbenchPage / WorkflowEditorPage / CompetitorMonitorPage / ContentEffectPredictPage / DataAnalysisPage | ✅ |
| copy | CopyLibraryPage / CopyTemplatePage / CopyApprovalPage | ✅ |
| ai | KnowledgeBasePage / EvolutionPage / AiDashboardPage / KnowledgeSearchPage / KnowledgeDocumentsPage / KnowledgeEvolutionPage / EvolutionReviewPage / IndustryBrainPage / PromptLabPage / ModelsConfigPage / AdminInfraPage / PromptTemplatePage / ViralAnalysisPage / CreativeStudioPage / AiCallLogPage / AiMonitoringPage / DigitalHumanPage / EvolutionTasksPage / EvolutionTopicPage / ModelBenchmarkPage / IndustryBrainDiagnosisPage / PromptToolsPage | ✅ |
| agent | AgentListPage / AgentChatPage | ✅ |
| abtest | ExperimentsPage / ExperimentDetailPage | ✅ |
| config | ConfigPage | ✅ |
| storage | StoragePage | ✅ |
| system | SystemPage / AlertRulesPage / ApiLogPage / ComplianceCheckPage / ExternalApiConfigPage / ExternalApiHealthPage / PerformanceMonitoringPage / TianApiPanelPage | ✅ |
| log | OperationLogPage / AuditLogPage | ✅ |
| wecom | RobotsPage / RulesPage | ✅ |
| payment | PaymentPage / SubscriptionPage / UsageQuotaPage | ✅ |
| attribution | AttributionPage | ✅ |
| org（多角色） | MembersPage / OrgAnalyticsPage / OrgLiveReviewsPage | ✅ |
| slangdict | SlangDictPage | ✅ |

### 页面数量对比

| 时间点 | 页面数 | 对齐率 |
|--------|--------|--------|
| 初始（2026-03-29 升级前） | 26 | ~43% |
| Phase 1 完成 | 37 | ~62% |
| Phase 2 完成 | 51 | ~85% |
| Phase 3 完成 | 54 | ~90% |
| Phase 4 完成 | 83 | ~100% |
| 补充完善（2026-03-29 session 1） | 86 | ~100% |
| 本轮全面补全（2026-03-29 session 2） | 103 | ~100% |

---

## 二、差距清单（Gap Analysis）— 全部已完成 ✅

### 2.1 P0 核心业务页面（11 个）— 全部完成

| 模块 | 页面 | 状态 |
|------|------|------|
| auth | ResourcesPage（资源/菜单管理） | ✅ 已完成 |
| live | ScriptRankingPage（话术效果排行） | ✅ 已完成 |
| live | RhythmPage（直播节奏配置） | ✅ 已完成 |
| live | HistoryComparePage（版本历史对比） | ✅ 已完成 |
| copy | CopyApprovalPage（文案审批） | ✅ 已完成 |
| script | ScriptTemplatePage（话术模板库） | ✅ 已完成 |
| payment | SubscriptionPage（订阅管理） | ✅ 已完成 |
| payment | UsageQuotaPage（额度使用） | ✅ 已完成 |
| system | AlertRulesPage（告警规则） | ✅ 已完成 |
| system | ApiLogPage（API调用日志） | ✅ 已完成 |
| log | AuditLogPage（审计日志） | ✅ 已完成 |

### 2.2 P1 AI + 短视频完整流程（14 个）— 全部完成

| 模块 | 页面 | 状态 |
|------|------|------|
| ai | PromptTemplatePage（Prompt模板管理） | ✅ 已完成 |
| ai | ViralAnalysisPage（爆款分析） | ✅ 已完成 |
| ai | CreativeStudioPage（创作工作室） | ✅ 已完成 |
| ai | AiCallLogPage（AI调用日志） | ✅ 已完成 |
| ai | AiMonitoringPage（AI基础设施监控） | ✅ 已完成 |
| ai | DigitalHumanPage（数字人服务） | ✅ 已完成 |
| shortvideo | ShotListPage（分镜设计） | ✅ 已完成 |
| shortvideo | MaterialPage（素材管理） | ✅ 已完成 |
| shortvideo | VideoEditingPage（视频生成任务） | ✅ 已完成 |
| shortvideo | PublishPage（发布调度） | ✅ 已完成 |
| shortvideo | QualityPage（质量看板） | ✅ 已完成 |
| shortvideo | DailyContentPage（日排内容） | ✅ 已完成 |
| agent | AgentChatPage（对话界面） | ✅ 已完成 |
| abtest | ExperimentDetailPage（实验详情） | ✅ 已完成 |

### 2.3 超出 dy01 的额外补全（6 个）

以下为 dy02 新增、dy01 无对应的页面：

| 模块 | 页面 | 说明 |
|------|------|------|
| ai | KnowledgeEvolutionPage | 知识进化详情 |
| ai | EvolutionReviewPage | 进化审核 |
| ai | IndustryBrainPage | 行业大脑（趋势/因果/画像/知识图谱）|
| ai | PromptLabPage | Prompt 实验室 |
| ai | ModelsConfigPage | 模型配置 |
| ai | AdminInfraPage | AI 基础设施面板 |
| shortvideo | ViralChainPage | 传播链分析 |
| shortvideo | HotTopicPage | 热点话题 |
| shortvideo | ContentCalendarPage | 内容日历 |
| shortvideo | DramaPage | 剧情短视频 |
| product | EffectivenessScorePage | 话术效果评分排行 |
| product | StylePresetPage | 话术风格预设 |
| system | ExternalApiHealthPage | 外部 API 健康监控 |
| system | PerformanceMonitoringPage | 系统性能监控 |
| system | TianApiPanelPage | 天API 数据面板 |
| slangdict | SlangDictPage | 行业俚语词典 |

---

## 三、API 层补全情况

### 新增 API 文件

| 文件 | 用途 |
|------|------|
| `src/api/brain.ts` | 行业大脑（趋势/因果推断/用户画像/知识图谱）|
| `src/api/slangdict.ts` | 行业俚语词典 CRUD |

### 关键 API 方法对齐（补充的）

- `auth.ts`：`resourceTree / resourceSave / resourceDelete / menuTree`
- `log.ts`：`auditLogPage / auditLogGet`
- `payment.ts`：`subscriptionGet / usageQuota`
- `system.ts`：`alertRuleList / alertRuleSave / alertRuleDelete / alertRuleEnable / alertRuleDisable / apiLogList / apiLogStats / externalApiHealthStatus`
- `ai.ts`：`promptTemplateList / promptTemplateSave / promptTemplateDelete / promptTemplateGetActive / promptTemplateTestRender / callVolumeTrend / callTypeDistribution / infraHealth / cacheStats / searchStats / digitalHumanStatus / digitalHumanGenerate / topicList / topicSave / topicDelete / evolveRoi / scoreTrend`
- `shortvideo.ts`：`calendar / calendarStats / recommendPublishTime / viralAnalyze / svScriptList`

---

## 四、路由与菜单变更

### router/index.tsx

总计注册路由 57+ 条（/admin、/org、/talent 三套）。所有新页面均通过 `lazy()` + `<LazyRoute>` 实现代码分割。

### AdminLayout.tsx NAV_GROUPS

新增菜单项分布：
- **auth 组**：资源权限
- **live 组**：话术排行、节奏配置、历史对比
- **copy 组**：文案审批
- **script 组**：话术模板
- **payment 组**：订阅管理、额度使用
- **system 组**：告警规则、API日志、API健康监控、性能监控、天API面板、俚语词典
- **log 组**：审计日志
- **ai 组**：知识进化、进化审核、行业大脑、Prompt实验室、模型配置、基础设施
- **operation 组**：传播链分析、热点话题、内容日历、剧情短视频、话术效果、风格预设

---

## 五、TypeScript 验证

```bash
cd C:/claude/dy02/front
npm run type-check   # ✅ 零错误
npm run build        # ✅ built in 16.79s
```

---

## 六、技术约束备忘

- **SSE 流**：`agentApi.chatWithAgent()` 用 `ssePost()` 工具函数，不用普通 axios
- **DataGrid**：所有列表页用 `StandardDataGrid`，slotProps.toolbar 用 `as any`
- **Toast**：`const toast = useToast(); toast('消息', 'success')`
- **MUI Grid**：legacy API `<Grid item xs={12} sm={6}>`，不用 v2 size prop
- **图表**：ECharts via echarts-for-react，不引入其他图表库
- **CDN图片**：缩略图 `url + '@!80X80'`，大图 `url + '@!300X250'`
- **TypeScript**：`noUnusedLocals` 严格开启，未用变量加 `_` 前缀
- **懒加载**：所有新页面用 `lazy()` + `<LazyRoute>` 包裹
- **paginationMode**：返回平铺数组的 API（如 stylePresetList）用 `"client"` 模式
- **request.post**：只传 1 个泛型参数，不传 2 个
