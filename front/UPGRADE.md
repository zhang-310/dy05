# dy02 前端升级文档 — 后端 API 全量对齐

> 每次修改前必须对照本文档，保证 API 路径/类型/页面功能三处一致。
> 生成日期：2026-03-27

---

## 目录

1. [auth 模块](#1-auth-模块)
2. [douyin 模块](#2-douyin-模块)
3. [live 模块](#3-live-模块)
4. [product 模块](#4-product-模块)
5. [script 模块](#5-script-模块)
6. [shortvideo 模块](#6-shortvideo-模块)
7. [copy 模块](#7-copy-模块)
8. [ai 模块](#8-ai-模块)
9. [agent 模块](#9-agent-模块)
10. [abtest 模块](#10-abtest-模块)
11. [wecom 模块](#11-wecom-模块)
12. [payment 模块](#12-payment-模块)
13. [storage 模块](#13-storage-模块)
14. [log 模块](#14-log-模块)
15. [dashboard 模块](#15-dashboard-模块)
16. [config 模块](#16-config-模块)
17. [system 模块](#17-system-模块)
18. [attribution 模块](#18-attribution-模块)
19. [升级优先级清单](#19-升级优先级清单)

---

## 1. auth 模块

### 后端 API（`AuthUserController` + `AuthRoleController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/auth/login` | POST | 登录，返回 token |
| `/api/v1/auth/logout` | POST | 登出 |
| `/api/v1/auth/user/search` | POST | 用户分页 |
| `/api/v1/auth/user/save` | POST | 新增/编辑用户 |
| `/api/v1/auth/user/ban` | POST | 禁用/启用 |
| `/api/v1/auth/user/delete` | POST | 删除 |
| `/api/v1/auth/user/login-logs` | POST | 登录日志 |
| `/api/v1/auth/user/online` | POST | 在线用户 |
| `/api/v1/auth/role/search` | POST | 角色分页 |
| `/api/v1/auth/role/list` | POST | 角色全列表 |
| `/api/v1/auth/role/save` | POST | 新增/编辑角色 |
| `/api/v1/auth/role/delete` | POST | 删除角色 |
| `/api/v1/auth/role/resources` | POST | 角色资源 |
| `/api/v1/auth/dashboard/admin` | POST | 管理员统计 |

### 状态
- `src/api/auth.ts` ✅ 全量对齐：login/logout/captcha + user CRUD/ban/loginLogs/onlineUsers + profile/profileUpdate/changePassword + role CRUD/roleGet/roleAll/roleResources/roleResourcesSave + resource CRUD/tree/treeFull + menuSearch + org my/create/update/members/invite/remove/invitations
- `src/pages/auth/UsersPage.tsx` ✅ 完整 CRUD
- `src/pages/auth/RolesPage.tsx` ✅ 完整 CRUD
- `src/pages/auth/LoginLogsPage.tsx` ✅ 完整 DataGrid

### 待做
- [x] `auth.ts` 全量对齐：profile/changePassword/resource/org/menu — ✅ 已完成


---

## 2. douyin 模块

### 后端 API（`DouyinAccountController` + `DouyinPersonaController` + `DouyinVideoController` + `FanProfileController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/douyin/account/search` | POST | 账号分页 |
| `/api/v1/douyin/account/save` | POST | 新增/编辑账号 |
| `/api/v1/douyin/account/delete` | POST | 删除账号 |
| `/api/v1/douyin/account/statistics` | POST | 账号统计数据 |
| `/api/v1/douyin/persona/list` | POST | 人设列表 |
| `/api/v1/douyin/persona/save` | POST | 新增/编辑人设 |
| `/api/v1/douyin/persona/delete` | POST | 删除人设 |
| `/api/v1/douyin/persona/set-default` | POST | 设为默认人设 |
| `/api/v1/douyin/persona/get-default` | POST | 获取默认人设 |
| `/api/v1/douyin/persona/templates` | POST | 人设模板列表 |
| `/api/v1/douyin/video/search` | POST | 视频分页 |
| `/api/v1/douyin/video/save` | POST | 新增/编辑视频 |
| `/api/v1/douyin/video/sync` | POST | 同步视频 |
| `/api/v1/douyin/fan-profile/get` | POST | 粉丝画像 |
| `/api/v1/douyin/fan-profile/stats` | POST | 粉丝统计 |

### 状态
- `src/api/douyin.ts` ✅ 全量对齐：account CRUD/stats + persona CRUD/setDefault/getDefault/templates + video search/get/save/sync + fanProfile get/stats/sync
- `src/pages/douyin/AccountsPage.tsx` ✅ 完整实现（含右侧详情抽屉：粉丝画像 ECharts 饼图 + 近期视频列表 + 同步按钮 fanProfileSync）
- `src/pages/douyin/PersonasPage.tsx` ✅ 完整 CRUD

### 待做
- [x] `douyin.ts` 补充 videoSearch/Save/Sync + fanProfileGet/Stats/Sync — ✅ 已完成

---

## 3. live 模块

### 后端 API（39 Controllers，156 API 方法）

| 控制器 | 路径前缀 | 核心端点 |
|--------|----------|----------|
| LiveSessionController | `/api/v1/live/session` | search/get/save/delete/start/end/clone/trend/overview/readiness/exportToShortVideo/multiMetrics |
| LiveSessionTemplateController | `/api/v1/live/session-template` | search/get/save/delete/save-as |
| LiveScriptController | `/api/v1/live/script` | search/get/save/delete/by-session/effectiveness/executed/save-to-library/save-batch-to-library/export |
| LiveScriptCustomTemplateController | `/api/v1/live/script-template` | save-from-session |
| LiveProductController | `/api/v1/live/product` | search/get/save/delete/by-session/batch-sort |
| LiveScriptVersionController | `/api/v1/live/script-version` | list/save/activate/delete/diff |
| LiveMonitorController | `/api/v1/live/monitor` | search/save/by-session/snapshot/push |
| LiveRealtimePanelController | `/api/v1/live/realtime-panel` | init/next-slot/save-timer/save-data |
| EffectivenessScoreController | `/api/v1/live/effectiveness` | calculate/session-ranking/compare/ranking/top-scripts/recommended-scripts/emerged-scripts/script-effectiveness |
| LiveEffectivenessConfigController | `/api/v1/live/effectiveness-config` | list/save/default/set-default/delete |
| LiveGenerationPresetController | `/api/v1/live/generation-preset` | list/save/delete/getDefault/set-default |
| LiveGenerationTaskController | `/api/v1/live/generation-task` | latest/create/update-progress |
| LiveScriptGenerationController | `/api/v1/live/ai` | generate-opening/product/transition/closing/slot/full/full-async/full-in-progress/generate-parallel/generate-skeleton/generate-product-script/generate-emotional/sort-suggest |
| LiveScriptQualityController | `/api/v1/live/ai` | check-violation/check-violation-enhanced/save-to-copy-if-passed |
| LiveScriptRefineController | `/api/v1/live/ai` | refine-script/refine-segment/chat-for-script/batch-chat-for-script/suggest-improvement/check-similarity |
| LiveScriptRecommendController | `/api/v1/live/ai` | recommend-scripts/chat-2h-strategy |
| DanmakuAnalysisController | `/api/v1/live/danmaku` | analyze/suggest |
| LiveScriptApprovalController | `/api/v1/live/script-approval` | submit/submit-by-session/review/revoke/search/history |
| LiveApprovalController | `/api/v1/live/approval` | submit/approve/reject/history/pending |
| LiveScriptCommentController | `/api/v1/live/script-comment` | by-script/by-session/save/resolve/delete/unresolved-count/unresolved-by-script |
| LiveCompetitiveInsightController | `/api/v1/live/competitive-insight` | search/get/save/delete |
| LiveCompetitorScriptController | `/api/v1/live/competitor-script` | search/get/save/delete |
| LiveCompetitorMonitorBridgeController | `/api/v1/live/competitor-monitor` | list |
| LiveCollaborationPresenceController | `/api/v1/live/collaboration` | join/leave/viewers |
| LiveDataSyncController | `/api/v1/live/data` | session/session/with-compare/session/save/session/sync/session/sync-from-douyin/product/history/product/save |
| LivePlatformRuleController | `/api/v1/live/platform` | list/violation-check/prompt-template |
| LiveRhythmController | `/api/v1/live/rhythm` | get-rhythm/save-rhythm |
| LiveScriptPipelineController | `/api/v1/live/pipeline` | start/status/cancel |
| LiveScriptNavigationController | `/api/v1/live/script-navigation` | next/{sessionId}/skip/{sessionId} |
| LiveAbTestAnalysisController | `/api/v1/live/ab-analysis` | record/recommend/summary |
| LiveAnalysisController | `/api/v1/live/analysis` | generate/get/review |
| ContentMaterialController | `/api/v1/live/material` | random/by-persona/categories/prompt/performance-prompt/risk-match |

### 状态
- `src/api/live.ts` ✅ 全量对齐（156 个 API 方法，覆盖全部 33 个 Controller）：
  - Session: CRUD/start/end/clone/trend/overview/readiness/exportToShortVideo/multiMetrics
  - SessionTemplate: search/get/save/delete/saveAs
  - Script: CRUD/bySession/effectiveness/executed/saveToLibrary/saveBatchToLibrary/export
  - ScriptTemplate: saveFromSession
  - Product: search/get/save/delete/bySession/batchSort
  - ScriptVersion: list/save/activate/delete/diff
  - Monitor: search/save/bySession/snapshot/push
  - RealtimePanel: init/nextSlot/saveTimer/saveData
  - Effectiveness: calculate/sessionRanking/compare/ranking/topScripts/recommended/emerged/scriptDetail
  - EffectivenessConfig: list/save/default/setDefault/delete
  - GenerationPreset: list/save/delete/getDefault/setDefault
  - GenerationTask: latest/create/updateProgress
  - AI Generation: generateOpening/Product/Transition/Closing/Slot/Full/FullAsync/FullInProgress/Parallel/Skeleton/ProductScript/Emotional/SortSuggest
  - AI Quality: checkViolation/checkViolationEnhanced/saveToCopyIfPassed
  - AI Refine: refineScript/refineSegment/chatForScript/batchChatForScript/suggestImprovement/checkSimilarity
  - AI Recommend: recommendScripts/chat2hStrategy
  - Danmaku: analyze/suggest
  - ScriptApproval: submit/submitBySession/review/revoke/search/history
  - Approval: submit/approve/reject/history/pending
  - ScriptComment: byScript/bySession/save/resolve/delete/unresolvedCount/unresolvedByScript
  - CompetitiveInsight: search/get/save/delete
  - CompetitorScript: search/get/save/delete
  - CompetitorMonitor: list
  - Collaboration: join/leave/viewers
  - DataSync: session/sessionWithCompare/sessionSave/sessionSync/sessionSyncFromDouyin/product/history/productSave
  - Platform: list/violationCheck/promptTemplate
  - Rhythm: getRhythm/saveRhythm
  - Pipeline: start/status/cancel
  - Navigation: nextSlot/skipToSlot
  - AbTest: record/recommend/summary
  - Analysis: generate/get/review
  - Material: random/byPersona/categories/prompt/performancePrompt/riskMatch
- `src/pages/live/SessionsPage.tsx` ✅ 完整 CRUD（含 start/end/clone）
- `src/pages/live/ScriptsPage.tsx` ✅ 完整 CRUD
- `src/pages/live/LiveWorkbenchPage.tsx` ✅ 话术工作台

### 待做
- [x] `live.ts` 补充全量接口 — ✅ 已完成（参考 docs/api/live-module.md）


---

## 4. product 模块

### 后端 API（`ProductController` + `ProductScriptVersionController` + `ScriptOptimizationController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/product/search` | POST | 商品分页 |
| `/api/v1/product/get` | POST | 商品详情 |
| `/api/v1/product/infer-product-type` | POST | 推断商品类型 |
| `/api/v1/product/trigger-extract` | POST | 触发特征提取 |
| `/api/v1/product/extract-from-link` | POST | 从链接提取商品 |
| `/api/v1/product/import-paiping` | POST | 导入拍品 |
| `/api/v1/product/script-version/list` | POST | 话术版本列表 |
| `/api/v1/product/script-version/save` | POST | 新增/编辑版本 |
| `/api/v1/product/script-version/delete/{id}` | DELETE | 删除版本 |
| `/api/v1/product/script-version/apply-from-library` | POST | 从话术库应用 |
| `/api/v1/product/script-version/best` | POST | 最优版本 |
| `/api/v1/product/script/analyze` | POST | 话术分析 |
| `/api/v1/product/script/suggestions` | POST | 优化建议 |
| `/api/v1/product/script/regenerate` | POST | 重新生成话术 |
| `/api/v1/product/script-effectiveness/ranking` | POST | 效果排行 |
| `/api/v1/product/script-effectiveness/compare` | POST | 效果对比 |
| `/api/v1/product/script-effectiveness/trend` | POST | 效果趋势 |

### 状态
- `src/api/product.ts` ✅ 全量对齐：list(路径修正为/product/search)/save/delete/publish/unpublish/setFeatured/extractFromLink/importPaiping/salesHistorySearch/Save/scriptVersionList/Save/Activate/Delete/stylePresetList/Save/effectivenessRanking/Compare
- `src/pages/product/ProductsPage.tsx` ✅ 深度升级（商品图片缩略图列CDN@!80X80 + 效果评分LinearProgress + AI卖点Tooltip截断 + 复选框批量删除 + rowHeight=56）

### 待做
- [x] `product.ts` 补充全量接口 — ✅ 已完成（参考 docs/api/product-module.md）

---

## 5. script 模块

### 后端 API（`ScriptController` + `ViolationWordAdminController` + `ComplianceController` + `HybridSearchController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/script/list` | POST | 话术分页 |
| `/api/v1/script/save` | POST | 新增/编辑话术 |
| `/api/v1/script/delete` | POST | 删除话术 |
| `/api/v1/script/admin/violation/list` | POST | 违规词列表（管理员）|
| `/api/v1/script/admin/violation/save` | POST | 新增/编辑违规词 |
| `/api/v1/script/admin/violation/delete` | POST | 删除违规词 |
| `/api/v1/script/admin/violation/active` | POST | 启用/停用违规词 |
| `/api/v1/script/admin/violation/import` | POST | 批量导入 |
| `/api/v1/script/admin/violation/export` | POST | 导出 |
| `/api/v1/script/compliance/check` | POST | 合规检测 |
| `/api/v1/script/compliance/rules` | POST | 合规规则列表 |
| `/api/v1/script/search/hybrid` | POST | 混合检索 |
| `/api/v1/script/search/suggest` | POST | 搜索建议 |

### 状态
- `src/api/script.ts` ✅ 全量对齐：list/save/delete/generate/updateUseCount + templateSearch/Get/Save/Delete/ByScene + violationList/Save/Delete/Toggle/PublicList/CheckBatch/SuggestReplacement(路径修正为/script/admin/violation/*) + complianceCheck/Rules + searchHybrid/Semantic/Suggest
- `src/pages/script/ScriptListPage.tsx` ✅ 完整 CRUD
- `src/pages/script/ViolationWordPage.tsx` ✅ 完整 CRUD

### 待做
- [x] `script.ts` 补充全量接口并修正路径 — ✅ 已完成（参考 docs/api/script-module.md）

---

## 6. shortvideo 模块

### 后端 API（`ShortVideoProjectController` + `AccountVideoCollectController` + `RemakeTemplateController` + `ShortVideoAiController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/short-video/project/list` | POST | 项目分页 |
| `/api/v1/short-video/project/save` | POST | 新增/编辑项目 |
| `/api/v1/short-video/project/delete` | POST | 删除项目 |
| `/api/v1/short-video/account-collect/start` | POST | 开始采集 |
| `/api/v1/short-video/account-collect/list` | POST | 采集任务列表 |
| `/api/v1/short-video/account-collect/status` | POST | 采集状态 |
| `/api/v1/short-video/account-collect/cancel` | POST | 取消采集 |
| `/api/v1/short-video/account-collect/videos` | POST | 已采集视频 |
| `/api/v1/short-video/remake-template/list` | POST | 改编模板列表 |
| `/api/v1/short-video/remake-template/save` | POST | 新增/编辑模板 |
| `/api/v1/short-video/remake-template/delete` | POST | 删除模板 |
| `/api/v1/short-video/remake-template/generate` | POST | 生成改编内容 |
| `/api/v1/short-video/ai/check-violation` | POST | AI 违规检测 |
| `/api/v1/short-video/ai/generate-copy` | POST | AI 生成文案 |

### 状态
- `src/api/shortvideo.ts` ✅ 全量对齐：project list/get/save/delete/generateDaily/exportScript + videoSearch/Get/Save/Delete/DataTrend(路径修正为/short-video/content/*) + collectStart/list/status/cancel/retry/videos + remakeTemplateList/save/delete/generate/createFromViral + viralList/analyze/favorite + aiGenerateCopy/Script/Title/CheckViolation
- `src/pages/shortvideo/ProjectsPage.tsx` ✅ 完整 CRUD
- `src/pages/shortvideo/AccountCollectPage.tsx` ✅ 采集任务列表
- `src/pages/shortvideo/RemakeTemplatePage.tsx` ✅ 改编模板 CRUD

### 待做
- [x] `shortvideo.ts` 全量对齐 — ✅ 已完成（参考 docs/api/shortvideo-module.md）


---

## 7. copy 模块

### 后端 API（`CopyLibraryController` + `CopyTemplateController` + `CopyAiController` + `CopyApprovalController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/copy/library/search` | POST | 文案库分页 |
| `/api/v1/copy/library/save` | POST | 新增/编辑文案 |
| `/api/v1/copy/library/delete` | POST | 删除文案 |
| `/api/v1/copy/library/update-status` | POST | 启用/停用 |
| `/api/v1/copy/library/increment-use-count` | POST | 增加使用次数 |
| `/api/v1/copy/template/search` | POST | 模板分页 |
| `/api/v1/copy/template/save` | POST | 新增/编辑模板 |
| `/api/v1/copy/template/delete` | POST | 删除模板 |
| `/api/v1/copy/template/update-status` | POST | 启用/停用模板 |
| `/api/v1/copy/ai/generate` | POST | AI 生成文案 |
| `/api/v1/copy/approval/search` | POST | 审批分页 |
| `/api/v1/copy/approval/save` | POST | 提交审批 |
| `/api/v1/copy/approval/delete` | POST | 删除审批 |

### 状态
- `src/api/copy.ts` ✅ 全量对齐：library list/get/save/delete/updateStatus/incrementUseCount + template list/get/save/delete/updateStatus + aiGenerate + approval search/get/save/delete
- `src/pages/copy/CopyLibraryPage.tsx` ✅ 完整 CRUD
- `src/pages/copy/CopyTemplatePage.tsx` ✅ 完整 CRUD

### 待做
- [x] `copy.ts` 补充全量接口 — ✅ 已完成（参考 docs/api/copy-module.md）

---

## 8. ai 模块

### 后端 API（`KnowledgeBaseController` + `EvolutionController` + `PromptTemplateController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/ai/knowledge-base/create` | POST | 创建知识库 |
| `/api/v1/ai/knowledge-base/list` | POST | 知识库列表 |
| `/api/v1/ai/knowledge-base/{kbId}/documents` | POST | 文档列表 |
| `/api/v1/ai/knowledge-base/{kbId}/upload-file` | POST | 上传文档 |
| `/api/v1/ai/knowledge-base/{kbId}/search` | POST | 知识库搜索 |
| `/api/v1/ai/knowledge-base/{kbId}/dedup-preview` | POST | 去重预览 |
| `/api/v1/ai/knowledge-base/import-from-path` | POST | 从路径导入 |
| `/api/v1/ai/knowledge-base/import-active-jobs` | POST | 导入任务列表 |
| `/api/v1/ai/knowledge-base/{kbId}` | DELETE | 删除知识库 |
| `/api/v1/ai/knowledge-base/document/{docId}` | DELETE | 删除文档 |
| `/api/v1/ai/evolution/viral/list` | POST | 病毒内容进化列表 |
| `/api/v1/ai/evolution/viral/trigger` | POST | 触发进化 |
| `/api/v1/ai/evolution/viral/complete` | POST | 完成进化 |
| `/api/v1/ai/evolution/viral/delete` | POST | 删除进化记录 |
| `/api/v1/ai/evolution/live-review/list` | POST | 直播复盘列表 |
| `/api/v1/ai/evolution/live-review/trigger` | POST | 触发复盘 |
| `/api/v1/ai/evolution/stats` | POST | 进化统计 |
| `/api/v1/ai/prompt-template/list` | POST | 提示词模板列表 |
| `/api/v1/ai/prompt-template/save` | POST | 新增/编辑模板 |
| `/api/v1/ai/prompt-template/delete` | POST | 删除模板 |
| `/api/v1/ai/prompt-template/get-active` | POST | 获取激活模板 |
| `/api/v1/ai/prompt-template/test-render` | POST | 测试渲染 |

### 状态
- `src/api/ai.ts` ✅ 全量对齐：kbList/Create/Delete/Search/DedupPreview/ImportFromPath/ImportActiveJobs + docList/Delete + evolveList/Trigger/Complete/Delete/Stats + liveReviewList/Trigger/Delete + promptTemplateList/Save/Delete/GetActive/TestRender + chat/generate + adminCallLogList/adminInfraModelList + brainKnowledgeGraphQuery/CausalInfer/TrendsCurrent + topicList/topicSave/topicDelete + evolveRoi + scoreTrend + kbSearch
- `src/pages/ai/KnowledgeBasePage.tsx` ✅ 完整实现（文档子抽屉 DataGrid + RAG 检索测试 Tab：输入问题→返回匹配分块评分）
- `src/pages/ai/EvolutionPage.tsx` ✅ 3-Tab 深度升级（进化任务列表 / 主题池管理 Chip+CRUD / ROI 指标卡+评分趋势折线图）

### 待做
- [x] `ai.ts` 补充全量接口 — ✅ 已完成（参考 docs/api/ai-module.md）

---

## 9. agent 模块

### 后端 API（`AgentController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/agent/list` | POST | 智能体分页 |
| `/api/v1/agent/get` | POST | 获取智能体详情 |
| `/api/v1/agent/save` | POST | 新增/编辑智能体 |
| `/api/v1/agent/delete` | POST | 删除智能体 |
| `/api/v1/agent/update-status` | POST | 启用/停用 |
| `/api/v1/agent/conversation/create` | POST | 创建会话 |
| `/api/v1/agent/conversation/list` | POST | 会话列表 |
| `/api/v1/agent/conversation/delete` | POST | 删除会话 |
| `/api/v1/agent/message/send` | POST | 发送消息 |
| `/api/v1/agent/message/list` | POST | 消息列表 |
| `/api/v1/agent/chat-stream` | POST | SSE 流式对话 |

### 状态
- `src/api/agent.ts` ✅ 全量对齐：list/get/save/delete/updateStatus + conversationCreate/list/delete + messageSend/list + chatStreamUrl(SSE)
- `src/pages/agent/AgentListPage.tsx` ✅ 基础 CRUD

### 待做
- [x] `agent.ts` 补充全量接口 — ✅ 已完成（参考 docs/api/agent-module.md）


---

## 10. abtest 模块

### 后端 API（`AbTestController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/abtest/experiment/list` | POST | 实验分页 |
| `/api/v1/abtest/experiment/get` | POST | 获取实验详情 |
| `/api/v1/abtest/experiment/save` | POST | 新增/编辑实验 |
| `/api/v1/abtest/experiment/delete` | POST | 删除实验 |
| `/api/v1/abtest/experiment/update-status` | POST | 启用/停用实验 |
| `/api/v1/abtest/experiment/set-winner` | POST | 设置获胜变体 |
| `/api/v1/abtest/experiment/result` | POST | 实验结果统计 |
| `/api/v1/abtest/experiment/daily-trend` | POST | 每日趋势 |
| `/api/v1/abtest/variant/save` | POST | 新增/编辑变体 |
| `/api/v1/abtest/variant/delete` | POST | 删除变体 |
| `/api/v1/abtest/event/record` | POST | 记录实验事件 |
| `/api/v1/abtest/script-style/assign` | POST | 分配话术风格 |
| `/api/v1/abtest/script-style/record-conversion` | POST | 记录转化 |

### 状态
- `src/api/abtest.ts` ✅ list/get/save/delete/updateStatus/setWinner/result/dailyTrend/variantSave/variantDelete/eventRecord
- `src/pages/abtest/ExperimentsPage.tsx` ✅ 完整实现（状态筛选 + 启动/暂停/结束切换 + 实验结果弹窗 + 变体对比表 + 设置获胜变体）

### 待做
- 已完整实现，无遗漏

---

## 11. wecom 模块

### 后端 API（`WecomController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/wecom/robot/list` | POST | 机器人分页 |
| `/api/v1/wecom/robot/get` | POST | 机器人详情 |
| `/api/v1/wecom/robot/save` | POST | 新增/编辑机器人 |
| `/api/v1/wecom/robot/delete` | POST | 删除机器人 |
| `/api/v1/wecom/robot/update-status` | POST | 启用/停用 |
| `/api/v1/wecom/rule/list` | POST | 推送规则列表 |
| `/api/v1/wecom/rule/get` | POST | 规则详情 |
| `/api/v1/wecom/rule/save` | POST | 新增/编辑规则 |
| `/api/v1/wecom/rule/delete` | POST | 删除规则 |
| `/api/v1/wecom/rule/update-status` | POST | 启用/停用规则 |
| `/api/v1/wecom/log/list` | POST | 推送日志列表 |
| `/api/v1/wecom/push` | POST | 手动推送 |

### 状态
- `src/api/wecom.ts` ✅ 全量对齐：robot list/get/save/delete/updateStatus/test/push + rule list/get/save/delete/updateStatus + log list（路径修正为 `/wecom/log/list`）
- `src/pages/wecom/RobotsPage.tsx` ✅ 完整实现（机器人CRUD + 推送对话框 + 推送日志Tab）

### 待做
- [x] `wecom.ts` 全量对齐：robot get/updateStatus + rule get/updateStatus + log路径修正 — ✅ 已完成

---

## 12. payment 模块

### 后端 API（`OrderController` + `SubscriptionController` + `RefundController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/payment/order/list` | POST | 订单分页 |
| `/api/v1/payment/order/get` | POST | 订单详情 |
| `/api/v1/payment/order/create` | POST | 创建订单 |
| `/api/v1/payment/order/confirmPayment` | POST | 确认支付 |
| `/api/v1/payment/order/ship` | POST | 发货 |
| `/api/v1/payment/order/complete` | POST | 完成订单 |
| `/api/v1/payment/order/getByOrderNo` | POST | 按单号查询 |
| `/api/v1/payment/subscription/current` | POST | 当前订阅 |
| `/api/v1/payment/subscription/upgrade` | POST | 升级订阅 |
| `/api/v1/payment/subscription/check-quota` | POST | 检查配额 |
| `/api/v1/payment/subscription/plans` | POST | 订阅计划列表 |

### 状态
- `src/api/payment.ts` ✅ 全量对齐：order list/detail/create/getByOrderNo/confirmPayment/ship/complete/cancel + refund create/get/listByOrder/approve/reject/complete + subscription current/upgrade/checkQuota/plans
- `src/pages/payment/OrdersPage.tsx` ✅ 已升级：状态筛选 + 确认/发货/完成/取消操作列 + 发货对话框

### 待做
- [x] `payment.ts` 补充全量接口 — ✅ 已完成（参考 docs/api/payment-module.md）

---

## 13. storage 模块

### 后端 API（`StorageController` + `UploadController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/storage/configured` | POST | 是否已配置存储 |
| `/api/v1/storage/list` | POST | 文件列表 |
| `/api/v1/storage/upload` | POST | 上传文件 |
| `/api/v1/storage/delete` | POST | 删除文件 |
| `/api/v1/storage/url` | POST | 获取文件 URL |
| `/api/v1/storage/upload/init` | POST | 初始化分块上传 |
| `/api/v1/storage/upload/chunk` | POST | 上传分块 |
| `/api/v1/storage/upload/complete` | POST | 合并分块 |
| `/api/v1/storage/upload/cancel` | POST | 取消上传 |

### 状态
- `src/api/storage.ts` ✅ 全量对齐：configured/list/upload/delete/url + 分片上传 uploadInit/uploadChunk/uploadComplete/uploadCancel（路径修正为 `/storage/*`，非 `/storage/file/*`）
- `src/pages/storage/StoragePage.tsx` ✅ 文件列表 + 上传 + 删除 + 链接预览

### 待做
- [x] `storage.ts` 全量对齐：configured + 分片上传 + 路径修正 — ✅ 已完成


---

## 14. log 模块

### 后端 API（`LogController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/log/operation/page` | POST | 操作日志分页 |
| `/api/v1/log/system/page` | POST | 系统日志分页 |
| `/api/v1/log/operation/export` | POST | 导出操作日志 |
| `/api/v1/log/system/export` | POST | 导出系统日志 |

### 状态
- `src/api/log.ts` ✅ operationList/systemList
- `src/pages/log/OperationLogPage.tsx` ✅ 基础实现

### 待做
- [x] `log.ts` 路径核实：路径已修正为 `/log/operation/page` + `/log/system/page` — ✅ 已完成
- [x] `OperationLogPage.tsx` 补充导出按钮 — ✅ 已完成

---

## 15. dashboard 模块

### 后端 API（`DashboardController` + `auth/DashboardController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/dashboard/admin/stats` | POST | 管理员统计数据 |
| `/api/v1/dashboard/org/stats` | POST | 机构统计数据 |
| `/api/v1/dashboard/kpi-unified` | POST | 统一 KPI |
| `/api/v1/dashboard/live-format-gmv` | POST | 直播 GMV 统计 |
| `/api/v1/dashboard/product-gmv-summary` | POST | 商品 GMV 汇总 |
| `/api/v1/dashboard/cockpit-preview` | POST | 驾驶舱预览 |
| `/api/v1/dashboard/profit-matrix-preview` | POST | 利润矩阵预览 |
| `/api/v1/dashboard/conversion-funnel` | POST | 转化漏斗 |
| `/api/v1/dashboard/cockpit-export` | POST | 导出驾驶舱数据 |
| `/api/v1/auth/dashboard/admin` | POST | auth 模块管理员统计 |

### 状态
- `src/api/dashboard.ts` ✅ adminStats/kpiUnified/liveFormatGmv/productGmvSummary/cockpitPreview/conversionFunnel
- `src/pages/DashboardPage.tsx` ✅ 完整实现（KPI条带 + 时间切换 + 转化漏斗 + 商品GMV排行 + 统计卡片）

### 待做
- 已完整实现，无遗漏

---

## 16. config 模块

### 后端 API（`ConfigController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/config/list` | POST | 配置项分页 |
| `/api/v1/config/get` | POST | 获取配置详情 |
| `/api/v1/config/save` | POST | 新增/编辑配置 |
| `/api/v1/config/delete` | POST | 删除配置 |

### 状态
- `src/api/config.ts` ✅ list/get/save/delete（config-module.md 4 个端点全覆盖）
- `src/pages/config/ConfigPage.tsx` ✅ 完整 CRUD

### 待做
- [x] `config.ts` 补充 get — ✅ 已完成

---

## 17. system 模块

### 后端 API（`SystemController`）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/system/api-log/list` | POST | API 调用日志分页 |
| `/api/v1/system/api-log/stats` | POST | API 日志统计 |
| `/api/v1/system/api-log/get` | POST | API 日志详情 |
| `/api/v1/system/sync-log/list` | POST | 同步日志分页 |
| `/api/v1/system/health` | POST | 系统健康状态 |
| `/api/v1/system/info` | POST | 系统信息 |

### 状态
- `src/api/system.ts` ✅ 全量对齐：info/health + apiLog list/stats/get + syncLog list + alertRule search/create/update/delete/enable/disable + alert search/active/acknowledge/resolve + metrics realtime/historical + taxonomy list/save/delete + externalApi list/save/delete/healthStatus
- `src/pages/system/SystemPage.tsx` ✅ 深度升级（4-Tab：系统信息 / 接口日志 / 同步日志 / 告警记录；告警Tab含确认操作+30s轮询）

### 待做
- [x] `system.ts` 全量对齐：health/apiLogGet/alertRule/alert/monitoring/taxonomy/externalApi — ✅ 已完成

---

## 18. attribution 模块

### 后端 API（`AttributionController` — 路径推断）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/attribution/summary` | POST | 归因汇总（渠道 GMV/转化） |

### 状态
- `src/pages/attribution/AttributionPage.tsx` ✅ 完整实现（卡片视图 + 7/30/90天切换）
- API 直接内联在页面，无独立 api 文件

### 待做
- [ ] 可将 `attributionApi` 抽取到 `src/api/attribution.ts`（非紧急）

---

## 19. 升级优先级清单

按影响范围和使用频率排序：

### P0 — 路径错误必须立即修复

| # | 文件 | 问题 | 修复 |
|---|------|------|------|
| 1 | `src/api/log.ts` | `/log/operation/list` → 后端实际 `/log/operation/page` | ✅ 已修复 |
| 2 | `src/api/copy.ts` | `templateList` 用 `/copy/template/list` → 后端实际 `/copy/template/search` | ✅ 已修复 |

### P1 — 功能缺失影响日常操作

| # | 文件 | 待补充内容 |
|---|------|------------|
| 3 | `src/pages/auth/LoginLogsPage.tsx` | ✅ 已升级为真实 DataGrid（含 username/status 筛选）|
| 4 | `src/pages/ai/EvolutionPage.tsx` | ✅ 已升级为真实 DataGrid（含触发/删除操作）|
| 5 | `src/pages/payment/OrdersPage.tsx` | ✅ 已升级：状态筛选 + 确认/发货/完成/取消操作列 + 发货对话框 |
| 6 | `src/pages/log/OperationLogPage.tsx` | ✅ 已完成：导出按钮（fetch 直接下载 xlsx）|
| 7 | `src/pages/live/SessionsPage.tsx` | ✅ 已有克隆按钮（`/live/session/clone`）|

### P2 — 扩展功能，按需实现

| # | 文件 | 内容 |
|---|------|------|
| 8 | `src/api/dashboard.ts` | ✅ 已完整实现（kpiUnified/liveFormatGmv/productGmvSummary/conversionFunnel）|
| 9 | `src/pages/DashboardPage.tsx` | ✅ 深度升级（KPI条带+漏斗+商品排行 + 近7天GMV/场次趋势折线图 + AI调用类型分布饼图 + 快捷入口4个按钮）|
| 10 | `src/api/abtest.ts` | ✅ 已完整实现（result/dailyTrend/updateStatus/setWinner/variantSave/Delete）|
| 11 | `src/api/wecom.ts` | ✅ 已完成（ruleList/ruleSave/ruleDelete/logList/push）|
| 12 | `src/api/storage.ts` | ✅ 已完成（getUrl + 文件预览）|
| 13 | `src/api/system.ts` | ✅ 已完成（apiLogList/stats/syncLogList/alertSearch/alertAcknowledge）|

### P2 新增完成

| # | 文件 | 内容 |
|---|------|------|
| 14 | `src/pages/live/LiveWorkbenchPage.tsx` | ✅ 新增话术工作台 |
| 15 | `src/pages/shortvideo/AccountCollectPage.tsx` | ✅ 新增账号采集页 |
| 16 | `src/pages/shortvideo/RemakeTemplatePage.tsx` | ✅ 新增改编模板页 |
| 17 | `src/api/shortvideo.ts` | ✅ 补充采集/改编/AI接口 |
| 18 | `src/api/payment.ts` | ✅ 补充 confirm/ship/complete/cancel |

### P3 — 可选新增页面

| # | 新页面 | 对应后端 |
|---|--------|----------|
| 19 | `src/pages/douyin/VideosPage.tsx` | ✅ 已完成（`/douyin/video/search`，含播放量/点赞/评论/分享列）|
| 20 | `src/pages/wecom/RulesPage.tsx` | ✅ 已完成（`/wecom/rule/list`，含触发类型筛选 + 完整 CRUD）|
| 21 | `src/api/attribution.ts` | 抽取归因 API（当前内联，非紧急）|

---

## 升级操作规范

1. **每次升级前**：对照本文档确认端点路径和参数
2. **修改 API 文件**：同步更新对应页面的调用
3. **修改页面**：确认 `queryKey` 与 API 函数名一致
4. **完成后**：运行 `npm run type-check` 确保无 TS 错误
5. **完成后**：更新本文档对应模块的「状态」标记（存根→完整）
