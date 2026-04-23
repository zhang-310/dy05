# ai 模块 API 文档

## 文件结构
```
AiEmbeddingConstants.java
config/AiAttributionScheduler.java
config/AiCacheConfig.java
config/AiCircuitBreakerConfig.java
config/AiObservabilityConfig.java
config/AiRuntimeConfig.java
config/AiServicesHealthIndicator.java
config/AiViralDetectionScheduler.java
config/CausalFactorAdaptScheduler.java
config/ColdDocDetectionScheduler.java
config/CompetitorInsightScheduler.java
config/CrossDomainShareScheduler.java
config/DeepEvolveScheduler.java
config/DualWriteCompensationScheduler.java
config/EvolveDataInitializer.java
config/EvolveScheduler.java
config/EvolveTaskCompletionEventListener.java
config/EvolveTopicSync.java
config/FreshnessCheckScheduler.java
config/ImportJobStore.java
config/IndexQueueAmqpConfig.java
config/IndexQueueAmqpConsumer.java
config/IndexQueueConsumerScheduler.java
config/IndustryBrainNeo4jConfig.java
config/KbDocumentQualityScheduler.java
config/KnowledgeBaseInitializer.java
config/KnowledgeEvolutionScheduler.java
config/LiveScriptToKbImportScheduler.java
config/SchedulerHealthMonitor.java
config/SearchMetricsCollector.java
config/SearchSynonymsProperties.java
config/TopicDiscoveryScheduler.java
config/VectorCacheWarmup.java
config/VideoAnalysisTempCleanupScheduler.java
controller/AiAdminCallLogController.java
controller/AiAdminInfraController.java
controller/AiCallLogController.java
controller/AiController.java
controller/AiDashboardController.java
controller/AiQuotaController.java
controller/DigitalHumanController.java
controller/EvolutionController.java
controller/EvolutionReviewController.java
controller/EvolveController.java
controller/IndustryBrainController.java
controller/KnowledgeBaseController.java
controller/KnowledgeEvolutionController.java
controller/KnowledgeSourceController.java
controller/MediaController.java
controller/ModelBenchmarkController.java
controller/PromptTemplateController.java
controller/TaskModelConfigController.java
domain/CameraType.java
domain/QualityLevel.java
domain/VideoAspectRatio.java
entity/AiAgentWorkflowContext.java
entity/AiCallLog.java
entity/AiCallQuota.java
entity/AiCompetitorInsight.java
entity/AiEvolutionReviewTask.java
entity/AiEvolvePendingDeepen.java
entity/AiEvolveReport.java
entity/AiEvolveTask.java
entity/AiEvolveTopic.java
entity/AiGenerationTask.java
entity/AiGraphEdge.java
entity/AiGraphNode.java
entity/AiGraphRelationSuggestion.java
entity/AiHostPersona.java
entity/AiImageGeneration.java
entity/AiIndexQueue.java
entity/AiInferenceAudit.java
entity/AiKbDocument.java
entity/AiKnowledgeBase.java
entity/AiKnowledgeSource.java
entity/AiLiveReview.java
entity/AiModel.java
entity/AiModelBenchmark.java
entity/AiModelPricing.java
entity/AiModelRoutingLog.java
entity/AiPromptOptimizationLog.java
entity/AiPromptTemplate.java
entity/AiQueryLog.java
entity/AiSearchLog.java
entity/AiTaskModelConfig.java
entity/AiTtsGeneration.java
entity/AiUserCognitiveProfile.java
entity/AiViralAnalysis.java
entity/EvolutionRule.java
entity/KbFeedback.java
entity/KbImportCheckpoint.java
entity/KbImportReport.java
entity/KnowledgeDeduplicationGroup.java
entity/KnowledgeEvolutionExecution.java
entity/KnowledgeEvolutionLog.java
entity/KnowledgeEvolutionReport.java
entity/KnowledgeQualityScore.java
event/AbTestWinnerEvent.java
event/EvolveTaskCompletedEvent.java
exception/VideoGenerationException.java
repository/AiAgentWorkflowContextRepository.java
repository/AiCallLogRepository.java
repository/AiCallQuotaRepository.java
repository/AiCompetitorInsightRepository.java
repository/AiEvolutionReviewTaskRepository.java
repository/AiEvolvePendingDeepenRepository.java
repository/AiEvolveReportRepository.java
repository/AiEvolveTaskRepository.java
repository/AiEvolveTopicRepository.java
repository/AiGenerationTaskRepository.java
repository/AiGraphEdgeRepository.java
repository/AiGraphNodeRepository.java
repository/AiGraphRelationSuggestionRepository.java
repository/AiHostPersonaRepository.java
repository/AiImageGenerationRepository.java
repository/AiIndexQueueRepository.java
repository/AiInferenceAuditRepository.java
repository/AiKbDocumentRepository.java
repository/AiKnowledgeBaseRepository.java
repository/AiKnowledgeSourceRepository.java
repository/AiLiveReviewRepository.java
repository/AiModelBenchmarkRepository.java
repository/AiModelPricingRepository.java
repository/AiModelRepository.java
repository/AiModelRoutingLogRepository.java
repository/AiPromptOptimizationLogRepository.java
repository/AiPromptTemplateRepository.java
repository/AiQueryLogRepository.java
repository/AiSearchLogRepository.java
repository/AiTaskModelConfigRepository.java
repository/AiTtsGenerationRepository.java
repository/AiUserCognitiveProfileRepository.java
repository/AiViralAnalysisRepository.java
repository/EvolutionRuleRepository.java
repository/KbFeedbackRepository.java
repository/KbImportCheckpointRepository.java
repository/KbImportReportRepository.java
repository/KnowledgeDeduplicationGroupRepository.java
repository/KnowledgeEvolutionLogRepository.java
repository/KnowledgeQualityScoreRepository.java
search/QueryIntent.java
service/AgentOutputValidator.java
service/AgentVotingService.java
service/AiAdminInfraService.java
service/AiCallLogService.java
service/AiCostService.java
service/AiDashboardService.java
service/AiInferenceAuditService.java
service/AiMusicProvider.java
service/AiMusicService.java
service/AiPromptConfigService.java
service/AiQueryLogService.java
service/AiQuotaService.java
service/AiService.java
service/AiVideoProvider.java
service/AudioVideoJointService.java
service/CharacterIdentityService.java
service/CinematicKnowledgeService.java
service/CinematicPromptEngine.java
service/ColdDocDetectionService.java
service/ComfyUIService.java
service/CompetitorDataParser.java
service/CompetitorInsightService.java
service/ContentEffectivenessService.java
service/CrossDomainShareService.java
service/DeepEvolveService.java
service/DigitalHumanProvider.java
service/DocArchiveService.java
service/EvolutionAnalysisService.java
service/EvolutionDashboardService.java
service/EvolutionReportService.java
service/EvolutionReviewService.java
service/EvolutionRuleEngineService.java
service/EvolutionService.java
service/EvolutionStrategyService.java
service/EvolveDocumentLockService.java
service/EvolveEngineService.java
service/EvolveProgressStore.java
service/EvolveRoiService.java
service/EvolveTaskDagSupport.java
service/EvolveTopicImportService.java
service/EvolveTopicService.java
service/ExpiryUpdateService.java
service/FreshnessCheckService.java
service/GraphExtractorService.java
service/HuashuGenerateService.java
service/HuashuMaintenanceService.java
service/HydeExpansionService.java
service/ImageGenerationService.java
service/ImportAsyncRunner.java
service/ImportProgress.java
service/ImportRequirementsService.java
service/IndexQueueAmqpPublisher.java
service/IndexQueueConsumerService.java
service/IntelligentComposeService.java
service/IntelligentModelRouter.java
service/KbDocumentQualityService.java
service/KbHybridRetrieveService.java
service/KbSearchCacheService.java
service/KlingVideoService.java
service/KnowledgeBaseImportService.java
service/KnowledgeBaseService.java
service/KnowledgeBoostService.java
service/KnowledgeEvolutionService.java
service/KnowledgeQualityScoringService.java
service/KnowledgeSourceService.java
service/LiveScriptToKbImportService.java
service/LlmClient.java
service/LlmJudgeService.java
service/LlmToolAugmentedChatHelper.java
service/LlmToolOrchestratorService.java
service/ModelBenchmarkService.java
service/ModelChatStreamService.java
service/MultiAgentOrchestrator.java
service/PlaywrightDouyinDownloader.java
service/PromptSelfOptimizationService.java
service/PromptTemplateService.java
service/QualityScoreService.java
service/QueryIntentClassifier.java
service/QueryRewriteService.java
service/QuerySynonymExpansionService.java
service/RagService.java
service/RerankerService.java
service/SceneDetectionService.java
service/SearchLogService.java
service/SearchPersonalizationService.java
service/SearchService.java
service/SfxGenerationService.java
service/TaskModelConfigService.java
service/ToolRateLimiterService.java
service/TtsService.java
service/VectorService.java
service/VideoAnalysisService.java
service/VideoEditService.java
service/VideoGenerationService.java
service/VideoPostProcessingService.java
service/VideoQualityScoreService.java
service/VoiceCloneService.java
service/brain/AccountDiagnosisService.java
service/brain/FiveHostsSynergyService.java
service/brain/GrowthPathService.java
service/brain/HostPersonaService.java
service/brain/HostStyleConsistencyService.java
service/brain/IndustryCausalEngine.java
service/brain/IndustryKnowledgeGraphService.java
service/brain/IpGrowthStageService.java
service/brain/RiskWarningService.java
service/brain/StrategicPlanningService.java
service/brain/TrendMonitorService.java
service/brain/UserCognitiveProfileService.java
service/impl/AgentOutputValidatorImpl.java
service/impl/AgentVotingServiceImpl.java
service/impl/AiAdminInfraServiceImpl.java
service/impl/AiCallLogServiceImpl.java
service/impl/AiCostServiceImpl.java
service/impl/AiDashboardServiceImpl.java
service/impl/AiInferenceAuditServiceImpl.java
service/impl/AiKbDocumentCounterTransactionalService.java
service/impl/AiMusicServiceImpl.java
service/impl/AiPromptConfigServiceImpl.java
service/impl/AiQueryLogServiceImpl.java
service/impl/AiQuotaServiceImpl.java
service/impl/AiServiceImpl.java
service/impl/CharacterIdentityServiceImpl.java
service/impl/ColdDocDetectionServiceImpl.java
service/impl/ComfyUIServiceImpl.java
service/impl/CompetitorDataParserImpl.java
service/impl/CompetitorInsightServiceImpl.java
service/impl/ContentEffectivenessServiceImpl.java
service/impl/CrossDomainShareServiceImpl.java
service/impl/DeepEvolveServiceImpl.java
service/impl/DocArchiveServiceImpl.java
service/impl/EdgeTtsClient.java
service/impl/EvolutionAnalysisServiceImpl.java
service/impl/EvolutionDashboardServiceImpl.java
service/impl/EvolutionReportServiceImpl.java
service/impl/EvolutionReviewServiceImpl.java
service/impl/EvolutionRuleEngineServiceImpl.java
service/impl/EvolutionServiceImpl.java
service/impl/EvolutionStrategyServiceImpl.java
service/impl/EvolveAgentRunner.java
service/impl/EvolveEngineServiceImpl.java
service/impl/EvolvePromptBuilder.java
service/impl/EvolveReportProcessor.java
service/impl/EvolveRoiServiceImpl.java
service/impl/EvolveTopicImportServiceImpl.java
service/impl/EvolveTopicProcessor.java
service/impl/EvolveTopicServiceImpl.java
service/impl/ExpiryUpdateServiceImpl.java
service/impl/FfmpegFilterHelpProbe.java
service/impl/FreshnessCheckServiceImpl.java
service/impl/GraphExtractorServiceImpl.java
service/impl/HeyGenProvider.java
service/impl/HuashuGenerateServiceImpl.java
service/impl/HuashuMaintenanceServiceImpl.java
service/impl/HydeExpansionServiceImpl.java
service/impl/IflytekTtsClient.java
service/impl/ImageGenerationServiceImpl.java
service/impl/ImportRequirementsServiceImpl.java
service/impl/IndexQueueConsumerServiceImpl.java
service/impl/IntelligentComposeServiceImpl.java
service/impl/KbDocumentQualityServiceImpl.java
service/impl/KbHybridRetrieveServiceImpl.java
service/impl/KbSearchCacheServiceImpl.java
service/impl/Kling3VideoProvider.java
service/impl/KlingVideoProvider.java
service/impl/KlingVideoServiceImpl.java
service/impl/KnowledgeBaseImportServiceImpl.java
service/impl/KnowledgeBaseServiceImpl.java
service/impl/KnowledgeEvolutionServiceImpl.java
service/impl/KnowledgeQualityScoringServiceImpl.java
service/impl/KnowledgeSourceServiceImpl.java
service/impl/LiveScriptToKbImportServiceImpl.java
service/impl/LlmJudgeServiceImpl.java
service/impl/LumaVideoProvider.java
service/impl/MiniMaxVideoProvider.java
service/impl/ModelBenchmarkServiceImpl.java
service/impl/ModelChatStreamServiceImpl.java
service/impl/MultiAgentOrchestratorImpl.java
service/impl/OpenAiCompatibleLlmClient.java
service/impl/PikaVideoProvider.java
service/impl/PromptSelfOptimizationServiceImpl.java
service/impl/PromptTemplateServiceImpl.java
service/impl/QualityScoreServiceImpl.java
service/impl/QueryIntentClassifierImpl.java
service/impl/QueryRewriteServiceImpl.java
service/impl/QuerySynonymExpansionServiceImpl.java
service/impl/RagServiceImpl.java
service/impl/RerankerServiceImpl.java
service/impl/RunwayVideoProvider.java
service/impl/SearchLogServiceImpl.java
service/impl/SearchPersonalizationServiceImpl.java
service/impl/SearchServiceImpl.java
service/impl/Seedance2VideoProvider.java
service/impl/SfxGenerationServiceImpl.java
service/impl/SunoMusicProvider.java
service/impl/TaskModelConfigServiceImpl.java
service/impl/TtsServiceImpl.java
service/impl/UdioMusicProvider.java
service/impl/VectorServiceImpl.java
service/impl/VeoVideoProvider.java
service/impl/VideoComposeXfadeSupport.java
service/impl/VideoEditServiceImpl.java
service/impl/VideoGenerationServiceImpl.java
service/impl/VoiceCloneServiceImpl.java
service/impl/VolcengineArkImageService.java
service/impl/VolcengineArkVideoProvider.java
service/impl/WanVideoProvider.java
service/impl/brain/AccountDiagnosisServiceImpl.java
service/impl/brain/FiveHostsSynergyServiceImpl.java
service/impl/brain/GrowthPathServiceImpl.java
service/impl/brain/HostPersonaServiceImpl.java
service/impl/brain/HostStyleConsistencyServiceImpl.java
service/impl/brain/IndustryCausalEngineImpl.java
service/impl/brain/IndustryKnowledgeGraphServiceImpl.java
service/impl/brain/IpGrowthStageServiceImpl.java
service/impl/brain/RiskWarningServiceImpl.java
service/impl/brain/StrategicPlanningServiceImpl.java
service/impl/brain/TrendMonitorServiceImpl.java
service/impl/brain/UserCognitiveProfileServiceImpl.java
tool/LlmRegisteredTool.java
tool/LlmToolContext.java
tool/impl/CompetitorAnalysisLlmTool.java
tool/impl/KnowledgeBaseRagLlmTool.java
tool/impl/LiveSessionStatsTool.java
tool/impl/ProductSearchLlmTool.java
tool/impl/TrendQueryLlmTool.java
util/ChunkLabeler.java
util/ChunkResult.java
util/ContentFingerprintUtil.java
util/ContentSecurityScanner.java
util/DocumentParser.java
util/DocumentTypeDetector.java
util/EvolveModelOrderUtil.java
util/LlmTraceContext.java
util/MixedDocumentProcessor.java
util/PromptInjectionDetector.java
util/PromptSanitizer.java
util/ScriptAwareChunker.java
util/SimHashUtil.java
vo/AgentNodeResult.java
vo/AgentWorkflowVO.java
vo/AiPromptTemplateSaveVO.java
vo/AiPromptTemplateSearchVO.java
vo/CallLogSearchVO.java
vo/ChunkItemVO.java
vo/CounterfactualResultVO.java
vo/DedupPreviewVO.java
vo/EvolutionOpportunityVO.java
vo/EvolutionReportVO.java
vo/HotspotWindow.java
vo/HuashuGenerateRequestVO.java
vo/HuashuGenerateResponseVO.java
vo/InfraSearchVO.java
vo/KbCreateVO.java
vo/KbDocumentSearchVO.java
vo/KbDocumentUploadVO.java
vo/KbFeedbackVO.java
vo/KbImportVO.java
vo/KbSearchVO.java
vo/KnowledgeSourceSaveVO.java
vo/KnowledgeSourceSearchVO.java
vo/OpenClawRagRetrieveVO.java
vo/RagRetrieveItemVO.java
vo/TrendLifecycle.java
vo/TrendPredictionVO.java
vo/VideoCompareRequestVO.java
vo/VideoCompareResultVO.java
vo/VideoStyleAnalysisVO.java
```

