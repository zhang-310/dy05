# live 模块 API 文档

## 文件结构
```
config/LiveApiPathMigrationGuide.java
config/LiveAutoSyncScheduler.java
config/LiveBusinessMetricsCollector.java
config/LiveCrossSessionDecayScheduler.java
config/LiveDanmakuSentimentProperties.java
config/LiveGenerationAmqpConfig.java
config/LiveGenerationProperties.java
config/LiveGenerationRabbitListenerConfig.java
config/LiveGenerationTaskAmqpConsumer.java
config/LiveGmvReconciliationScheduler.java
config/LiveLearningMemoryScheduler.java
config/LiveMonitorArchiveScheduler.java
config/LivePromptConfig.java
config/LiveRealtimeProperties.java
config/LiveScriptTemplateScheduler.java
config/LiveSessionContentPolicyProperties.java
config/ScriptEventListeners.java
controller/ContentMaterialController.java
controller/DanmakuAnalysisController.java
controller/EffectivenessScoreController.java
controller/LiveAbTestAnalysisController.java
controller/LiveAnalysisController.java
controller/LiveApprovalController.java
controller/LiveCollaborationController.java
controller/LiveCollaborationPresenceController.java
controller/LiveCompetitiveInsightController.java
controller/LiveCompetitorMonitorBridgeController.java
controller/LiveCompetitorScriptController.java
controller/LiveDataSyncController.java
controller/LiveEffectivenessConfigController.java
controller/LiveGenerationPresetController.java
controller/LiveGenerationTaskController.java
controller/LiveMonitorController.java
controller/LiveMonitorSseController.java
controller/LivePlatformRuleController.java
controller/LiveProductController.java
controller/LiveRealtimePanelController.java
controller/LiveRhythmController.java
controller/LiveScriptApprovalController.java
controller/LiveScriptCommentController.java
controller/LiveScriptController.java
controller/LiveScriptCustomTemplateController.java
controller/LiveScriptGenerationController.java
controller/LiveScriptNavigationController.java
controller/LiveScriptPipelineController.java
controller/LiveScriptQualityController.java
controller/LiveScriptRecommendController.java
controller/LiveScriptRefineController.java
controller/LiveScriptTemplateController.java
controller/LiveScriptVersionController.java
controller/LiveSessionController.java
controller/LiveSessionTemplateController.java
controller/LiveStyleController.java
controller/LiveTemplateController.java
controller/ScriptQualityController.java
controller/package-info.java
entity/LiveAbTestResult.java
entity/LiveApprovalLog.java
entity/LiveCompetitiveInsight.java
entity/LiveCompetitorScript.java
entity/LiveDanmakuRecord.java
entity/LiveEffectivenessConfig.java
entity/LiveGenerationPreset.java
entity/LiveGenerationTask.java
entity/LiveLearningMemory.java
entity/LiveMonitor.java
entity/LivePlatform.java
entity/LiveProduct.java
entity/LiveProductData.java
entity/LiveScript.java
entity/LiveScriptAbTest.java
entity/LiveScriptApproval.java
entity/LiveScriptComment.java
entity/LiveScriptEffectiveness.java
entity/LiveScriptPipeline.java
entity/LiveScriptQualityScore.java
entity/LiveScriptTemplate.java
entity/LiveScriptVersion.java
entity/LiveSession.java
entity/LiveSessionData.java
entity/LiveSessionRealtimeData.java
entity/LiveSessionRealtimeViewerSample.java
entity/LiveSessionScriptSlot.java
entity/LiveSessionTemplate.java
entity/LiveSlotType.java
entity/LiveStylePreset.java
entity/LiveViolationRule.java
entity/ScriptUsageLog.java
entity/package-info.java
event/LiveSessionEndedEvent.java
event/LiveSessionEndedEventListener.java
package-info.java
realtime/LiveRealtimeSseHub.java
repository/LiveAbTestResultRepository.java
repository/LiveApprovalLogRepository.java
repository/LiveCompetitiveInsightRepository.java
repository/LiveCompetitorScriptRepository.java
repository/LiveDanmakuRecordRepository.java
repository/LiveEffectivenessConfigRepository.java
repository/LiveGenerationPresetRepository.java
repository/LiveGenerationTaskRepository.java
repository/LiveLearningMemoryRepository.java
repository/LiveMonitorRepository.java
repository/LivePlatformRepository.java
repository/LiveProductDataRepository.java
repository/LiveProductRepository.java
repository/LiveScriptAbTestRepository.java
repository/LiveScriptApprovalRepository.java
repository/LiveScriptCommentRepository.java
repository/LiveScriptEffectivenessRepository.java
repository/LiveScriptPipelineRepository.java
repository/LiveScriptQualityScoreRepository.java
repository/LiveScriptRepository.java
repository/LiveScriptTemplateRepository.java
repository/LiveScriptVersionRepository.java
repository/LiveSessionDataRepository.java
repository/LiveSessionRealtimeDataRepository.java
repository/LiveSessionRealtimeViewerSampleRepository.java
repository/LiveSessionRepository.java
repository/LiveSessionScriptSlotRepository.java
repository/LiveSessionTemplateRepository.java
repository/LiveSlotTypeRepository.java
repository/LiveStylePresetRepository.java
repository/LiveViolationRuleRepository.java
repository/ScriptUsageLogRepository.java
repository/package-info.java
service/ContentMaterialService.java
service/CrossSessionLearningService.java
service/DanmakuAnalysisService.java
service/DanmakuSentimentService.java
service/DouyinLiveDataSyncService.java
service/DouyinRealtimeMetricsService.java
service/EffectivenessScoreService.java
service/EmotionCurveEngine.java
service/GmvCalculationService.java
service/LiveAbTestAnalysisService.java
service/LiveAiModelResolver.java
service/LiveAiService.java
service/LiveAlertRuleEngine.java
service/LiveAnalysisService.java
service/LiveApprovalService.java
service/LiveCompetitiveInsightService.java
service/LiveCompetitorScriptService.java
service/LiveCrossModuleAdapter.java
service/LiveDataSyncService.java
service/LiveEffectivenessConfigService.java
service/LiveFullGenerationProgressTracker.java
service/LiveGenerationPresetService.java
service/LiveGenerationTaskService.java
service/LiveMetricsProvider.java
service/LiveMonitorService.java
service/LiveOfficialCompletionRateProvider.java
service/LiveOnlineLearningService.java
service/LivePanelTimerRedisService.java
service/LivePlatformRuleService.java
service/LiveProductService.java
service/LivePromptAbTestService.java
service/LivePromptBuilder.java
service/LivePromptContextBuilder.java
service/LivePromptFormatService.java
service/LiveRealtimePanelService.java
service/LiveRealtimeStrategyService.java
service/LiveRealtimeSuggestionService.java
service/LiveRhythmOptimizer.java
service/LiveScriptAbTestService.java
service/LiveScriptAnalysisService.java
service/LiveScriptApprovalService.java
service/LiveScriptAttributionService.java
service/LiveScriptCommentService.java
service/LiveScriptComplianceEnhancer.java
service/LiveScriptGenerationService.java
service/LiveScriptPipelineService.java
service/LiveScriptPromptService.java
service/LiveScriptQualityScoringService.java
service/LiveScriptQualityService.java
service/LiveScriptRecommendService.java
service/LiveScriptService.java
service/LiveScriptSkeletonService.java
service/LiveScriptStreamService.java
service/LiveScriptTemplateService.java
service/LiveScriptVersionService.java
service/LiveSessionReadService.java
service/LiveSessionService.java
service/LiveSessionShortVideoExportService.java
service/LiveSessionTemplateService.java
service/LiveSuggestionService.java
service/LiveTemplateService.java
service/ProductStrategyRecommender.java
service/ScriptQualityEvaluator.java
service/SmartProductScheduler.java
service/StyleLearningService.java
service/StyleRecommendService.java
service/SuggestionPushService.java
service/impl/ContentMaterialServiceImpl.java
service/impl/CrossSessionLearningServiceImpl.java
service/impl/DanmakuAnalysisServiceImpl.java
service/impl/DanmakuSentimentRedisServiceImpl.java
service/impl/DanmakuSentimentServiceImpl.java
service/impl/DefaultLiveOfficialCompletionRateProvider.java
service/impl/DouyinLiveDataSyncServiceImpl.java
service/impl/DouyinRealtimeMetricsServiceImpl.java
service/impl/EffectivenessScoreServiceImpl.java
service/impl/GmvCalculationServiceImpl.java
service/impl/LiveAbTestAnalysisServiceImpl.java
service/impl/LiveAiModelHelper.java
service/impl/LiveAiServiceImpl.java
service/impl/LiveAlertRuleEngineImpl.java
service/impl/LiveAnalysisServiceImpl.java
service/impl/LiveApprovalServiceImpl.java
service/impl/LiveCompetitiveInsightServiceImpl.java
service/impl/LiveCompetitorScriptServiceImpl.java
service/impl/LiveCrossModuleAdapterImpl.java
service/impl/LiveDataSyncServiceImpl.java
service/impl/LiveEffectivenessConfigServiceImpl.java
service/impl/LiveGenerationPresetServiceImpl.java
service/impl/LiveGenerationQueueProcessor.java
service/impl/LiveGenerationTaskServiceImpl.java
service/impl/LiveMetricsProviderImpl.java
service/impl/LiveMonitorServiceImpl.java
service/impl/LiveOnlineLearningServiceImpl.java
service/impl/LivePanelTimerRedisServiceImpl.java
service/impl/LivePlatformRuleServiceImpl.java
service/impl/LiveProductAiServiceImpl.java
service/impl/LiveProductServiceImpl.java
service/impl/LivePromptAbTestServiceImpl.java
service/impl/LivePromptContextBuilderImpl.java
service/impl/LivePromptFormatServiceImpl.java
service/impl/LiveRealtimeAutoSuggestionExecutor.java
service/impl/LiveRealtimePanelServiceImpl.java
service/impl/LiveRealtimeStrategyServiceImpl.java
service/impl/LiveRealtimeSuggestionServiceImpl.java
service/impl/LiveRhythmOptimizerImpl.java
service/impl/LiveScriptAbTestServiceImpl.java
service/impl/LiveScriptAnalysisServiceImpl.java
service/impl/LiveScriptApprovalServiceImpl.java
service/impl/LiveScriptAttributionServiceImpl.java
service/impl/LiveScriptCommentServiceImpl.java
service/impl/LiveScriptComplianceEnhancerImpl.java
service/impl/LiveScriptGenerationServiceImpl.java
service/impl/LiveScriptLlmUserPromptComposer.java
service/impl/LiveScriptPipelineServiceImpl.java
service/impl/LiveScriptPostProcessor.java
service/impl/LiveScriptPromptServiceImpl.java
service/impl/LiveScriptQualityScoringServiceImpl.java
service/impl/LiveScriptQualityServiceImpl.java
service/impl/LiveScriptRecommendServiceImpl.java
service/impl/LiveScriptServiceImpl.java
service/impl/LiveScriptSkeletonServiceImpl.java
service/impl/LiveScriptSlotGenerator.java
service/impl/LiveScriptStreamServiceImpl.java
service/impl/LiveScriptTemplateServiceImpl.java
service/impl/LiveScriptVersionServiceImpl.java
service/impl/LiveSessionReadServiceImpl.java
service/impl/LiveSessionServiceImpl.java
service/impl/LiveSessionShortVideoExportServiceImpl.java
service/impl/LiveSessionTemplateServiceImpl.java
service/impl/LiveSuggestionServiceImpl.java
service/impl/LiveTemplateServiceImpl.java
service/impl/ProductStrategyRecommenderImpl.java
service/impl/ScriptQualityEvaluatorImpl.java
service/impl/SmartProductSchedulerImpl.java
service/impl/StyleLearningServiceImpl.java
service/impl/StyleRecommendServiceImpl.java
service/impl/SuggestionPushServiceImpl.java
service/impl/package-info.java
service/package-info.java
util/LiveSessionSlotBlueprintParser.java
vo/AudienceProfileVO.java
vo/BatchChatVO.java
vo/CreateFromExistingVO.java
vo/DanmakuBatchIngestVO.java
vo/DanmakuIngestVO.java
vo/DanmakuSentimentSnapshotVO.java
vo/EmotionalScriptGenerateVO.java
vo/LiveAiFullResultVO.java
vo/LiveAiGenerateVO.java
vo/LiveAiResultVO.java
vo/LiveAnalysisVO.java
vo/LiveCompetitiveInsightSaveVO.java
vo/LiveCompetitiveInsightSearchVO.java
vo/LiveCompetitiveInsightVO.java
vo/LiveCompetitorScriptSaveVO.java
vo/LiveCompetitorScriptSearchVO.java
vo/LiveCompetitorScriptVO.java
vo/LiveEffectivenessConfigSaveVO.java
vo/LiveEffectivenessConfigVO.java
vo/LiveGenerationPresetSaveVO.java
vo/LiveGenerationPresetVO.java
vo/LiveGenerationTaskVO.java
vo/LiveHistoryItemVO.java
vo/LiveMonitorSearchVO.java
vo/LiveMonitorVO.java
vo/LiveMultiSessionRowVO.java
vo/LivePanelTimerSaveVO.java
vo/LivePanelTimerStateVO.java
vo/LivePersonaSnapshotVO.java
vo/LiveProductBatchItemVO.java
vo/LiveProductDataSaveVO.java
vo/LiveProductDataVO.java
vo/LiveProductReconcileRowVO.java
vo/LiveProductReconcileVO.java
vo/LiveProductSaveVO.java
vo/LiveProductSearchVO.java
vo/LiveProductSnapshotVO.java
vo/LiveProductVO.java
vo/LiveReadinessVO.java
vo/LiveReviewVO.java
vo/LiveScriptApprovalSaveVO.java
vo/LiveScriptApprovalSearchVO.java
vo/LiveScriptApprovalVO.java
vo/LiveScriptCommentSaveVO.java
vo/LiveScriptCommentVO.java
vo/LiveScriptSaveVO.java
vo/LiveScriptSearchVO.java
vo/LiveScriptVO.java
vo/LiveScriptVersionSaveVO.java
vo/LiveScriptVersionSearchVO.java
vo/LiveScriptVersionVO.java
vo/LiveSessionDataSaveVO.java
vo/LiveSessionDataVO.java
vo/LiveSessionExportToShortVideoResultVO.java
vo/LiveSessionOverviewVO.java
vo/LiveSessionRealtimeDataVO.java
vo/LiveSessionSaveVO.java
vo/LiveSessionScriptSlotVO.java
vo/LiveSessionSearchVO.java
vo/LiveSessionTemplateSaveVO.java
vo/LiveSessionTemplateSearchVO.java
vo/LiveSessionTemplateVO.java
vo/LiveSessionVO.java
vo/LiveStylePresetVO.java
vo/LiveTrendRequestVO.java
vo/LiveTrendResultVO.java
vo/MultiSessionMetricsRequestVO.java
vo/PanelInitVO.java
vo/ParallelGenerateResult.java
vo/ParallelGenerateVO.java
vo/ProductScriptGenerateVO.java
vo/ProductScriptResultVO.java
vo/RealtimeDataSaveVO.java
vo/RealtimeSuggestionVO.java
vo/SaveFromSessionVO.java
vo/SaveSessionAsTemplateVO.java
vo/SaveToCopyVO.java
vo/ScriptChatVO.java
vo/ScriptIdVO.java
vo/ScriptRecommendRequestVO.java
vo/ScriptRecommendVO.java
vo/ScriptRefineSegmentVO.java
vo/ScriptRefineVO.java
vo/SessionDataWithCompareVO.java
vo/SessionIdVO.java
vo/SetRecommendedVO.java
vo/SimilarityItemVO.java
vo/SkeletonGenerateVO.java
vo/SkeletonSlotVO.java
vo/SlotGenerateVO.java
vo/SlotOperationVO.java
vo/StyleRecommendationVO.java
vo/SuggestionExecuteVO.java
vo/UpdateVersionStatusVO.java
vo/UserSessionVersionsVO.java
vo/VersionDiffRequestVO.java
vo/VersionDiffVO.java
vo/package-info.java
```