## API 接口

### AiAdminCallLogController
```
@RequestMapping("/api/v1/ai/admin/call-log")
@PostMapping("/search")
public RESTResult<PageResultVO<AiCallLog>> search(
```

### AiAdminInfraController
```
@RequestMapping("/api/v1/ai/admin/infra")
@PostMapping("/monitoring-config")
public RESTResult<Map<String, Object>> getMonitoringConfig(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/health")
public RESTResult<List<InfraHealthItem>> health(HttpServletRequest request) {
@PostMapping("/detail")
public RESTResult<Map<String, Object>> getInfraDetail(HttpServletRequest request) {
@PostMapping("/documents/pg")
public RESTResult<PageResultVO<Map<String, Object>>> pagePgDocuments(
@PostMapping("/documents/es")
public RESTResult<PageResultVO<Map<String, Object>>> pageEsDocuments(
@PostMapping("/milvus/stats")
public RESTResult<Map<String, Object>> getMilvusStats(
@PostMapping("/cache/stats")
public RESTResult<Map<String, Object>> getCacheStats(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/search/stats")
public RESTResult<Map<String, Object>> getSearchStats(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/queue")
public RESTResult<PageResultVO<Map<String, Object>>> pageIndexQueue(
```

### AiCallLogController
```
@RequestMapping("/api/v1/ai/call-log")
@PostMapping("/link")
public RESTResult<Void> link(
```