## API 接口

### ContentMaterialController
```
@RequestMapping("/api/v1/live/material")
@PostMapping("/random")
public RESTResult<List<Map<String, Object>>> getRandomMaterials(@RequestBody Map<String, Object> params) {
@PostMapping("/by-persona")
public RESTResult<List<Map<String, Object>>> getMaterialsByPersona(@RequestBody Map<String, Object> params) {
@PostMapping("/categories")
public RESTResult<Map<String, List<String>>> getCategories() {
@PostMapping("/prompt")
public RESTResult<String> buildMaterialPrompt(@RequestBody Map<String, String> params) {
@PostMapping("/performance-prompt")
public RESTResult<String> getPerformancePrompt(@RequestBody Map<String, String> params) {
@PostMapping("/risk-match")
public RESTResult<List<String>> matchRiskScripts(@RequestBody Map<String, String> params) {
```

### DanmakuAnalysisController
```
@RequestMapping("/api/v1/live/danmaku")
@PostMapping("/analyze")
public RESTResult<Map<String, Object>> analyze(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/suggest")
public RESTResult<List<String>> suggest(@RequestBody Map<String, Object> body, HttpServletRequest request) {
```

### EffectivenessScoreController
```
@RequestMapping("/api/v1/live/effectiveness")
@PostMapping("/calculate")
public RESTResult<Map<String, Object>> calculateScore(@RequestBody Map<String, Object> body) {
@PostMapping("/session-ranking")
public RESTResult<List<Map<String, Object>>> calculateSessionRanking(@RequestBody Map<String, Object> body) {
@PostMapping("/compare")
public RESTResult<Map<String, Object>> compareVersions(@RequestBody Map<String, Object> body) {
@PostMapping("/ranking")
public RESTResult<PageResultVO<Map<String, Object>>> getRanking(@RequestBody Map<String, Object> body) {
@PostMapping("/top-scripts")
public RESTResult<List<Map<String, Object>>> getTopScripts(@RequestBody Map<String, Object> body) {
@PostMapping("/recommended-scripts")
public RESTResult<List<Map<String, Object>>> getRecommendedScripts(@RequestBody Map<String, Object> body) {
@PostMapping("/emerged-scripts")
public RESTResult<List<Map<String, Object>>> getEmergedScripts(@RequestBody Map<String, Object> body) {
@PostMapping("/script-effectiveness")
public RESTResult<Map<String, Object>> getScriptEffectiveness(@RequestBody Map<String, Object> body) {
```

### LiveAbTestAnalysisController
```
@RequestMapping("/api/v1/live/ab-analysis")
@PostMapping("/record")
public RESTResult<Void> record(HttpServletRequest request,
@PostMapping("/recommend")
public RESTResult<Map<String, Object>> recommend(HttpServletRequest request) {
@PostMapping("/summary")
public RESTResult<List<Map<String, Object>>> summary(HttpServletRequest request,
```

### LiveAnalysisController
```
@RequestMapping("/api/v1/live/analysis")
@PostMapping("/generate")
public RESTResult<LiveAnalysisVO> generate(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<LiveAnalysisVO> get(HttpServletRequest request,
@PostMapping("/review")
public RESTResult<LiveReviewVO> getReview(HttpServletRequest request,
```

### LiveApprovalController
```
@RequestMapping("/api/v1/live/approval")
@PostMapping("/submit")
public RESTResult<Void> submit(@CurrentUserId Long userId,
@PostMapping("/approve")
public RESTResult<Void> approve(@CurrentUserId Long userId,
@PostMapping("/reject")
public RESTResult<Void> reject(@CurrentUserId Long userId,
@PostMapping("/history")
public RESTResult<List<LiveApprovalLog>> history(@CurrentUserId Long userId,
@PostMapping("/pending")
public RESTResult<List<LiveSession>> pending(@CurrentUserId Long userId) {
```

### LiveCollaborationController
```
```

### LiveCollaborationPresenceController
```
@RequestMapping("/api/v1/live/collaboration")
@PostMapping("/join")
public RESTResult<Void> join(HttpServletRequest request,
@PostMapping("/leave")
public RESTResult<Void> leave(HttpServletRequest request,
@PostMapping("/viewers")
public RESTResult<List<Map<String, Object>>> viewers(HttpServletRequest request,
```

### LiveCompetitiveInsightController
```
@RequestMapping("/api/v1/live/competitive-insight")
@PostMapping("/search")
public RESTResult<PageResultVO<LiveCompetitiveInsightVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<LiveCompetitiveInsightVO> get(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody LiveCompetitiveInsightSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
```

### LiveCompetitorMonitorBridgeController
```
@RequestMapping("/api/v1/live/competitor-monitor")
@PostMapping("/list")
public RESTResult<List<Map<String, Object>>> list(HttpServletRequest request) {
```

### LiveCompetitorScriptController
```
@RequestMapping("/api/v1/live/competitor-script")
@PostMapping("/search")
public RESTResult<PageResultVO<LiveCompetitorScriptVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<LiveCompetitorScriptVO> get(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody LiveCompetitorScriptSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
```

### LiveDataSyncController
```
@RequestMapping("/api/v1/live/data")
@PostMapping("/session")
public RESTResult<LiveSessionDataVO> getSessionData(HttpServletRequest request,
@PostMapping("/session/with-compare")
public RESTResult<SessionDataWithCompareVO> getSessionDataWithCompare(HttpServletRequest request,
@PostMapping("/session/save")
public RESTResult<LiveSessionDataVO> saveSessionData(HttpServletRequest request,
@PostMapping("/session/sync")
public RESTResult<LiveSessionDataVO> syncSessionData(HttpServletRequest request,
@PostMapping("/session/sync-from-douyin")
public RESTResult<LiveSessionDataVO> syncFromDouyin(HttpServletRequest request,
@PostMapping("/product")
public RESTResult<List<LiveProductDataVO>> getProductData(HttpServletRequest request,
@PostMapping("/history")
public RESTResult<List<LiveHistoryItemVO>> getHistory(HttpServletRequest request,
@PostMapping("/product/save")
public RESTResult<LiveProductDataVO> saveProductData(HttpServletRequest request,
```

### LiveEffectivenessConfigController
```
@RequestMapping("/api/v1/live/effectiveness-config")
@PostMapping("/list")
public RESTResult<List<LiveEffectivenessConfigVO>> list(@CurrentUserId Long userId) {
@PostMapping("/save")
public RESTResult<LiveEffectivenessConfigVO> save(
@PostMapping("/default")
public RESTResult<LiveEffectivenessConfigVO> getDefault(@CurrentUserId Long userId) {
@PostMapping("/set-default")
public RESTResult<Void> setDefault(
@PostMapping("/delete")
public RESTResult<Integer> delete(
```

### LiveGenerationPresetController
```
@RequestMapping("/api/v1/live/generation-preset")
@PostMapping("/list")
public RESTResult<List<LiveGenerationPresetVO>> list(HttpServletRequest request) {
@PostMapping("/save")
public RESTResult<LiveGenerationPresetVO> save(HttpServletRequest request,
@PostMapping("/delete")
public RESTResult<Integer> delete(HttpServletRequest request,
@PostMapping("/getDefault")
public RESTResult<LiveGenerationPresetVO> getDefault(HttpServletRequest request) {
@PostMapping("/set-default")
public RESTResult<Void> setDefault(HttpServletRequest request,
```

### LiveGenerationTaskController
```
@RequestMapping("/api/v1/live/generation-task")
@PostMapping("/latest")
public RESTResult<LiveGenerationTaskVO> latest(HttpServletRequest request,
@PostMapping("/create")
public RESTResult<LiveGenerationTaskVO> create(HttpServletRequest request,
@PostMapping("/update-progress")
public RESTResult<Void> updateProgress(HttpServletRequest request,
```