### AiController
```
@RequestMapping("/api/v1/ai")
@PostMapping("/model/list")
public RESTResult<List<Map<String, Object>>> modelList(HttpServletRequest request,
@PostMapping("/model/get")
public RESTResult<Map<String, Object>> modelGet(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/model/save")
public RESTResult<Long> modelSave(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/model/delete")
public RESTResult<Void> modelDelete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/task/list")
public RESTResult<PageResultVO<Map<String, Object>>> taskList(HttpServletRequest request,
@PostMapping("/task/get")
public RESTResult<Map<String, Object>> taskGet(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/task/create")
public RESTResult<Long> taskCreate(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/task/complete")
public RESTResult<Void> taskComplete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/prompt/list")
public RESTResult<List<Map<String, Object>>> promptList(HttpServletRequest request,
@PostMapping("/prompt/save")
public RESTResult<Long> promptSave(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/prompt/delete")
public RESTResult<Void> promptDelete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/knowledge/list")
public RESTResult<List<Map<String, Object>>> knowledgeList(HttpServletRequest request) {
@PostMapping("/knowledge/get")
public RESTResult<Map<String, Object>> knowledgeGet(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/knowledge/save")
public RESTResult<Long> knowledgeSave(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/knowledge/delete")
public RESTResult<Void> knowledgeDelete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/knowledge/status")
public RESTResult<Void> knowledgeStatus(HttpServletRequest request, @RequestBody Map<String, Object> body) {
```

### AiDashboardController
```
@RequestMapping("/api/v1/ai/admin/dashboard")
@PostMapping("/call-volume-trend")
public RESTResult<List<Map<String, Object>>> callVolumeTrend(
@PostMapping("/quota-trend")
public RESTResult<List<Map<String, Object>>> quotaTrend(
@PostMapping("/call-type-distribution")
public RESTResult<List<Map<String, Object>>> callTypeDistribution(
```

### AiQuotaController
```
@RequestMapping("/api/v1/ai/quota")
@PostMapping("/info")
public RESTResult<Map<String, Object>> getQuotaInfo(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
```

### DigitalHumanController
```
@RequestMapping("/api/v1/ai/digital-human")
@PostMapping("/status")
public RESTResult<Map<String, Object>> status(HttpServletRequest request) {
@PostMapping("/generate")
public RESTResult<Map<String, Object>> generate(@RequestBody Map<String, String> body, HttpServletRequest request) {
```

### EvolutionController
```
@RequestMapping("/api/v1/ai/evolution")
@PostMapping("/viral/list")
public RESTResult<PageResultVO<Map<String, Object>>> viralList(
@PostMapping("/viral/get")
public RESTResult<Map<String, Object>> viralGet(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/viral/trigger")
public RESTResult<Long> viralTrigger(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/viral/complete")
public RESTResult<Void> viralComplete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/viral/delete")
public RESTResult<Void> viralDelete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/live-review/list")
public RESTResult<PageResultVO<Map<String, Object>>> liveReviewList(
@PostMapping("/live-review/get")
public RESTResult<Map<String, Object>> liveReviewGet(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/live-review/trigger")
public RESTResult<Long> liveReviewTrigger(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/live-review/complete")
public RESTResult<Void> liveReviewComplete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/live-review/delete")
public RESTResult<Void> liveReviewDelete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/video/compare")
public RESTResult<VideoCompareResultVO> videoCompare(
@PostMapping("/stats")
public RESTResult<Map<String, Object>> stats(HttpServletRequest request) {
```

### EvolutionReviewController
```
@RequestMapping("/api/v1/ai/evolution-review")
@PostMapping("/list")
public RESTResult<PageResultVO<Map<String, Object>>> list(
@PostMapping("/approve")
public RESTResult<Void> approve(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/reject")
public RESTResult<Void> reject(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/revise")
public RESTResult<Void> revise(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/stats")
public RESTResult<Map<String, Object>> stats(HttpServletRequest request) {
```

### EvolveController
```
@RequestMapping("/api/v1/ai/admin/evolve")
@PostMapping("/trigger")
public RESTResult<Map<String, Object>> trigger(
@PostMapping("/status")
public RESTResult<Map<String, Object>> status() {
@PostMapping("/topic/list")
public RESTResult<List<AiEvolveTopic>> listTopics(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/topic/save")
public RESTResult<AiEvolveTopic> saveTopic(@RequestBody AiEvolveTopic topic) {
@PostMapping("/topic/import-from-file")
public RESTResult<EvolveTopicImportService.TopicImportResult> importTopics(
@DeleteMapping("/topic/{id}")
public RESTResult<Void> deleteTopic(@PathVariable Long id) {
@PostMapping("/task/list")
public RESTResult<List<AiEvolveTask>> listTasks(@RequestBody(required = false) Map<String, Object> body) {
@DeleteMapping("/task/{id}")
public RESTResult<Void> deleteTask(@PathVariable Long id) {
@PostMapping("/report/by-task")
public RESTResult<AiEvolveReport> getReport(@RequestBody Map<String, Object> body) {
```

### IndustryBrainController
```
@RequestMapping("/api/v1/ai/brain")
@PostMapping("/knowledge-graph/query")
public RESTResult<List<Map<String, Object>>> knowledgeGraphQuery(
@PostMapping("/knowledge-graph/subgraph-json")
public RESTResult<Map<String, Object>> knowledgeGraphSubgraphJson(
@PostMapping("/knowledge-graph/graphrag-context")
public RESTResult<Map<String, Object>> graphRagContext(
@PostMapping("/knowledge-graph/relation-suggestions/list")
public RESTResult<List<Map<String, Object>>> relationSuggestionsList(
@PostMapping("/knowledge-graph/relation-suggestions/materialize")
public RESTResult<java.util.Map<String, Object>> relationSuggestionsMaterialize(
@PostMapping("/knowledge-graph/relation-suggestions/update-status")
public RESTResult<Void> relationSuggestionsUpdateStatus(
@PostMapping("/causal/infer")
public RESTResult<IndustryCausalEngine.CausalInferenceResult> causalInfer(
@PostMapping("/host-personas")
public RESTResult<List<cn.gaifan.douyinOperations.module.ai.entity.AiHostPersona>> hostPersonas(
@PostMapping("/trends/for-host")
public RESTResult<List<TrendMonitorService.TrendSignal>> trendsForHost(
@PostMapping("/trends/current")
public RESTResult<List<TrendMonitorService.TrendSignal>> trendsCurrent(
@PostMapping("/user-profile/{userId}")
public RESTResult<UserCognitiveProfileService.UserProfile> userProfile(
@PostMapping("/account/diagnose")
public RESTResult<AccountDiagnosisService.DiagnosisResult> accountDiagnose(
@PostMapping("/strategic/plan")
public RESTResult<StrategicPlanningService.StrategicPlan> strategicPlan(
@PostMapping("/risk/warn")
public RESTResult<List<RiskWarningService.RiskItem>> riskWarn(
@PostMapping("/style-consistency")
public RESTResult<Map<String, Object>> styleConsistency(
@PostMapping("/synergy")
public RESTResult<Map<String, Object>> synergy(HttpServletRequest request) {
@PostMapping("/growth-path")
public RESTResult<GrowthPathService.GrowthPathResult> growthPath(
@PostMapping("/content-diagnosis")
public RESTResult<Map<String, Object>> contentDiagnosis(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/product-diagnosis")
public RESTResult<Map<String, Object>> productDiagnosis(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/rhythm-diagnosis")
public RESTResult<Map<String, Object>> rhythmDiagnosis(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/ip-growth-stage")
public RESTResult<Map<String, Object>> ipGrowthStage(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/ip-metrics-baseline")
public RESTResult<Map<String, Object>> ipMetricsBaseline(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/causal/counterfactual")
public RESTResult<cn.gaifan.douyinOperations.module.ai.vo.CounterfactualResultVO> causalCounterfactual(
@PostMapping("/causal/explain-strategy")
public RESTResult<Map<String, Object>> causalExplainStrategy(
@PostMapping("/trends/with-lifecycle")
public RESTResult<List<cn.gaifan.douyinOperations.module.ai.vo.TrendPredictionVO>> trendsWithLifecycle(
@PostMapping("/trends/detect-new")
public RESTResult<List<TrendMonitorService.TrendSignal>> detectNewTrends(HttpServletRequest request) {
@PostMapping("/account/diagnose-batch")
public RESTResult<List<AccountDiagnosisService.DiagnosisResult>> accountDiagnoseBatch(
@PostMapping("/risk/warn-batch")
public RESTResult<List<RiskWarningService.RiskItem>> riskWarnBatch(
@PostMapping("/risk/stats")
public RESTResult<RiskWarningService.RiskStats> riskStats(HttpServletRequest request) {
```

### KnowledgeBaseController
```
@RequestMapping("/api/v1/ai/knowledge-base")
@PostMapping("/create")
public RESTResult<AiKnowledgeBase> createKnowledgeBase(
@DeleteMapping("/{kbId:\\d+}")
public RESTResult<Void> deleteKnowledgeBase(
@PostMapping("/list")
public RESTResult<List<AiKnowledgeBase>> listKnowledgeBases(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest httpRequest) {
@PostMapping("/{kbId:\\d+}/document")
public RESTResult<AiKbDocument> uploadDocument(
@DeleteMapping("/document/{docId:\\d+}")
public RESTResult<Void> deleteDocument(
@PostMapping("/{kbId:\\d+}/documents")
public RESTResult<List<AiKbDocument>> listDocuments(
@PostMapping("/{kbId:\\d+}/import-reports")
public RESTResult<List<KbImportReport>> listImportReports(
@PostMapping("/import-requirements")
public RESTResult<java.util.Map<String, String>> checkImportRequirements() {
@PostMapping("/import-from-path")
public RESTResult<KnowledgeBaseImportService.ImportResult> importFromPath(
@PostMapping("/import-from-path-async")
public RESTResult<Map<String, String>> importFromPathAsync(
@PostMapping("/import-active-jobs")
public RESTResult<Map<String, Object>> getActiveImportJobs(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/import-status/{jobId}")
public RESTResult<Map<String, Object>> getImportStatus(@PathVariable String jobId, @RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/feedback")
public RESTResult<Void> submitFeedback(
@PostMapping("/{kbId:\\d+}/search")
public RESTResult<List<KnowledgeBaseService.SearchResult>> search(
@PostMapping("/{kbId:\\d+}/upload-file")
public RESTResult<AiKbDocument> uploadFile(
@PostMapping("/{kbId:\\d+}/upload-files")
public RESTResult<Map<String, Object>> uploadFiles(
@PostMapping("/{kbId:\\d+}/dedup-preview")
public RESTResult<DedupPreviewVO> dedupPreview(
@PostMapping("/{kbId:\\d+}/documents/{docId:\\d+}/chunks")
public RESTResult<List<ChunkItemVO>> getDocumentChunks(
@PostMapping("/import-incremental")
public RESTResult<KnowledgeBaseImportService.ImportResult> importIncremental(
```

### KnowledgeEvolutionController
```
@RequestMapping("/api/v1/ai/knowledge-evolution")
@PostMapping("/analyze")
public RESTResult<EvolutionOpportunityVO> analyzeEvolutionOpportunities(
@PostMapping("/auto-optimize")
public RESTResult<Map<String, Object>> executeAutoOptimization(
@PostMapping("/report")
public RESTResult<EvolutionReportVO> generateEvolutionReport(
@PostMapping("/deduplicate")
public RESTResult<Map<String, Object>> deduplicateKnowledge(
@PostMapping("/history")
public RESTResult<PageResultVO<Map<String, Object>>> getEvolutionHistory(
```

### KnowledgeSourceController
```
@RequestMapping("/api/v1/ai/admin/knowledge-source")
@PostMapping("/search")
public RESTResult<PageResultVO<AiKnowledgeSource>> search(
@PostMapping("/get")
public RESTResult<AiKnowledgeSource> get(@RequestParam Long id, HttpServletRequest request) {
@PostMapping("/save")
public RESTResult<Long> save(@Valid @RequestBody KnowledgeSourceSaveVO vo, HttpServletRequest request) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestParam Long id, HttpServletRequest request) {
```

### MediaController
```
@RequestMapping("/api/v1/ai/media")
@PostMapping("/image/text2img")
public RESTResult<ImageGenerationService.ImageResult> textToImage(
@PostMapping("/image/img2img")
public RESTResult<ImageGenerationService.ImageResult> imageToImage(
@PostMapping("/image/edit")
public RESTResult<ImageGenerationService.ImageResult> editImage(
@PostMapping("/image/history")
public RESTResult<List<ImageGenerationService.ImageGenerationHistory>> getImageHistory(
@PostMapping("/tts/generate")
public RESTResult<TtsService.AudioResult> textToSpeech(
@PostMapping("/tts/voices")
public RESTResult<List<TtsService.VoiceInfo>> getVoices(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/tts/history")
public RESTResult<List<TtsService.TtsHistory>> getTtsHistory(
@PostMapping("/video/trim")
public RESTResult<VideoEditService.VideoResult> trimVideo(
@PostMapping("/video/merge")
public RESTResult<VideoEditService.VideoResult> mergeVideos(
@PostMapping("/video/subtitle")
public RESTResult<VideoEditService.VideoResult> addSubtitles(
@PostMapping("/video/music")
public RESTResult<VideoEditService.VideoResult> addBackgroundMusic(
@PostMapping("/video/transcode")
public RESTResult<VideoEditService.VideoResult> transcodeVideo(
@PostMapping("/video/generate-from-frames")
public RESTResult<VideoEditService.VideoResult> generateFromFrames(
@PostMapping("/video/auto-compose")
public RESTResult<VideoEditService.VideoResult> autoCompose(
```

### ModelBenchmarkController
```
@RequestMapping("/api/v1/ai/model-benchmark")
@PostMapping("/comparison")
public RESTResult<List<Map<String, Object>>> getComparison(
@PostMapping("/best-model")
public RESTResult<Map<String, Object>> getBestModel(
@PostMapping("/record")
public RESTResult<Void> recordBenchmark(
```