### LiveMonitorController
```
@RequestMapping("/api/v1/live/monitor")
@PostMapping("/search")
public RESTResult<PageResultVO<LiveMonitorVO>> search(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/by-session")
public RESTResult<List<LiveMonitorVO>> getBySessionId(HttpServletRequest request,
```

### LiveMonitorSseController
```
@RequestMapping("/api/v1/live/monitor")
@GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/snapshot")
@PostMapping("/push")
```

### LivePlatformRuleController
```
@RequestMapping("/api/v1/live/platform")
@PostMapping("/list")
public RESTResult<List<LivePlatform>> listPlatforms() {
@PostMapping("/violation-check")
public RESTResult<List<Map<String, Object>>> checkViolation(@Valid @RequestBody ViolationCheckVO vo) {
@PostMapping("/prompt-template")
public RESTResult<String> getPromptTemplate(@Valid @RequestBody PlatformCodeVO vo) {
```

### LiveProductController
```
@RequestMapping("/api/v1/live/product")
@PostMapping("/search")
public RESTResult<PageResultVO<LiveProductVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<LiveProductVO> get(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
@PostMapping("/by-session")
public RESTResult<List<LiveProductVO>> getBySessionId(HttpServletRequest request,
@PostMapping("/batch-sort")
public RESTResult<Void> batchSort(HttpServletRequest request,
```

### LiveRealtimePanelController
```
@RequestMapping("/api/v1/live/realtime-panel")
@PostMapping("/init")
public RESTResult<PanelInitVO> initializePanel(HttpServletRequest request,
@GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/next-slot")
public RESTResult<LiveSessionScriptSlotVO> nextSlot(HttpServletRequest request,
@PostMapping("/prev-slot")
public RESTResult<LiveSessionScriptSlotVO> prevSlot(HttpServletRequest request,
@PostMapping("/jump-slot")
public RESTResult<LiveSessionScriptSlotVO> jumpSlot(HttpServletRequest request,
@PostMapping("/complete-slot")
public RESTResult<LiveSessionScriptSlotVO> completeSlot(HttpServletRequest request,
@PostMapping("/update-data")
public RESTResult<LiveSessionRealtimeDataVO> updateRealtimeData(HttpServletRequest request,
```

### LiveRhythmController
```
@RequestMapping("/api/v1/live/rhythm")
@PostMapping("/optimize")
public RESTResult<Map<String, Object>> optimize(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/product-strategy")
public RESTResult<Map<String, Object>> productStrategy(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/batch-order")
public RESTResult<Map<String, Object>> batchOrder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/save-rhythm")
public RESTResult<Map<String, Object>> saveRhythm(@RequestBody Map<String, Object> body,
```

### LiveScriptApprovalController
```
@RequestMapping("/api/v1/live/script-approval")
@PostMapping("/submit")
public RESTResult<LiveScriptApprovalVO> submit(
@PostMapping("/submit-by-session")
public RESTResult<java.util.Map<String, Object>> submitBySession(
@PostMapping("/review")
public RESTResult<LiveScriptApprovalVO> review(
@PostMapping("/revoke")
public RESTResult<Void> revoke(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/search")
public RESTResult<PageResultVO<LiveScriptApprovalVO>> search(
@PostMapping("/history")
public RESTResult<List<LiveScriptApprovalVO>> history(
```

### LiveScriptCommentController
```
@RequestMapping("/api/v1/live/script-comment")
@PostMapping("/by-script")
public RESTResult<List<LiveScriptCommentVO>> getByScript(
@PostMapping("/by-session")
public RESTResult<List<LiveScriptCommentVO>> getBySession(
@PostMapping("/save")
public RESTResult<LiveScriptCommentVO> save(
@PostMapping("/resolve")
public RESTResult<Void> resolve(
@PostMapping("/delete")
public RESTResult<Integer> delete(
@PostMapping("/unresolved-count")
public RESTResult<Long> unresolvedCount(
@PostMapping("/unresolved-by-script")
public RESTResult<Map<Long, Long>> unresolvedByScript(
```

### LiveScriptController
```
@RequestMapping("/api/v1/live/script")
@PostMapping("/search")
public RESTResult<PageResultVO<LiveScriptVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<LiveScriptVO> get(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
@PostMapping("/by-session")
public RESTResult<List<LiveScriptVO>> getBySessionId(HttpServletRequest request,
@PostMapping("/effectiveness")
public RESTResult<List<LiveScriptVO>> getEffectiveness(HttpServletRequest request,
@PostMapping("/executed")
public RESTResult<Void> updateExecuted(HttpServletRequest request,
@PostMapping("/save-to-library")
public RESTResult<Long> saveToLibrary(HttpServletRequest request,
@PostMapping("/save-batch-to-library")
public RESTResult<Integer> saveBatchToLibrary(HttpServletRequest request,
@PostMapping("/export")
public RESTResult<String> exportScripts(HttpServletRequest request,
```

### LiveScriptCustomTemplateController
```
@RequestMapping("/api/v1/live/script-template")
@PostMapping("/save-from-session")
public RESTResult<Long> saveFromSession(HttpServletRequest request,
```

### LiveScriptGenerationController
```
@RequestMapping("/api/v1/live/ai")
@PostMapping("/generate-opening")
public RESTResult<LiveAiResultVO> generateOpening(HttpServletRequest request,
@PostMapping("/generate-product")
public RESTResult<LiveAiResultVO> generateProduct(HttpServletRequest request,
@PostMapping("/generate-transition")
public RESTResult<LiveAiResultVO> generateTransition(HttpServletRequest request,
@PostMapping("/generate-closing")
public RESTResult<LiveAiResultVO> generateClosing(HttpServletRequest request,
@PostMapping("/generate-slot")
public RESTResult<String> generateForSlot(HttpServletRequest request,
@PostMapping(value = "/generate-slot-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/generate-full")
public RESTResult<List<LiveAiResultVO>> generateFull(HttpServletRequest request,
@PostMapping(value = "/generate-full-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping(value = "/generate-full-pipelined-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/generate-full-async")
public RESTResult<LiveGenerationTaskVO> generateFullAsync(HttpServletRequest request,
@PostMapping("/generate-full-in-progress")
public RESTResult<Map<String, Object>> generateFullInProgress(@RequestBody(required = false) SessionIdVO vo) {
@PostMapping("/generation-task/active")
public RESTResult<LiveGenerationTaskVO> getActiveGenerationTask(@RequestBody(required = false) SessionIdVO vo) {
@PostMapping("/generate-parallel")
public RESTResult<java.util.Map<Long, cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult>> generateParallel(
@PostMapping("/generate-skeleton")
public RESTResult<List<SkeletonSlotVO>> generateSkeleton(HttpServletRequest request,
@PostMapping(value = "/generate-skeleton-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/generate-product-script")
public RESTResult<ProductScriptResultVO> generateProductScript(HttpServletRequest request,
@PostMapping("/generate-emotional")
public RESTResult<LiveAiResultVO> generateEmotional(HttpServletRequest request,
@PostMapping("/sort-suggest")
public RESTResult<Map<String, Object>> sortSuggest(HttpServletRequest request,
public RESTResult<java.util.Map<Long, cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult>> generateParallelFallback(
```

### LiveScriptNavigationController
```
@RequestMapping("/api/v1/live/script-navigation")
@GetMapping("/current-slot/{sessionId}")
public RESTResult<Map<String, Object>> getCurrentSlot(@PathVariable Long sessionId) {
@GetMapping("/scripts/{sessionId}")
public RESTResult<List<Map<String, Object>>> getScriptSlots(@PathVariable Long sessionId) {
@GetMapping("/metrics/{sessionId}")
public RESTResult<Map<String, Object>> getRealtimeMetrics(@PathVariable Long sessionId) {
@PostMapping("/next/{sessionId}")
public RESTResult<Map<String, Object>> nextSlot(@PathVariable Long sessionId) {
@PostMapping("/skip/{sessionId}")
public RESTResult<Map<String, Object>> skipToSlot(@PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
```