### PromptTemplateController
```
@RequestMapping("/api/v1/ai/prompt-template")
@PostMapping("/list")
public RESTResult<PageResultVO<AiPromptTemplate>> list(
@PostMapping("/get")
public RESTResult<AiPromptTemplate> get(
@PostMapping("/save")
public RESTResult<AiPromptTemplate> save(
@PostMapping("/delete")
public RESTResult<?> delete(
@PostMapping("/get-active")
public RESTResult<AiPromptTemplate> getActive(
@PostMapping("/test-render")
public RESTResult<Map<String, String>> testRender(
```

### TaskModelConfigController
```
@RequestMapping("/api/v1/ai/admin/task-model-config")
@PostMapping("/list")
public RESTResult<List<AiTaskModelConfig>> list(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/get")
public RESTResult<AiTaskModelConfig> get(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/save")
public RESTResult<Long> save(@RequestBody AiTaskModelConfig config, HttpServletRequest request) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
```

## Entity 字段

### AiAgentWorkflowContext
```
@Id
private Long id;
@Column(name = "session_id")
private Long sessionId;
@Column(name = "workflow_id", length = 64, nullable = false)
private String workflowId;
@Column(name = "node_role", length = 50, nullable = false)
private String nodeRole;
@Column(name = "node_status", length = 20, nullable = false)
private String nodeStatus;
@Column(name = "input_json", columnDefinition = "TEXT")
private String inputJson;
@Column(name = "output_json", columnDefinition = "TEXT")
private String outputJson;
@Column(name = "error_message", length = 500)
private String errorMessage;
@Column(name = "retry_count")
private Integer retryCount = 0;
@Column(name = "started_at")
private Timestamp startedAt;
@Column(name = "completed_at")
private Timestamp completedAt;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### AiCallLog
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "call_type", nullable = false, length = 64)
private String callType;
@Column(name = "template_code", length = 128)
private String templateCode;
@Column(name = "model_code", length = 64)
private String modelCode;
@Column(name = "input_summary", length = 512)
private String inputSummary;
@Column(name = "output_length")
private Integer outputLength;
@Column(name = "prompt_tokens")
private Integer promptTokens;
@Column(name = "completion_tokens")
private Integer completionTokens;
@Column(name = "total_tokens")
private Integer totalTokens;
@Column(name = "duration_ms")
private Long durationMs;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "error_message", length = 512)
private String errorMessage;
@Column(name = "is_fallback", nullable = false)
private Integer isFallback = 0;
@Column(name = "referenced_chunk_ids", columnDefinition = "TEXT")
private String referencedChunkIds;
@Column(name = "linked_video_id")
private Long linkedVideoId;
@Column(name = "linked_session_id")
private Long linkedSessionId;
@Column(name = "content_effect", length = 32)
private String contentEffect;
@Column(name = "effect_score", precision = 8, scale = 2)
private java.math.BigDecimal effectScore;
@Column(name = "stage_timings", columnDefinition = "TEXT")
private String stageTimings;
@Column(name = "create_time")
private Timestamp createTime;
```

### AiCallQuota
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "quota_date", nullable = false)
private Date quotaDate;
@Column(name = "used_count", nullable = false)
private Integer usedCount = 0;
@Column(name = "used_units", precision = 10, scale = 2)
private BigDecimal usedUnits = BigDecimal.ZERO;
@Column(name = "max_count", nullable = false)
private Integer maxCount = 10;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiCompetitorInsight
```
@Id
private Long id;
@Column(nullable = false, length = 50)
private String source;
@Column(length = 50)
private String category;
@Column(name = "competitor_name", length = 100)
private String competitorName;
@Column(name = "insight_type", length = 50)
private String insightType;
@Column(columnDefinition = "TEXT")
private String content;
@Column(name = "collected_at")
private Timestamp collectedAt;
@Column(name = "quality_score")
private Double qualityScore;
@Column(name = "ingested_to_kb")
private Boolean ingestedToKb = false;
@Column
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiEvolutionReviewTask
```
@Id
private Long id;
@Column(name = "evolve_task_id")
private Long evolveTaskId;
@Column(name = "content_preview", length = 500)
private String contentPreview;
@Column(name = "quality_score")
private Integer qualityScore;
@Column(name = "reviewer_id")
private Long reviewerId;
@Column(name = "review_status", nullable = false, length = 20)
private String reviewStatus = "PENDING";
@Column(name = "review_comment", columnDefinition = "TEXT")
private String reviewComment;
@Column(name = "revised_content", columnDefinition = "TEXT")
private String revisedContent;
@Column(name = "reviewed_at")
private Timestamp reviewedAt;
@Column(name = "auto_expired")
private Boolean autoExpired = false;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiEvolvePendingDeepen
```
@Id
private Long id;
@Column(name = "report_id", nullable = false)
private Long reportId;
@Column(name = "task_id", nullable = false)
private Long taskId;
@Column(name = "kb_id")
private Long kbId;
@Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
private String questionText;
@Column(name = "priority_level", nullable = false)
private Integer priorityLevel = 1;
@Column(name = "status", nullable = false, length = 16)
private String status = "pending";
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### AiEvolveReport
```
@Id
private Long id;
@Column(name = "task_id", nullable = false)
private Long taskId;
@Column(name = "report_title", length = 256)
private String reportTitle;
@Column(name = "methodology_section", columnDefinition = "TEXT")
private String methodologySection;
@Column(name = "deepen_section", columnDefinition = "TEXT")
private String deepenSection;
@Column(name = "iterate_section", columnDefinition = "TEXT")
private String iterateSection;
@Column(name = "full_content", nullable = false, columnDefinition = "TEXT")
private String fullContent;
@Column(name = "methodology_count")
private Integer methodologyCount = 0;
@Column(name = "deepen_count")
private Integer deepenCount = 0;
@Column(name = "has_failure_case", nullable = false)
private Integer hasFailureCase = 0;
@Column(name = "has_sop", nullable = false)
private Integer hasSop = 0;
@Column(name = "has_benchmark", nullable = false)
private Integer hasBenchmark = 0;
@Column(name = "index_status", nullable = false, length = 16)
private String indexStatus = "pending";
@Column(name = "indexed_time")
private Timestamp indexedTime;
@Column(name = "create_time")
private Timestamp createTime;
```

### AiEvolveTask
```
@Id
private Long id;
@Column(name = "kb_id")
private Long kbId;
@Column(name = "task_no", nullable = false, unique = true, length = 64)
private String taskNo;
@Column(name = "topic_ids", length = 512)
private String topicIds;
@Column(name = "topic_texts", columnDefinition = "TEXT")
private String topicTexts;
@Column(name = "evolve_angle", length = 32)
private String evolveAngle;
@Column(name = "gather_mode", length = 16)
private String gatherMode = "hybrid";
@Column(name = "context_length")
private Integer contextLength;
@Column(name = "model_used", length = 64)
private String modelUsed;
@Column(name = "fallback_tier")
private Integer fallbackTier = 1;
@Column(name = "prompt_tokens")
private Integer promptTokens;
@Column(name = "completion_tokens")
private Integer completionTokens;
@Column(name = "total_tokens")
private Integer totalTokens;
@Column(name = "duration_ms")
private Long durationMs;
@Column(name = "score_total")
private Integer scoreTotal;
@Column(name = "score_detail", columnDefinition = "TEXT")
private String scoreDetail;
@Column(name = "expanded_count")
private Integer expandedCount = 0;
@Column(name = "deepened_count")
private Integer deepenedCount = 0;
@Column(name = "had_quality_hint", nullable = false)
private Integer hadQualityHint = 0;
@Column(name = "status", nullable = false, length = 16)
private String status = "pending";
@Column(name = "error_message", length = 512)
private String errorMessage;
@Column(name = "depends_on_task_nos", length = 1024)
private String dependsOnTaskNos;
@Column(name = "blocked_reason", length = 512)
private String blockedReason;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiEvolveTopic
```
@Id
private Long id;
@Column(name = "kb_id")
private Long kbId;
@Column(name = "account_id")
private Long accountId;
@Column(name = "topic", nullable = false, length = 256)
private String topic;
@Column(name = "category", length = 32)
private String category = "basic";
@Column(name = "priority", nullable = false)
private Integer priority = 100;
@Column(name = "source", nullable = false, length = 16)
private String source = "initial";
@Column(name = "used_count")
private Integer usedCount = 0;
@Column(name = "last_used_time")
private Timestamp lastUsedTime;
@Column(name = "score_avg", precision = 5, scale = 2)
private BigDecimal scoreAvg;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiGenerationTask
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "task_type", nullable = false, length = 32)
private String taskType;
@Column(name = "input_content", columnDefinition = "TEXT")
private String inputContent;
@Column(name = "prompt", columnDefinition = "TEXT")
private String prompt;
@Column(name = "model_used", length = 64)
private String modelUsed;
@Column(name = "output_content", columnDefinition = "TEXT")
private String outputContent;
@Column(name = "tokens_used", nullable = false)
private Long tokensUsed = 0L;
@Column(name = "task_status", nullable = false)
private Integer taskStatus = 0;
@Column(name = "error_msg", columnDefinition = "TEXT")
private String errorMsg;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiGraphEdge
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId = 0L;
@Column(name = "source_node_id", nullable = false)
private Long sourceNodeId;
@Column(name = "target_node_id", nullable = false)
private Long targetNodeId;
@Column(name = "relation_type", nullable = false, length = 64)
private String relationType;
@Column(name = "weight")
private Double weight = 1.0;
@Column(name = "properties", columnDefinition = "jsonb")
private Map<String, Object> properties;
@Column(name = "source_doc_id")
private Long sourceDocId;
@Column(name = "confidence")
private Double confidence = 0.8;
@Column(name = "create_time")
private LocalDateTime createTime;
@Column(name = "deleted")
private Integer deleted = 0;
```