### LiveScriptPipelineController
```
@RequestMapping("/api/v1/live/pipeline")
@PostMapping("/start")
public RESTResult<LiveScriptPipeline> start(HttpServletRequest request,
@PostMapping("/status")
public RESTResult<LiveScriptPipeline> status(HttpServletRequest request,
@PostMapping("/cancel")
public RESTResult<Void> cancel(HttpServletRequest request,
```

### LiveScriptQualityController
```
@RequestMapping("/api/v1/live/ai")
@PostMapping("/check-violation-enhanced")
public RESTResult<LiveAiResultVO.ViolationCheckResult> checkViolationEnhanced(
@PostMapping("/check-violation")
public RESTResult<LiveAiResultVO.ViolationCheckResult> checkViolation(HttpServletRequest request,
@PostMapping("/save-to-copy-if-passed")
public RESTResult<Map<String, Object>> saveToCopyIfPassed(HttpServletRequest request,
```

### LiveScriptRecommendController
```
@RequestMapping("/api/v1/live/ai")
@PostMapping("/recommend-scripts")
public RESTResult<List<ScriptRecommendVO>> recommendScripts(HttpServletRequest request,
@PostMapping("/chat-2h-strategy")
public RESTResult<Map<String, Object>> chat2hStrategy(HttpServletRequest request,
```

### LiveScriptRefineController
```
@RequestMapping("/api/v1/live/ai")
@PostMapping("/refine-script")
public RESTResult<String> refineScript(HttpServletRequest request,
@PostMapping("/refine-segment")
public RESTResult<String> refineSegment(HttpServletRequest request,
@PostMapping(value = "/refine-script-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/chat-for-script")
public RESTResult<String> chatForScript(HttpServletRequest request,
@PostMapping(value = "/chat-for-script-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/batch-chat-for-script")
public RESTResult<java.util.Map<Long, String>> batchChatForScript(HttpServletRequest request,
@PostMapping("/suggest-improvement")
public RESTResult<Map<String, Object>> suggestImprovement(HttpServletRequest request,
@PostMapping("/check-similarity")
public RESTResult<java.util.List<SimilarityItemVO>> checkSimilarity(HttpServletRequest request,
```

### LiveScriptTemplateController
```
@RequestMapping("/api/v1/live/template")
@PostMapping("/search")
public RESTResult<PageResultVO<LiveScriptTemplate>> search(
@PostMapping("/save-from-script")
public RESTResult<LiveScriptTemplate> saveFromScript(
@PostMapping("/apply")
public RESTResult<Map<String, Object>> apply(
@PostMapping("/auto-collect")
public RESTResult<Integer> autoCollect(HttpServletRequest request) {
```

### LiveScriptVersionController
```
@RequestMapping("/api/v1/live/script/version")
@PostMapping("/search")
public RESTResult<PageResultVO<LiveScriptVersionVO>> search(@Valid @RequestBody LiveScriptVersionSearchVO vo) {
@PostMapping("/get")
public RESTResult<LiveScriptVersionVO> getById(@RequestBody Long id) {
@PostMapping("/save")
public RESTResult<Long> save(@Valid @RequestBody LiveScriptVersionSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestBody Long id) {
@PostMapping("/getByScriptId")
public RESTResult<List<LiveScriptVersionVO>> getVersionsByScriptId(@RequestBody Long scriptId) {
@PostMapping("/getLatestVersion")
public RESTResult<LiveScriptVersionVO> getLatestVersion(@RequestBody Long scriptId) {
@PostMapping("/diff")
public RESTResult<VersionDiffVO> diffVersions(@RequestBody VersionDiffRequestVO requestVO) {
@PostMapping("/setRecommended")
public RESTResult<Void> setRecommended(@RequestBody SetRecommendedVO vo) {
@PostMapping("/cancelRecommended")
public RESTResult<Void> cancelRecommended(@RequestBody Long versionId) {
@PostMapping("/getRecommendedVersions")
public RESTResult<List<LiveScriptVersionVO>> getRecommendedVersions(@RequestBody Long scriptId) {
@PostMapping("/recommend")
public RESTResult<List<LiveScriptVersionVO>> recommendVersions(@RequestBody Long scriptId) {
@PostMapping("/updateStatus")
public RESTResult<Void> updateVersionStatus(@RequestBody UpdateVersionStatusVO vo) {
@PostMapping("/incrementUsageCount")
public RESTResult<Void> incrementUsageCount(@RequestBody Long versionId) {
@PostMapping("/like")
public RESTResult<Void> incrementLikedCount(@RequestBody Long versionId) {
@PostMapping("/getLatestByScriptIds")
public RESTResult<List<LiveScriptVersionVO>> getLatestVersionsByScriptIds(@RequestBody List<Long> scriptIds) {
@PostMapping("/getUserVersionsBySession")
public RESTResult<List<LiveScriptVersionVO>> getUserVersionsBySession(@RequestBody UserSessionVersionsVO vo) {
@PostMapping("/createFromExisting")
public RESTResult<Long> createVersionFromExisting(@RequestBody CreateFromExistingVO vo) {
```

### LiveSessionController
```
@RequestMapping("/api/v1/live/session")
@PostMapping("/search")
public RESTResult<PageResultVO<LiveSessionVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<LiveSessionVO> get(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
@PostMapping("/overview")
public RESTResult<LiveSessionOverviewVO> getOverview(HttpServletRequest request,
@PostMapping("/readiness")
public RESTResult<LiveReadinessVO> getReadiness(HttpServletRequest request,
@PostMapping("/status")
public RESTResult<Void> updateStatus(HttpServletRequest request,
@PostMapping("/viewers")
public RESTResult<Void> updateViewers(HttpServletRequest request,
@PostMapping("/likes")
public RESTResult<Void> updateLikes(HttpServletRequest request,
@PostMapping("/trend")
public RESTResult<LiveTrendResultVO> analyzeTrend(HttpServletRequest request,
@PostMapping("/clone")
public RESTResult<Long> clone(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/export-to-short-video")
public RESTResult<Long> exportToShortVideo(HttpServletRequest request, @RequestBody Map<String, Object> body) {
```

### LiveSessionTemplateController
```
@RequestMapping("/api/v1/live/session-template")
@PostMapping("/search")
public RESTResult<PageResultVO<LiveSessionTemplateVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<LiveSessionTemplateVO> get(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody LiveSessionTemplateSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
```

### LiveStyleController
```
@RequestMapping("/api/v1/live/style")
@PostMapping("/recommend")
public RESTResult<List<StyleRecommendationVO>> recommend(HttpServletRequest request,
```

### LiveTemplateController
```
@RequestMapping("/api/v1/live/session-template")
@PostMapping("/save-from-session")
public RESTResult<Long> saveFromSession(HttpServletRequest request,
```

### ScriptQualityController
```
@RequestMapping("/api/v1/live/script-quality")
@PostMapping("/evaluate")
public RESTResult<Map<String, Object>> evaluate(@RequestBody Map<String, String> params,
@PostMapping("/score")
public RESTResult<Map<String, Object>> score(@RequestBody Map<String, Object> params,
@PostMapping("/score-session")
public RESTResult<Map<String, Object>> scoreSession(@RequestBody Map<String, Object> params,
@PostMapping("/tts-preview")
public RESTResult<Map<String, Object>> ttsPreview(@RequestBody Map<String, Object> params,
```

### package-info
```
```

## Entity 字段

### LiveAbTestResult
```
@Id
private Long id;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "experiment_key", length = 100, nullable = false)
private String experimentKey;
@Column(name = "variant", length = 50)
private String variant;
@Column(name = "style", length = 50)
private String style;
@Column(name = "effectiveness_score", precision = 5, scale = 2)
private BigDecimal effectivenessScore;
@Column(name = "conversion_rate", precision = 5, scale = 4)
private BigDecimal conversionRate;
@Column(name = "interaction_rate", precision = 5, scale = 4)
private BigDecimal interactionRate;
@Column(name = "sample_size")
private Integer sampleSize = 0;
@Column(name = "confidence", precision = 5, scale = 4)
private BigDecimal confidence;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveApprovalLog
```
@Id
private Long id;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "script_id")
private Long scriptId;
@Column(name = "action", length = 16, nullable = false)
private String action;
@Column(name = "operator_id", nullable = false)
private Long operatorId;
@Column(name = "comment", columnDefinition = "TEXT")
private String comment;
@Column(name = "deleted", nullable = false)
private int deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### LiveCompetitiveInsight
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "session_id")
private Long sessionId;
@Column(name = "competitor_label", nullable = false, length = 128)
private String competitorLabel;
@Column(name = "product_price", precision = 12, scale = 2)
private BigDecimal productPrice;
@Column(name = "market_share_percent", precision = 6, scale = 2)
private BigDecimal marketSharePercent;
@Column(name = "gmv_estimate", precision = 14, scale = 2)
private BigDecimal gmvEstimate;
@Column(name = "win_loss_notes", columnDefinition = "TEXT")
private String winLossNotes;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveCompetitorScript
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(nullable = false, length = 256)
private String title;
@Column(name = "competitor_name", length = 128)
private String competitorName;
@Column(length = 32)
private String platform = "douyin";
@Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
private String scriptContent;
@Column(name = "source_url", length = 1024)
private String sourceUrl;
@Column(length = 256)
private String tags;
@Column(length = 512)
private String notes;
@Column(nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveDanmakuRecord
```
@Id
private Long id;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(nullable = false, columnDefinition = "TEXT")
private String content;
@Column(length = 20)
private String sentiment;
@Column(name = "author_nickname", length = 100)
private String authorNickname;
@Column(name = "danmaku_time")
private Timestamp danmakuTime;
@Column(name = "douyin_comment_id", length = 100)
private String douyinCommentId;
@Column(name = "user_id")
private Long userId;
@Column
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveEffectivenessConfig
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "config_name", nullable = false, length = 128)
private String configName = "默认配置";
@Column(name = "name", insertable = false, updatable = false)
private String name;
@Column(name = "conversion_weight", nullable = false, precision = 3, scale = 2)
private BigDecimal conversionWeight = new BigDecimal("0.30");
@Column(name = "interaction_weight", nullable = false, precision = 3, scale = 2)
private BigDecimal interactionWeight = new BigDecimal("0.25");
@Column(name = "retention_weight", nullable = false, precision = 3, scale = 2)
private BigDecimal retentionWeight = new BigDecimal("0.25");
@Column(name = "gmv_weight", nullable = false, precision = 3, scale = 2)
private BigDecimal gmvWeight = new BigDecimal("0.20");
@Column(name = "viewer_weight", precision = 5, scale = 2)
private BigDecimal viewerWeight = new BigDecimal("0.30");
@Column(name = "is_default")
private Integer isDefault = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveGenerationPreset
```
@Id
private Long id;
@Column(name = "name", nullable = false, length = 100)
private String name;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "style", length = 50)
private String style = "standard";
@Column(name = "model_id")
private Long modelId;
@Column(name = "use_kb_ref")
private Boolean useKbRef = true;
@Column(name = "duration_mode", length = 20)
private String durationMode = "standard";
@Column(name = "hot_keywords", columnDefinition = "TEXT")
private String hotKeywords;
@Column(name = "ip_type", length = 30)
private String ipType;
@Column(name = "material_type", length = 30)
private String materialType;
@Column(name = "script_module", length = 40)
private String scriptModule;
@Column(name = "retention_strategy", length = 30)
private String retentionStrategy;
@Column(name = "interaction_level", length = 20)
private String interactionLevel;
@Column(name = "is_default")
private Boolean isDefault = false;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveGenerationTask
```
@Id
private Long id;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "status", length = 20)
private String status = "running";
@Column(name = "total_slots")
private Integer totalSlots = 0;
@Column(name = "completed_slots")
private Integer completedSlots = 0;
@Column(name = "failed_slots")
private Integer failedSlots = 0;
@Column(name = "style", length = 50)
private String style;
@Column(name = "model_id")
private Long modelId;
@Column(name = "use_kb_ref")
private Boolean useKbRef = false;
@Column(name = "hot_keywords", columnDefinition = "TEXT")
private String hotKeywords;
@Column(name = "error_message", columnDefinition = "TEXT")
private String errorMessage;
@Column(name = "request_payload", columnDefinition = "TEXT")
private String requestPayload;
@Column(name = "execution_mode", length = 16)
private String executionMode;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveLearningMemory
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "org_id")
private Long orgId;
@Column(name = "category", length = 50)
private String category;
@Column(name = "insight_type", nullable = false, length = 30)
private String insightType;
@Column(name = "content", nullable = false, columnDefinition = "TEXT")
private String content;
@Column(name = "confidence", precision = 3, scale = 2)
private BigDecimal confidence = BigDecimal.ONE;
@Column(name = "source_session_id")
private Long sourceSessionId;
@Column(name = "usage_count")
private Integer usageCount = 0;
@Column(name = "last_used_at")
private Timestamp lastUsedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveMonitor
```
@Id
private Long id;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "timestamp", nullable = false)
private Timestamp timestamp;
@Column(name = "viewers")
private Integer viewers = 0;
@Column(name = "likes")
private Long likes = 0L;
@Column(name = "comments")
private Integer comments = 0;
@Column(name = "shares")
private Integer shares = 0;
@Column(name = "product_impressions")
private Integer productImpressions = 0;
@Column(name = "total_viewers")
private Integer totalViewers = 0;
@Column(name = "new_followers")
private Integer newFollowers = 0;
@Column(name = "online_count")
private Integer onlineCount = 0;
@Column(name = "gmv", precision = 12, scale = 2)
private BigDecimal gmv = BigDecimal.ZERO;
@Column(name = "orders")
private Integer orders = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### LivePlatform
```
@Id
private Long id;
@Column(name = "platform_code", nullable = false, unique = true, length = 32)
private String platformCode;
@Column(name = "platform_name", nullable = false, length = 64)
private String platformName;
@Column(name = "icon_url", length = 512)
private String iconUrl;
@Column(name = "prompt_template", columnDefinition = "TEXT")
private String promptTemplate;
@Column(name = "max_script_length")
private Integer maxScriptLength = 0;
@Column(name = "forbidden_topics", columnDefinition = "TEXT")
private String forbiddenTopics;
@Column(name = "active", nullable = false)
private Integer active = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveProduct
```
@Id
private Long id;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "product_id", nullable = false)
private Long productId;
@Column(name = "product_name", length = 256)
private String productName;
@Column(name = "sale_quantity")
private Integer saleQuantity = 0;
@Column(name = "revenue", precision = 12, scale = 2)
private BigDecimal revenue = BigDecimal.ZERO;
@Column(name = "position")
private Integer position;
@Column(name = "script_source", length = 32)
private String scriptSource = "session";
@Column(name = "product_script_id")
private Long productScriptId;
@Column(name = "product_type", length = 128)
private String productType;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveProductData
```
@Id
private Long id;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "product_id", nullable = false)
private Long productId;
@Column(name = "impressions", nullable = false)
private Integer impressions = 0;
@Column(name = "clicks", nullable = false)
private Integer clicks = 0;
@Column(name = "orders", nullable = false)
private Integer orders = 0;
@Column(name = "sale_quantity", nullable = false)
private Integer saleQuantity = 0;
@Column(name = "revenue", nullable = false, precision = 12, scale = 2)
private BigDecimal revenue = BigDecimal.ZERO;
@Column(name = "refund_quantity", nullable = false)
private Integer refundQuantity = 0;
@Column(name = "conversion_rate", nullable = false, precision = 5, scale = 4)
private BigDecimal conversionRate = BigDecimal.ZERO;
@Column(name = "sync_time")
private Timestamp syncTime;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveScript
```
@Id
private Long id;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
private String scriptContent;
@Column(name = "script_type", length = 32)
private String scriptType = "custom";
@Column(name = "style", length = 64)
private String style;
@Column(name = "ai_generated")
private Integer aiGenerated = 0;
@Column(name = "product_id")
private Long productId;
@Column(name = "ai_call_log_id")
private Long aiCallLogId;
@Column(name = "generation_status", length = 16)
private String generationStatus = "success";
@Column(name = "violation_checked")
private Integer violationChecked = 0;
@Column(name = "violation_result", columnDefinition = "TEXT")
private String violationResult;
@Column(name = "viewer_delta")
private Integer viewerDelta;
@Column(name = "interaction_delta")
private Integer interactionDelta;
@Column(name = "conversion_delta")
private Integer conversionDelta;
@Column(name = "effectiveness_score", precision = 5, scale = 2)
private BigDecimal effectivenessScore;
@Column(name = "sequence_no")
private Integer sequenceNo;
@Column(name = "execution_time")
private Long executionTime;
@Column(name = "duration_limit_sec")
private Integer durationLimitSec;
@Column(name = "requirement", length = 128)
private String requirement;
@Column(name = "executed")
private Integer executed = 0;
@Column(name = "referenced_script_id")
private Long referencedScriptId;
@Column(name = "referenced_script_snapshot", columnDefinition = "jsonb")
private String referencedScriptSnapshot;
@Column(name = "actual_execution_time")
private Timestamp actualExecutionTime;
@Column(name = "approval_status")
private Integer approvalStatus = 0;
@Column(name = "user_id")
private Long userId;
@Column(name = "generation_prompt_hash", length = 64)
private String generationPromptHash;
@Column(name = "ab_experiment_id")
private Long abExperimentId;
@Column(name = "ab_variant_id")
private Long abVariantId;
@Column(name = "ai_suggestion", columnDefinition = "TEXT")
private String aiSuggestion;
@Column(name = "prompt_template_id")
private Long promptTemplateId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveScriptAbTest
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "version_a_id", nullable = false)
private Long versionAId;
@Column(name = "version_b_id", nullable = false)
private Long versionBId;
@Column(name = "status", length = 16)
private String status = "running";
@Column(name = "traffic_split")
private Integer trafficSplit = 50;
@Column(name = "a_impressions")
private Integer aImpressions = 0;
@Column(name = "b_impressions")
private Integer bImpressions = 0;
@Column(name = "a_conversion_rate")
private Double aConversionRate;
@Column(name = "b_conversion_rate")
private Double bConversionRate;
@Column(name = "a_retention_rate")
private Double aRetentionRate;
@Column(name = "b_retention_rate")
private Double bRetentionRate;
@Column(name = "a_interaction_rate")
private Double aInteractionRate;
@Column(name = "b_interaction_rate")
private Double bInteractionRate;
@Column(name = "p_value")
private Double pValue;
@Column(name = "confidence_level")
private Double confidenceLevel;
@Column(name = "winner", length = 1)
private String winner;
@Column(name = "start_time")
private LocalDateTime startTime;
@Column(name = "end_time")
private LocalDateTime endTime;
@Column(name = "create_time")
private LocalDateTime createTime;
@Column(name = "update_time")
private LocalDateTime updateTime;
@Column(name = "deleted")
private Integer deleted = 0;
```

### LiveScriptApproval
```
@Id
private Long id;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "submitter_id", nullable = false)
private Long submitterId;
@Column(name = "reviewer_id")
private Long reviewerId;
@Column(name = "action", length = 16, nullable = false)
private String action = "submit";
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "comments", columnDefinition = "TEXT")
private String comments;
@Column(name = "review_time")
private Timestamp reviewTime;
@Column(name = "approval_level")
private Integer approvalLevel = 1;
@Column(name = "max_level")
private Integer maxLevel = 1;
@Column(name = "current_approver_id")
private Long currentApproverId;
@Column(name = "auto_approve_rule", length = 64)
private String autoApproveRule;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveScriptComment
```
@Id
private Long id;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "session_id")
private Long sessionId;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "author_id")
private Long authorId;
@Column(name = "user_name", length = 64)
private String userName;
@Column(name = "author_name", length = 100)
private String authorName;
@Column(name = "content", nullable = false, columnDefinition = "TEXT")
private String content;
@Column(name = "resolved", nullable = false)
private Integer resolved = 0;
@Column(name = "resolved_by")
private Long resolvedBy;
@Column(name = "resolved_at")
private LocalDateTime resolvedAt;
@Column(name = "parent_id")
private Long parentId;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveScriptEffectiveness
```
@Id
private Long id;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "version", length = 16)
private String version;
@Column(name = "conversion_rate", precision = 5, scale = 2)
private BigDecimal conversionRate = BigDecimal.ZERO;
@Column(name = "likes")
private Long likes = 0L;
@Column(name = "comments")
private Integer comments = 0;
@Column(name = "completion_rate", precision = 5, scale = 2)
private BigDecimal completionRate = BigDecimal.ZERO;
@Column(name = "total_score", precision = 4, scale = 2)
private BigDecimal totalScore = BigDecimal.ZERO;
@Column(name = "score_formula", length = 256)
private String scoreFormula;
@Column(name = "ranking")
private Integer ranking = 1;
@Column(name = "ranking_trend")
private Integer rankingTrend = 0;
@Column(name = "tag", length = 32)
private String tag;
@Column(name = "sample_size")
private Integer sampleSize = 0;
@Column(name = "calculated_at")
private Timestamp calculatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveScriptPipeline
```
@Id
private Long id;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "status", length = 20, nullable = false)
private String status = "pending";
@Column(name = "total_scripts")
private Integer totalScripts = 0;
@Column(name = "completed_scripts")
private Integer completedScripts = 0;
@Column(name = "refined_scripts")
private Integer refinedScripts = 0;
@Column(name = "failed_scripts")
private Integer failedScripts = 0;
@Column(name = "config_json", columnDefinition = "TEXT")
private String configJson;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveScriptQualityScore
```
@Id
private Long id;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "session_id")
private Long sessionId;
@Column(name = "compliance_score", precision = 5, scale = 2)
private BigDecimal complianceScore = BigDecimal.ZERO;
@Column(name = "fluency_score", precision = 5, scale = 2)
private BigDecimal fluencyScore = BigDecimal.ZERO;
@Column(name = "engagement_score", precision = 5, scale = 2)
private BigDecimal engagementScore = BigDecimal.ZERO;
@Column(name = "total_quality_score", precision = 5, scale = 2)
private BigDecimal totalQualityScore = BigDecimal.ZERO;
@Column(name = "details_json", columnDefinition = "JSONB")
private String detailsJson;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveScriptTemplate
```
@Id
private Long id;
@Column(name = "template_name", nullable = false, length = 128)
private String templateName;
@Column(name = "template_content", columnDefinition = "TEXT")
private String templateContent;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "auto_collected")
private Integer autoCollected = 0;
@Column(name = "script_type", nullable = false, length = 32)
private String scriptType;
@Column(name = "category", length = 64)
private String category;
@Column(name = "content", nullable = false, columnDefinition = "TEXT")
private String content;
@Column(name = "variables", length = 512)
private String variables;
@Column(name = "effectiveness_score", precision = 5, scale = 2)
private BigDecimal effectivenessScore = BigDecimal.ZERO;
@Column(name = "usage_count")
private Integer usageCount = 0;
@Column(name = "avg_conversion_rate", precision = 5, scale = 2)
private BigDecimal avgConversionRate = BigDecimal.ZERO;
@Column(name = "source_script_id")
private Long sourceScriptId;
@Column(name = "source_session_id")
private Long sourceSessionId;
@Column(name = "industry_code", length = 64)
private String industryCode;
@Column(name = "is_preset")
private Integer isPreset = 0;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveScriptVersion
```
@Id
private Long id;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "version_no", nullable = false)
private Integer versionNo;
@Column(name = "version_label", length = 64)
private String versionLabel;
@Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
private String scriptContent;
@Column(name = "script_type", length = 32)
private String scriptType;
@Column(name = "remark", columnDefinition = "TEXT")
private String remark;
@Column(name = "version_status", length = 16)
private String versionStatus = "draft";
@Column(name = "effectiveness_score", precision = 5, scale = 2)
private BigDecimal effectivenessScore;
@Column(name = "liked_count")
private Integer likedCount = 0;
@Column(name = "usage_count")
private Integer usageCount = 0;
@Column(name = "last_used_time")
private Timestamp lastUsedTime;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "is_recommended")
private Integer isRecommended = 0;
@Column(name = "recommend_reason", columnDefinition = "TEXT")
private String recommendReason;
@Column(name = "recommend_score", precision = 5, scale = 2)
private BigDecimal recommendScore;
@Column(name = "based_on_version_id")
private Long basedOnVersionId;
@Column(name = "change_summary", columnDefinition = "jsonb")
private String changeSummary;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveSession
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "account_id")
private Long accountId;
@Column(name = "persona_id")
private Long personaId;
@Column(name = "live_title", nullable = false, length = 256)
private String liveTitle;
@Column(name = "session_cover", length = 512)
private String sessionCover;
@Column(name = "script_style", length = 64)
private String scriptStyle = "professional";
@Column(name = "readiness_check", length = 512)
private String readinessCheck;
@Column(name = "live_description", columnDefinition = "TEXT")
private String liveDescription;
@Column(name = "scheduled_time")
private Timestamp scheduledTime;
@Column(name = "scheduled_end_time")
private Timestamp scheduledEndTime;
@Column(name = "start_time")
private Timestamp startTime;
@Column(name = "end_time")
private Timestamp endTime;
@Column(name = "live_url", length = 512)
private String liveUrl;
@Column(name = "viewers")
private Integer viewers = 0;
@Column(name = "likes")
private Long likes = 0L;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "recording_url", length = 512)
private String recordingUrl;
@Column(name = "recording_duration")
private Long recordingDuration;
@Column(name = "planned_end_time")
private Timestamp plannedEndTime;
@Column(name = "auto_sync_enabled", nullable = false)
private Integer autoSyncEnabled = 0;
@Column(name = "session_type", length = 32)
private String sessionType;
@Column(name = "live_format", length = 32)
private String liveFormat;
@Column(name = "org_id")
private Long orgId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveSessionData
```
@Id
private Long id;
@Column(name = "session_id", nullable = false, unique = true)
private Long sessionId;
@Column(name = "total_viewers", nullable = false)
private Integer totalViewers = 0;
@Column(name = "peak_viewers", nullable = false)
private Integer peakViewers = 0;
@Column(name = "total_likes", nullable = false)
private Long totalLikes = 0L;
@Column(name = "total_comments", nullable = false)
private Integer totalComments = 0;
@Column(name = "total_shares", nullable = false)
private Integer totalShares = 0;
@Column(name = "total_revenue", nullable = false, precision = 14, scale = 2)
private BigDecimal totalRevenue = BigDecimal.ZERO;
@Column(name = "total_orders", nullable = false)
private Integer totalOrders = 0;
@Column(name = "avg_stay_time", nullable = false)
private Integer avgStayTime = 0;
@Column(name = "new_followers", nullable = false)
private Integer newFollowers = 0;
@Column(name = "sync_time")
private Timestamp syncTime;
@Column(name = "ai_analysis", columnDefinition = "TEXT")
private String aiAnalysis;
@Column(name = "ai_review_id")
private Long aiReviewId;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### LiveSessionRealtimeData
```
@Id
@Column(name = "id")
private Long id;
@Column(name = "live_session_id", nullable = false)
private Long liveSessionId;
@Column(name = "watched_count")
private Integer watchedCount;
@Column(name = "viewer_count")
private Integer viewerCount;
@Column(name = "like_count")
private Integer likeCount;
@Column(name = "comment_count")
private Integer commentCount;
@Column(name = "share_count")
private Integer shareCount;
@Column(name = "follow_count")
private Integer followCount;
@Column(name = "gift_amount", precision = 10, scale = 2)
private BigDecimal giftAmount;
@Column(name = "product_click_count")
private Integer productClickCount;
@Column(name = "product_purchase_count")
private Integer productPurchaseCount;
@Column(name = "product_purchase_amount", precision = 10, scale = 2)
private BigDecimal productPurchaseAmount;
@Column(name = "current_slot_index")
private Integer currentSlotIndex;
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted;
```

### LiveSessionRealtimeViewerSample
```
@Id
private Long id;
@Column(name = "live_session_id", nullable = false)
private Long liveSessionId;
@Column(name = "viewer_count", nullable = false)
private Integer viewerCount = 0;
@Column(name = "sampled_at", nullable = false)
private LocalDateTime sampledAt;
@Column(name = "source", length = 32)
private String source;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### LiveSessionScriptSlot
```
@Id
@Column(name = "id")
private Long id;
@Column(name = "live_session_id", nullable = false)
private Long liveSessionId;
@Column(name = "slot_index", nullable = false)
private Integer slotIndex;
@Column(name = "script_version_id")
private Long scriptVersionId;
@Column(name = "content", columnDefinition = "TEXT", nullable = false)
private String content;
@Column(name = "duration_seconds")
private Integer durationSeconds;
@Column(name = "script_type", length = 32)
private String scriptType;
@Column(name = "style", length = 64)
private String style;
@Column(name = "is_current")
private Boolean isCurrent;
@Column(name = "is_completed")
private Boolean isCompleted;
@Column(name = "started_at")
private LocalDateTime startedAt;
@Column(name = "completed_at")
private LocalDateTime completedAt;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted;
```