### AiGraphNode
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId = 0L;
@Column(name = "entity_type", nullable = false, length = 32)
private String entityType;
@Column(name = "entity_name", nullable = false, length = 200)
private String entityName;
@Column(name = "properties", columnDefinition = "jsonb")
private Map<String, Object> properties;
@Column(name = "source_doc_id")
private Long sourceDocId;
@Column(name = "confidence")
private Double confidence = 0.8;
@Column(name = "create_time")
private LocalDateTime createTime;
@Column(name = "update_time")
private LocalDateTime updateTime;
@Column(name = "deleted")
private Integer deleted = 0;
```

### AiGraphRelationSuggestion
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId = 0L;
@Column(name = "source_entity_key", nullable = false, length = 512)
private String sourceEntityKey;
@Column(name = "target_entity_key", nullable = false, length = 512)
private String targetEntityKey;
@Column(name = "relation_type", nullable = false, length = 64)
private String relationType;
@Column(name = "confidence", nullable = false)
private Double confidence = 0.0;
@Column(name = "status", nullable = false, length = 32)
private String status = "pending";
@Column(name = "evidence_json", columnDefinition = "TEXT")
private String evidenceJson;
@Column(name = "create_time", nullable = false)
private Timestamp createTime;
@Column(name = "update_time", nullable = false)
private Timestamp updateTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### AiHostPersona
```
@Id
private Long id;
@Column(name = "host_code", nullable = false, length = 32)
private String hostCode;
@Column(name = "host_name", nullable = false, length = 64)
private String hostName;
@Column(name = "age")
private Integer age;
@Column(name = "orientation", nullable = false, length = 16)
private String orientation = "C";
@Column(name = "positioning", length = 256)
private String positioning;
@Column(name = "content_matrix", columnDefinition = "TEXT")
private String contentMatrix;
@Column(name = "ai_priorities", columnDefinition = "TEXT")
private String aiPriorities;
@Column(name = "style_vector", columnDefinition = "TEXT")
private String styleVector;
@Column(name = "bayes_factors", columnDefinition = "TEXT")
private String bayesFactors;
@Column(name = "flow_phase")
private Integer flowPhase = 0;
@Column(name = "target_category", length = 64)
private String targetCategory;
@Column(name = "target_gmv_tier", length = 32)
private String targetGmvTier;
@Column(name = "strategy_phase")
private Integer strategyPhase;
@Column(name = "sort_order")
private Integer sortOrder = 0;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiImageGeneration
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "prompt", columnDefinition = "TEXT")
private String prompt;
@Column(name = "negative_prompt", columnDefinition = "TEXT")
private String negativePrompt;
@Column(name = "image_url", columnDefinition = "TEXT")
private String imageUrl;
@Column(name = "generation_type", length = 32)
private String generationType;
@Column(name = "parameters", columnDefinition = "TEXT")
private String parameters;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiIndexQueue
```
@Id
private Long id;
@Column(name = "source_type", nullable = false, length = 32)
@Column(name = "source_id", nullable = false)
private Long sourceId;
@Column(name = "target_kb_id")
private Long targetKbId;
@Column(name = "content", nullable = false, columnDefinition = "TEXT")
private String content;
@Column(name = "priority", nullable = false)
@Column(name = "status", nullable = false, length = 16)
@Column(name = "retry_count", nullable = false)
private Integer retryCount = 0;
@Column(name = "error_msg", length = 512)
private String errorMsg;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiInferenceAudit
```
@Id
private Long id;
@Column(name = "trace_id", length = 64)
private String traceId;
@Column(name = "provider", length = 32)
private String provider;
@Column(name = "capability", length = 32)
private String capability;
@Column(name = "model_ref", length = 256)
private String modelRef;
@Column(name = "input_chars")
private Integer inputChars;
@Column(name = "ok")
private Integer ok;
@Column(name = "latency_ms")
private Integer latencyMs;
@Column(name = "error_message", length = 512)
private String errorMessage;
@Column(name = "meta_json", columnDefinition = "TEXT")
private String metaJson;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### AiKbDocument
```
@Id
private Long id;
@Column(name = "kb_id", nullable = false)
private Long kbId;
@Column(name = "title", nullable = false, length = 256)
private String title;
@Column(name = "content", columnDefinition = "TEXT")
private String content;
@Column(name = "file_type", length = 32)
private String fileType;
@Column(name = "file_size")
private Long fileSize;
@Column(name = "chunk_count", nullable = false)
private Integer chunkCount = 0;
@Column(name = "token_count", nullable = false)
private Integer tokenCount = 0;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "boost_factor", precision = 5, scale = 2)
private java.math.BigDecimal boostFactor;
@Column(name = "source_type", length = 32)
private String sourceType;
@Column(name = "expiry_status")
private Integer expiryStatus;
@Column(name = "last_expiry_check")
private Timestamp lastExpiryCheck;
@Column(name = "sync_retry_count")
private Integer syncRetryCount = 0;
@Column(name = "last_sync_retry_at")
private Timestamp lastSyncRetryAt;
@Column(name = "content_fingerprint", length = 64)
private String contentFingerprint;
@Column(name = "simhash")
private Long simhash;
@Column(name = "retrieval_count")
private Long retrievalCount = 0L;
@Column(name = "citation_count")
private Long citationCount = 0L;
@Column(name = "last_retrieval_at")
private java.sql.Timestamp lastRetrievalAt;
@Column(name = "quality_tier")
private Integer qualityTier = 0;
@Column(name = "quality_heuristic_score")
private Integer qualityHeuristicScore;
@Column(name = "last_quality_eval_at")
private java.sql.Timestamp lastQualityEvalAt;
@Column(name = "metadata", columnDefinition = "jsonb")
private String metadata;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiKnowledgeBase
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "kb_name", nullable = false, length = 128)
private String kbName;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "total_documents", nullable = false)
private Integer totalDocuments = 0;
@Column(name = "total_tokens", nullable = false)
private Long totalTokens = 0L;
@Column(name = "embedding_model", length = 64)
private String embeddingModel;
@Column(name = "kb_type", length = 32)
private String kbType;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiKnowledgeSource
```
@Id
private Long id;
@Column(name = "source_name", nullable = false, length = 128)
private String sourceName;
@Column(name = "source_path", nullable = false, length = 512)
private String sourcePath;
@Column(name = "source_type", nullable = false, length = 32)
private String sourceType = "local";
@Column(name = "file_count", nullable = false)
private Integer fileCount = 0;
@Column(name = "index_count", nullable = false)
private Integer indexCount = 0;
@Column(name = "last_index_time")
private Timestamp lastIndexTime;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiLiveReview
```
@Id
private Long id;
@Column(name = "session_id", nullable = false)
private Long sessionId;
@Column(name = "account_id")
private Long accountId;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "total_viewers", nullable = false)
private Long totalViewers = 0L;
@Column(name = "total_gmv", nullable = false, precision = 12, scale = 2)
private BigDecimal totalGmv = BigDecimal.ZERO;
@Column(name = "conversion_rate", nullable = false, precision = 5, scale = 4)
private BigDecimal conversionRate = BigDecimal.ZERO;
@Column(name = "peak_viewers", nullable = false)
private Long peakViewers = 0L;
@Column(name = "top_scripts", columnDefinition = "TEXT")
private String topScripts;
@Column(name = "weak_points", columnDefinition = "TEXT")
private String weakPoints;
@Column(name = "report_content", columnDefinition = "TEXT")
private String reportContent;
@Column(name = "model_used", length = 64)
private String modelUsed;
@Column(name = "tokens_used", nullable = false)
private Long tokensUsed = 0L;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiModel
```
@Id
private Long id;
@Column(name = "model_name", nullable = false, length = 64)
private String modelName;
@Column(name = "model_provider", nullable = false, length = 32)
private String modelProvider;
@Column(name = "model_version", nullable = false, length = 128)
private String modelVersion;
@Column(name = "api_key", length = 512)
private String apiKey;
@Column(name = "max_tokens", nullable = false)
private Integer maxTokens = 2048;
@Column(name = "temperature", nullable = false, precision = 4, scale = 2)
private BigDecimal temperature = new BigDecimal("0.70");
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "cost_per_1k_tokens", nullable = false, precision = 10, scale = 6)
private BigDecimal costPer1kTokens = BigDecimal.ZERO;
@Column(name = "quota_limit")
private Long quotaLimit = 0L;
@Column(name = "quota_used", nullable = false)
private Long quotaUsed = 0L;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiModelBenchmark
```
@Id
private Long id;
@Column(name = "model_id", nullable = false)
private Long modelId;
@Column(name = "task_code", nullable = false, length = 64)
private String taskCode;
@Column(name = "latency_ms", nullable = false)
private Long latencyMs;
@Column(name = "tokens_used")
private Integer tokensUsed;
@Column(name = "success", nullable = false)
private Boolean success;
@Column(name = "create_time")
private Timestamp createTime;
```