### LiveSessionTemplate
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId = 0L;
@Column(nullable = false, length = 128)
private String name;
@Column(nullable = false, length = 64)
private String code;
@Column(length = 512)
private String description;
@Column(name = "structure_json", nullable = false, columnDefinition = "TEXT")
private String structureJson;
@Column(nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveSlotType
```
@Id
private Long id;
@Column(name = "slot_code", nullable = false, unique = true, length = 32)
private String slotCode;
@Column(name = "slot_label", nullable = false, length = 64)
private String slotLabel;
@Column(name = "default_requirement", length = 128)
private String defaultRequirement;
@Column(name = "sort_order")
private Integer sortOrder = 0;
@Column(name = "is_enabled")
private Boolean isEnabled = true;
@Column(name = "created_by")
private Long createdBy;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### LiveStylePreset
```
@Id
private Long id;
@Column(name = "style_key", nullable = false, unique = true, length = 50)
private String styleKey;
@Column(name = "label", nullable = false, length = 50)
private String label;
@Column(name = "group_name", nullable = false, length = 30)
private String groupName;
@Column(name = "prompt_template", nullable = false, columnDefinition = "TEXT")
private String promptTemplate;
@Column(name = "sort_order", nullable = false)
private Integer sortOrder = 0;
@Column(name = "active", nullable = false)
private Integer active = 1;
@Column(name = "create_time", nullable = false, updatable = false)
private LocalDateTime createTime;
@Column(name = "update_time", nullable = false)
private LocalDateTime updateTime;
```

### LiveViolationRule
```
@Id
private Long id;
@Column(name = "platform_id", nullable = false)
private Long platformId;
@Column(name = "word", nullable = false, length = 128)
private String word;
@Column(name = "level", nullable = false, length = 16)
private String level = "warning";
@Column(name = "reason", length = 256)
private String reason;
@Column(name = "replacement", length = 256)
private String replacement;
@Column(name = "category", length = 64)
private String category;
@Column(name = "active", nullable = false)
private Integer active = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### ScriptUsageLog
```
@Id
private Long id;
@Column(name = "script_source", nullable = false, length = 16)
private String scriptSource;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "session_id")
private Long sessionId;
@Column(name = "product_id")
private Long productId;
@Column(name = "script_type", length = 32)
private String scriptType;
@Column(name = "used_at")
private Timestamp usedAt;
@Column(name = "user_id")
private Long userId;
@Column(name = "ab_experiment_id")
private Long abExperimentId;
@Column(name = "ab_variant_id")
private Long abVariantId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### package-info
```
```