### AiModelPricing
```
@Id
private Long id;
@Column(name = "model_name", nullable = false, length = 100, unique = true)
private String modelName;
@Column(name = "provider", length = 50)
private String provider;
@Column(name = "input_price_per_1k", nullable = false, precision = 10, scale = 6)
private BigDecimal inputPricePer1k = BigDecimal.ZERO;
@Column(name = "output_price_per_1k", nullable = false, precision = 10, scale = 6)
private BigDecimal outputPricePer1k = BigDecimal.ZERO;
@Column(name = "currency", length = 10, nullable = false)
private String currency = "CNY";
@Column(name = "effective_from")
private Timestamp effectiveFrom;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### AiModelRoutingLog
```
@Id
private Long id;
@Column(name = "content_type", length = 30)
private String contentType;
@Column(name = "selected_model", length = 100)
private String selectedModel;
@Column(name = "latency_ms")
private Integer latencyMs;
@Column(name = "quality_score")
private Integer qualityScore;
@Column(name = "success")
private Boolean success;
@Column(name = "cost_tokens")
private Integer costTokens;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### AiPromptOptimizationLog
```
@Id
private Long id;
@Column(name = "task_type", nullable = false, length = 50)
private String taskType;
@Column(name = "original_prompt_hash", length = 64)
private String originalPromptHash;
@Column(name = "optimized_prompt_hash", length = 64)
private String optimizedPromptHash;
@Column(name = "improvement_pct", precision = 5, scale = 2)
private BigDecimal improvementPct;
@Column(name = "trigger_reason", length = 100)
private String triggerReason;
@Column(name = "details_json", columnDefinition = "TEXT")
private String detailsJson;
@Column(name = "user_id")
private Long userId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiPromptTemplate
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "template_code", length = 128)
private String templateCode;
@Column(name = "variant_name", length = 64)
private String variantName = "default";
@Column(name = "version", length = 32)
private String version = "1.0";
@Column(name = "is_active", nullable = false)
private Integer isActive = 1;
@Column(name = "is_default", nullable = false)
private Integer isDefault = 0;
@Column(name = "system_prompt", columnDefinition = "TEXT")
private String systemPrompt;
@Column(name = "user_prompt_tpl", columnDefinition = "TEXT")
private String userPromptTpl;
@Column(name = "model_hint", length = 64)
private String modelHint;
@Column(name = "temperature")
private Float temperature;
@Column(name = "max_tokens")
private Integer maxTokens;
@Column(name = "usage_count", nullable = false)
private Integer usageCount = 0;
@Column(name = "avg_score")
private Float avgScore;
@Column(name = "p50_score")
private Float p50Score;
@Column(name = "p90_score")
private Float p90Score;
@Column(name = "template_name", nullable = false, length = 128)
private String templateName;
@Column(name = "template_content", nullable = false, columnDefinition = "TEXT")
private String templateContent;
@Column(name = "category", length = 32)
private String category;
@Column(name = "variables", columnDefinition = "TEXT")
private String variables;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiQueryLog
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "kb_id", nullable = false)
private Long kbId;
@Column(name = "query_text", nullable = false, length = 512)
private String queryText;
@Column(name = "top_k")
private Integer topK = 10;
@Column(name = "hit_count")
private Integer hitCount = 0;
@Column(name = "latency_ms")
private Integer latencyMs;
@Column(name = "cache_hit")
private Integer cacheHit = 0;
@Column(name = "source", length = 32)
private String source = "user";
@Column(name = "create_time")
private Timestamp createTime;
```

### AiSearchLog
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
private String queryText;
@Column(name = "query_rewritten", columnDefinition = "TEXT")
private String queryRewritten;
@Column(name = "kb_id")
private Long kbId;
@Column(name = "hit_doc_ids", columnDefinition = "TEXT")
private String hitDocIds;
@Column(name = "hit_count")
private Integer hitCount = 0;
@Column(name = "top1_score")
private Double top1Score;
@Column(name = "search_type", length = 32)
private String searchType;
@Column(name = "latency_ms")
private Integer latencyMs;
@Column(name = "user_feedback", length = 16)
private String userFeedback;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### AiTaskModelConfig
```
@Id
private Long id;
@Column(name = "task_code", nullable = false, length = 64)
private String taskCode;
@Column(name = "task_name", nullable = false, length = 128)
private String taskName;
@Column(name = "task_group", nullable = false, length = 32)
private String taskGroup = "evolve";
@Column(name = "primary_model_id")
private Long primaryModelId;
@Column(name = "fallback_model_id")
private Long fallbackModelId;
@Column(name = "fallback2_model_id")
private Long fallback2ModelId;
@Column(name = "timeout_seconds")
private Integer timeoutSeconds;
@Column(name = "max_retries", nullable = false)
private Integer maxRetries = 1;
@Column(name = "sort_order", nullable = false)
private Integer sortOrder = 0;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiTtsGeneration
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "text", columnDefinition = "TEXT")
private String text;
@Column(name = "voice", length = 64)
private String voice;
@Column(name = "language", length = 16)
private String language;
@Column(name = "audio_url", length = 512)
private String audioUrl;
@Column(name = "duration")
private Long duration;
@Column(name = "file_size")
private Long fileSize;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AiUserCognitiveProfile
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false, unique = true)
private Long ownerId;
@Column(name = "preferred_script_types", columnDefinition = "jsonb")
private Map<String, Double> preferredScriptTypes;
@Column(name = "preferred_styles", columnDefinition = "jsonb")
private Map<String, Double> preferredStyles;
@Column(name = "preferred_emotion_curves", columnDefinition = "jsonb")
private Map<String, Double> preferredEmotionCurves;
@Column(name = "avg_edit_ratio")
private Double avgEditRatio = 0.5;
@Column(name = "generation_frequency")
private Integer generationFrequency = 0;
@Column(name = "preferred_length", length = 16)
private String preferredLength = "medium";
@Column(name = "optimization_focus", length = 32)
private String optimizationFocus = "balanced";
@Column(name = "risk_tolerance")
private Double riskTolerance = 0.5;
@Column(name = "profile_version")
private Integer profileVersion = 1;
@Column(name = "last_updated")
private LocalDateTime lastUpdated;
@Column(name = "create_time")
private LocalDateTime createTime;
@Column(name = "update_time")
private LocalDateTime updateTime;
@Column(name = "deleted")
private Integer deleted = 0;
```

### AiViralAnalysis
```
@Id
private Long id;
@Column(name = "video_id", nullable = false)
private Long videoId;
@Column(name = "account_id")
private Long accountId;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "viral_score", nullable = false)
private Integer viralScore = 0;
@Column(name = "view_count", nullable = false)
private Long viewCount = 0L;
@Column(name = "avg_view_count", nullable = false)
private Long avgViewCount = 0L;
@Column(name = "success_factors", columnDefinition = "TEXT")
private String successFactors;
@Column(name = "replicable_methods", columnDefinition = "TEXT")
private String replicableMethods;
@Column(name = "report_content", columnDefinition = "TEXT")
private String reportContent;
@Column(name = "quality_score", nullable = false)
private Integer qualityScore = 0;
@Column(name = "model_used", length = 64)
private String modelUsed;
@Column(name = "tokens_used", nullable = false)
private Long tokensUsed = 0L;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### EvolutionRule
```
@Id
private Long id;
@Column(name = "rule_type", nullable = false, unique = true, length = 50)
private String ruleType;
@Column(name = "rule_name", nullable = false, length = 100)
private String ruleName;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "is_enabled")
private Integer isEnabled = 1;
@Column(name = "config", columnDefinition = "JSONB")
private String config;
@Column(name = "created_at")
private Timestamp createdAt;
@Column(name = "updated_at")
private Timestamp updatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### KbFeedback
```
@Id
private Long id;
@Column(name = "query", nullable = false, length = 512)
private String query;
@Column(name = "doc_id", nullable = false, length = 128)
private String docId;
@Column(name = "rating", nullable = false)
private Integer rating;
@Column(name = "comment", length = 512)
private String comment;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "search_mode", length = 16)
private String searchMode;
@Column(name = "create_time")
private Timestamp createTime;
```

### KbImportCheckpoint
```
@Id
private Long id;
@Column(name = "kb_id", nullable = false)
private Long kbId;
@Column(name = "source_path", nullable = false, length = 500)
private String sourcePath;
private Timestamp lastImportTime;
@Column(name = "file_count")
private Integer fileCount = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### KbImportReport
```
@Id
private Long id;
@Column(name = "kb_id", nullable = false)
private Long kbId;
@Column(name = "source_path", length = 500)
private String sourcePath;
@Column(name = "total_files")
private Integer totalFiles = 0;
@Column(name = "success_count")
private Integer successCount = 0;
@Column(name = "failed_count")
private Integer failedCount = 0;
@Column(name = "skipped_count")
private Integer skippedCount = 0;
@Column(name = "dedup_skipped")
private Integer dedupSkipped = 0;
@Column(name = "dedup_downweighted")
private Integer dedupDownweighted = 0;
@Column(name = "new_chunks")
private Integer newChunks = 0;
@Column(name = "content_type", length = 16)
private String contentType;
@Column(name = "errors", columnDefinition = "jsonb")
private String errors;
@Column(name = "duration_ms")
private Long durationMs;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### KnowledgeDeduplicationGroup
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "master_script_id", nullable = false)
private Long masterScriptId;
@Column(name = "duplicate_script_id", nullable = false)
private Long duplicateScriptId;
@Column(name = "similarity_score", nullable = false, precision = 3, scale = 2)
private BigDecimal similarityScore;
@Column(name = "merge_status", length = 50)
private String mergeStatus = "DETECTED";
@Column(name = "merge_reason", length = 255)
private String mergeReason;
@Column(name = "merged_at")
private Timestamp mergedAt;
@Column(name = "variant_type", length = 50)
private String variantType = "VARIANT";
@Column(name = "is_active")
private Integer isActive = 1;
@Column(name = "notes", columnDefinition = "TEXT")
private String notes;
@Column(name = "created_at")
private Timestamp createdAt;
@Column(name = "updated_at")
private Timestamp updatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### KnowledgeEvolutionExecution
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "execution_period", length = 50)
private String executionPeriod;
@Column(name = "execution_date", nullable = false)
private Date executionDate;
@Column(name = "rule_type", length = 50)
private String ruleType;
@Column(name = "included_count")
private Integer includedCount = 0;
@Column(name = "updated_count")
private Integer updatedCount = 0;
@Column(name = "merged_count")
private Integer mergedCount = 0;
@Column(name = "archived_count")
private Integer archivedCount = 0;
@Column(name = "quality_improvement", precision = 5, scale = 2)
private BigDecimal qualityImprovement;
@Column(name = "execution_status", length = 50)
private String executionStatus = "COMPLETED";
@Column(name = "error_message", columnDefinition = "TEXT")
private String errorMessage;
@Column(name = "executed_by", length = 255)
private String executedBy = "system";
@Column(name = "notes", columnDefinition = "TEXT")
private String notes;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### KnowledgeEvolutionLog
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "script_version_id", nullable = false)
private Long scriptVersionId;
@Column(name = "action", nullable = false, length = 50)
private String action;
@Column(name = "rule_type", length = 50)
private String ruleType;
@Column(name = "old_value", columnDefinition = "TEXT")
private String oldValue;
@Column(name = "new_value", columnDefinition = "TEXT")
private String newValue;
@Column(name = "reason", columnDefinition = "TEXT")
private String reason;
@Column(name = "executed_by", length = 255)
private String executedBy = "system";
@Column(name = "status", length = 50)
private String status = "COMPLETED";
@Column(name = "error_message", columnDefinition = "TEXT")
private String errorMessage;
@Column(name = "created_at")
private Timestamp createdAt;
@Column(name = "updated_at")
private Timestamp updatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### KnowledgeEvolutionReport
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "report_type", nullable = false, length = 50)
private String reportType;
@Column(name = "period_start", nullable = false)
private Date periodStart;
@Column(name = "period_end", nullable = false)
private Date periodEnd;
@Column(name = "total_in_library")
private Integer totalInLibrary = 0;
@Column(name = "new_added_count")
private Integer newAddedCount = 0;
@Column(name = "archived_count")
private Integer archivedCount = 0;
@Column(name = "deduplication_count")
private Integer deduplicationCount = 0;
@Column(name = "average_quality_score", precision = 5, scale = 2)
private BigDecimal averageQualityScore;
@Column(name = "trend", length = 20)
private String trend;
@Column(name = "report_content", columnDefinition = "JSONB")
private Map<String, Object> reportContent;
@Column(name = "generated_by", length = 255)
private String generatedBy = "system";
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### KnowledgeQualityScore
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "script_version_id", nullable = false)
private Long scriptVersionId;
@Column(name = "period_start", nullable = false)
private Date periodStart;
@Column(name = "period_end", nullable = false)
private Date periodEnd;
@Column(name = "quality_score", nullable = false, precision = 5, scale = 2)
private BigDecimal qualityScore;
@Column(name = "effectiveness_score", precision = 5, scale = 2)
private BigDecimal effectivenessScore;
@Column(name = "usage_count")
private Integer usageCount = 0;
@Column(name = "adoption_rate", precision = 5, scale = 2)
private BigDecimal adoptionRate;
@Column(name = "engagement_rate", precision = 5, scale = 2)
private BigDecimal engagementRate;
@Column(name = "conversion_rate", precision = 5, scale = 2)
private BigDecimal conversionRate;
@Column(name = "avg_sentiment_score", precision = 5, scale = 2)
private BigDecimal avgSentimentScore;
@Column(name = "consecutive_low_scores")
private Integer consecutiveLowScores = 0;
@Column(name = "trend", length = 20)
private String trend;
@Column(name = "notes", columnDefinition = "TEXT")
private String notes;
@Column(name = "created_at")
private Timestamp createdAt;
@Column(name = "updated_at")
private Timestamp updatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```
