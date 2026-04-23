# shortvideo 模块 API 文档

## 文件结构
```
config/ShortVideoAutoRemakeContentPolicyProperties.java
config/ShortVideoVerticalCollectorProperties.java
config/SvHotTopicSyncScheduler.java
config/VideoGenerationRabbitListenerConfig.java
config/VideoGenerationTaskAmqpConfig.java
config/VideoGenerationTaskAmqpConsumer.java
config/VideoGenerationTaskTimeoutScheduler.java
config/ViralAutoOrchestrationScheduler.java
config/ViralDeepAnalyzeTimeoutScheduler.java
config/ViralVideoCollectorScheduler.java
config/WorkflowTaskExecutorConfig.java
controller/AccountVideoCollectController.java
controller/AiMusicController.java
controller/CinematicOpsController.java
controller/CompetitorMonitorController.java
controller/ContrastVideoTemplateController.java
controller/CrossModuleContentController.java
controller/DailyContentController.java
controller/DigitalHumanWebhookController.java
controller/DramaController.java
controller/PersonaViralFusionController.java
controller/QualityDashboardController.java
controller/RemakeTemplateController.java
controller/ShortVideoAiController.java
controller/ShortVideoController.java
controller/ShortVideoDashboardController.java
controller/ShortVideoDataController.java
controller/ShortVideoEditController.java
controller/ShortVideoFeedbackController.java
controller/ShortVideoMaterialController.java
controller/ShortVideoMaterialLibraryController.java
controller/ShortVideoProjectController.java
controller/ShortVideoPublishController.java
controller/ShortVideoQuickController.java
controller/ShortVideoScriptController.java
controller/ShortVideoSeoController.java
controller/ShortVideoShootingTaskController.java
controller/ShortVideoShotListController.java
controller/ShortVideoUploadController.java
controller/SvContentCalendarController.java
controller/SvScriptTemplateController.java
controller/VideoGenerationTaskController.java
controller/ViralRemakeController.java
controller/ViralVideoController.java
controller/WorkflowController.java
controller/WorkflowTemplateController.java
entity/SvAccountCollectTask.java
entity/SvAudienceProfileImport.java
entity/SvBgmLibrary.java
entity/SvCategory.java
entity/SvCinematicPreset.java
entity/SvComment.java
entity/SvCompetitor.java
entity/SvCompetitorSnapshot.java
entity/SvContentCalendar.java
entity/SvDailyBatch.java
entity/SvDigitalHumanTask.java
entity/SvDrama.java
entity/SvDramaCharacter.java
entity/SvDramaEpisode.java
entity/SvGenerationLog.java
entity/SvHotTopic.java
entity/SvMaterial.java
entity/SvMaterialTagCatalog.java
entity/SvOpsReportTemplate.java
entity/SvPlan.java
entity/SvPlanAsset.java
entity/SvProject.java
entity/SvPublishTimeAnalysis.java
entity/SvRemakeTemplate.java
entity/SvSceneCameraMapping.java
entity/SvScript.java
entity/SvScriptTemplate.java
entity/SvShootingTask.java
entity/SvShot.java
entity/SvShotList.java
entity/SvStoryFormulaTemplate.java
entity/SvVideo.java
entity/SvVideoData.java
entity/SvVideoGeneration.java
entity/SvVideoGenerationTask.java
entity/SvViralFavorite.java
entity/SvViralRemakeLog.java
entity/SvViralVideo.java
entity/SvWebhookDlq.java
entity/SvWorkflowTask.java
entity/SvWorkflowTemplate.java
ops/OpsReportDef.java
repository/SvAccountCollectTaskRepository.java
repository/SvAudienceProfileImportRepository.java
repository/SvBgmLibraryRepository.java
repository/SvCategoryRepository.java
repository/SvCinematicPresetRepository.java
repository/SvCommentRepository.java
repository/SvCompetitorRepository.java
repository/SvCompetitorSnapshotRepository.java
repository/SvContentCalendarRepository.java
repository/SvDailyBatchRepository.java
repository/SvDigitalHumanTaskRepository.java
repository/SvDramaCharacterRepository.java
repository/SvDramaEpisodeRepository.java
repository/SvDramaRepository.java
repository/SvGenerationLogRepository.java
repository/SvHotTopicRepository.java
repository/SvMaterialRepository.java
repository/SvMaterialTagCatalogRepository.java
repository/SvOpsReportTemplateRepository.java
repository/SvPlanRepository.java
repository/SvProjectRepository.java
repository/SvPublishTimeAnalysisRepository.java
repository/SvRemakeTemplateRepository.java
repository/SvSceneCameraMappingRepository.java
repository/SvScriptRepository.java
repository/SvScriptTemplateRepository.java
repository/SvShootingTaskRepository.java
repository/SvShotListRepository.java
repository/SvShotRepository.java
repository/SvStoryFormulaTemplateRepository.java
repository/SvVideoDataRepository.java
repository/SvVideoGenerationRepository.java
repository/SvVideoGenerationTaskRepository.java
repository/SvVideoRepository.java
repository/SvViralFavoriteRepository.java
repository/SvViralRemakeLogRepository.java
repository/SvViralVideoRepository.java
repository/SvWebhookDlqRepository.java
repository/SvWorkflowTaskRepository.java
repository/SvWorkflowTemplateRepository.java
schedule/DailyScriptGenerationScheduler.java
scheduler/OpsReportScheduleJob.java
service/AccountVideoCollectService.java
service/BgmRecommendService.java
service/CinematicGenerationLogReportService.java
service/CompetitorMonitorService.java
service/ContentAuditService.java
service/ContentCalendarService.java
service/ContentEffectPredictor.java
service/ContrastVideoTemplateService.java
service/CrossModuleContentService.java
service/DailyContentService.java
service/DailyShootService.java
service/DigitalHumanPollingService.java
service/DigitalHumanSynthesisService.java
service/DouyinPublishService.java
service/DouyinSeoService.java
service/DramaService.java
service/HotspotWindowService.java
service/MaterialLibraryService.java
service/MultiPlatformContentService.java
service/OpsReportTemplateService.java
service/PersonaConsistencyChecker.java
service/PersonaViralFusionService.java
service/PublishFeedbackService.java
service/PublishTimeRecommendationService.java
service/QualityDashboardService.java
service/RemakeTemplateService.java
service/SceneCameraMappingAdminService.java
service/ShortVideoAiService.java
service/ShortVideoDashboardService.java
service/ShortVideoMaterialService.java
service/ShortVideoQuickService.java
service/StoryFormulaTemplateService.java
service/SvContentCalendarService.java
service/SvHotTopicSyncService.java
service/SvProjectService.java
service/SvScriptEffectivenessService.java
service/SvScriptService.java
service/SvScriptTemplateService.java
service/SvShootingTaskService.java
service/SvShotListService.java
service/SvVideoService.java
service/VideoGenerationTaskService.java
service/VideoGenerationTaskWebhookNotifier.java
service/ViralRemakeService.java
service/ViralVideoDeepAnalysisService.java
service/ViralVideoService.java
service/WorkflowExecutionService.java
service/WorkflowTemplateService.java
service/client/BaiduContentAuditClient.java
service/impl/AccountCollectAsyncRunner.java
service/impl/AccountVideoCollectServiceImpl.java
service/impl/AccountVideoScraper.java
service/impl/BgmRecommendServiceImpl.java
service/impl/CinematicGenerationLogReportServiceImpl.java
service/impl/CompetitorMonitorServiceImpl.java
service/impl/ContentAuditServiceImpl.java
service/impl/ContentCalendarServiceImpl.java
service/impl/ContentEffectPredictorImpl.java
service/impl/ContrastVideoTemplateServiceImpl.java
service/impl/CrossModuleContentServiceImpl.java
service/impl/DailyContentServiceImpl.java
service/impl/DailyShootServiceImpl.java
service/impl/DigitalHumanPollingServiceImpl.java
service/impl/DigitalHumanSynthesisServiceImpl.java
service/impl/DouyinPublishServiceImpl.java
service/impl/DouyinSeoServiceImpl.java
service/impl/DouyinUrlResolver.java
service/impl/DramaAiAdapter.java
service/impl/DramaSceneManager.java
service/impl/DramaServiceImpl.java
service/impl/MaterialGeneratorHelper.java
service/impl/MaterialLibraryServiceImpl.java
service/impl/MaterialStorageHelper.java
service/impl/MultiPlatformContentServiceImpl.java
service/impl/OpsReportTemplateServiceImpl.java
service/impl/PersonaConsistencyCheckerImpl.java
service/impl/PersonaViralFusionServiceImpl.java
service/impl/PublishFeedbackServiceImpl.java
service/impl/PublishTimeRecommendationServiceImpl.java
service/impl/QualityDashboardServiceImpl.java
service/impl/RemakeTemplateServiceImpl.java
service/impl/SceneCameraMappingAdminServiceImpl.java
service/impl/ShortVideoAiServiceImpl.java
service/impl/ShortVideoDashboardServiceImpl.java
service/impl/ShortVideoMaterialServiceImpl.java
service/impl/ShortVideoQuickServiceImpl.java
service/impl/StoryFormulaTemplateServiceImpl.java
service/impl/SvCategoryServiceImpl.java
service/impl/SvCommentServiceImpl.java
service/impl/SvContentCalendarServiceImpl.java
service/impl/SvHotTopicSyncServiceImpl.java
service/impl/SvProjectServiceImpl.java
service/impl/SvScriptEffectivenessServiceImpl.java
service/impl/SvScriptServiceImpl.java
service/impl/SvScriptTemplateServiceImpl.java
service/impl/SvShootingTaskServiceImpl.java
service/impl/SvShotListServiceImpl.java
service/impl/SvVideoServiceImpl.java
service/impl/VideoBreakdownKbFormatter.java
service/impl/VideoGenerationTaskServiceImpl.java
service/impl/ViralAutoOrchestrationService.java
service/impl/ViralBosUploader.java
service/impl/ViralCommentExtractor.java
service/impl/ViralDeepAnalyzeAsyncRunner.java
service/impl/ViralDeepAnalyzeExecutor.java
service/impl/ViralMetadataExtractor.java
service/impl/ViralMetadataPlaywrightExtractor.java
service/impl/ViralRemakeServiceImpl.java
service/impl/ViralVideoDeepAnalysisServiceImpl.java
service/impl/ViralVideoMetadata.java
service/impl/ViralVideoServiceImpl.java
service/impl/WorkflowTemplateServiceImpl.java
util/AutoComposeSubtitleResolver.java
util/ImageDHashUtil.java
util/MaterialRefreshFailureClassifier.java
util/ProjectTimelineJsonCodec.java
util/ShortVideoPathHelper.java
util/ShotTimelineTrimUtil.java
vo/AccountCollectAnalyzeVO.java
vo/AccountCollectTaskIdVO.java
vo/AccountCollectTaskSaveVO.java
vo/AccountCollectTaskSearchVO.java
vo/AccountCollectTaskVO.java
vo/AccountCollectVideosQueryVO.java
vo/AiCopyGenerateResultVO.java
vo/AiCopyGenerateVO.java
vo/AiScriptGenerateVO.java
vo/AiTitleGenerateVO.java
vo/AiVideoPlanGenerateVO.java
vo/AnalyzeViralRequest.java
vo/AutoComposeRequestVO.java
vo/BgmRecommendVO.java
vo/GenerateTitleRequestVO.java
vo/RemakeTemplateSaveVO.java
vo/RemakeTemplateSearchVO.java
vo/ShortVideoBasicQueryDto.java
vo/SvCategorySaveVO.java
vo/SvCategoryVO.java
vo/SvCommentSaveVO.java
vo/SvCommentSearchVO.java
vo/SvCommentVO.java
vo/SvContentCalendarSaveVO.java
vo/SvContentCalendarSearchVO.java
vo/SvContentCalendarVO.java
vo/SvProjectSaveVO.java
vo/SvProjectSearchVO.java
vo/SvProjectVO.java
vo/SvScriptSaveVO.java
vo/SvScriptSearchVO.java
vo/SvScriptTemplateSaveVO.java
vo/SvScriptTemplateSearchVO.java
vo/SvScriptTemplateVO.java
vo/SvScriptVO.java
vo/SvShootingTaskSaveVO.java
vo/SvShootingTaskSearchVO.java
vo/SvShootingTaskVO.java
vo/SvShotListVO.java
vo/SvShotSaveVO.java
vo/SvShotVO.java
vo/SvVideoSaveVO.java
vo/SvVideoSearchVO.java
vo/SvVideoVO.java
vo/SvWebhookDlqVO.java
vo/VideoIdQueryVO.java
vo/ViralAnalysisResultVO.java
vo/ViralCollectVO.java
vo/ViralListQueryVO.java
```

## API 接口

### AccountVideoCollectController
```
@RequestMapping("/api/v1/short-video/account-collect")
@PostMapping("/start")
public RESTResult<AccountCollectTaskVO> start(
@PostMapping("/list")
public RESTResult<PageResultVO<AccountCollectTaskVO>> list(
@PostMapping("/status")
public RESTResult<AccountCollectTaskVO> status(
@PostMapping("/cancel")
public RESTResult<Void> cancel(
@PostMapping("/retry")
public RESTResult<AccountCollectTaskVO> retry(
@PostMapping("/analyze-selected")
public RESTResult<AccountCollectTaskVO> analyzeSelected(
@PostMapping("/videos")
public RESTResult<PageResultVO<Map<String, Object>>> videos(
@PostMapping("/delete")
public RESTResult<Void> delete(
@PostMapping(value = "/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
```

### AiMusicController
```
@RequestMapping("/api/v1/short-video/music")
@PostMapping("/generate-bgm")
public RESTResult<Map<String, Object>> generateBgm(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/providers")
public RESTResult<List<String>> providers(HttpServletRequest request) {
@PostMapping("/generate-sfx")
public RESTResult<List<Map<String, Object>>> generateSfx(@RequestBody Map<String, Object> body, HttpServletRequest request) {
```

### CinematicOpsController
```
@RequestMapping("/api/v1/short-video/cinematic")
@PostMapping("/scene-camera-mapping/list")
public RESTResult<List<Map<String, Object>>> mappingList(HttpServletRequest request) {
@PostMapping("/scene-camera-mapping/save")
public RESTResult<Long> mappingSave(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/scene-camera-mapping/delete")
public RESTResult<Void> mappingDelete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/generation-log/summary")
public RESTResult<Map<String, Object>> generationLogSummary(HttpServletRequest request,
```

### CompetitorMonitorController
```
@RequestMapping("/api/v1/short-video/competitor")
@PostMapping("/add")
public RESTResult<String> add(@RequestBody Map<String, String> body, HttpServletRequest request) {
@PostMapping("/list")
public RESTResult<List<Map<String, Object>>> list(HttpServletRequest request) {
@PostMapping("/analyze")
public RESTResult<Map<String, Object>> analyze(@RequestBody Map<String, Long> body, HttpServletRequest request) {
@PostMapping("/weekly-report")
public RESTResult<String> weeklyReport(HttpServletRequest request) {
@PostMapping("/remove")
public RESTResult<Void> remove(@RequestBody Map<String, Long> body, HttpServletRequest request) {
```

### ContrastVideoTemplateController
```
@RequestMapping("/api/v1/short-video/contrast-template")
@PostMapping("/shot-template")
public RESTResult<Map<String, Object>> getShotTemplate(@RequestBody Map<String, Object> params) {
@PostMapping("/comedy-config")
public RESTResult<Map<String, Object>> getComedyConfig(@RequestBody Map<String, Object> params) {
@PostMapping("/bgm-strategy")
public RESTResult<Map<String, Object>> getBgmStrategy(@RequestBody Map<String, Object> params) {
@PostMapping("/list")
public RESTResult<List<Map<String, Object>>> listTemplates() {
@PostMapping("/preset-template")
public RESTResult<Map<String, Object>> getPresetTemplate(@RequestBody Map<String, Object> params) {
```

### CrossModuleContentController
```
@RequestMapping("/api/v1/short-video/cross")
@PostMapping("/live-to-video")
public RESTResult<String> liveToVideo(@RequestBody Map<String, Long> body, HttpServletRequest request) {
@PostMapping("/video-preview-for-live")
public RESTResult<Map<String, Object>> videoPreviewForLive(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/hot-topic-pool")
public RESTResult<Map<String, Object>> hotTopicPool(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
```

### DailyContentController
```
@RequestMapping("/api/v1/short-video/daily")
@PostMapping("/generate-batch")
public RESTResult<SvDailyBatch> generateBatch(@CurrentUserId Long userId,
@PostMapping("/batch-status")
public RESTResult<SvDailyBatch> batchStatus(@CurrentUserId Long userId,
@PostMapping("/batch-list")
public RESTResult<PageResultVO<SvDailyBatch>> batchList(@CurrentUserId Long userId,
```

### DigitalHumanWebhookController
```
@RequestMapping("/api/v1/short-video/webhooks")
@PostMapping("/heygen")
public RESTResult<String> heygenWebhook(@RequestBody Map<String, Object> payload) {
@PostMapping("/did")
public RESTResult<String> didWebhook(@RequestBody Map<String, Object> payload) {
```

### DramaController
```
@RequestMapping("/api/v1/short-video/drama")
@PostMapping("/list")
public RESTResult<List<SvDrama>> list(HttpServletRequest request) {
@PostMapping("/get")
public RESTResult<SvDrama> get(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/update")
public RESTResult<SvDrama> update(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/create")
public RESTResult<SvDrama> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/episodes")
public RESTResult<List<SvDramaEpisode>> episodes(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/add-episode")
public RESTResult<SvDramaEpisode> addEpisode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/link-episode")
public RESTResult<Void> linkEpisode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/delete-episode")
public RESTResult<Void> deleteEpisode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/update-episode")
public RESTResult<Void> updateEpisode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/apply-script-to-episodes")
public RESTResult<Integer> applyScriptToEpisodes(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/characters")
public RESTResult<List<SvDramaCharacter>> characters(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/update-character")
public RESTResult<SvDramaCharacter> updateCharacter(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/delete-character")
public RESTResult<Void> deleteCharacter(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/add-character")
public RESTResult<SvDramaCharacter> addCharacter(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/create-project-from-episode")
public RESTResult<Long> createProjectFromEpisode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/by-project")
public RESTResult<Map<String, Object>> getByProject(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/generate-script")
public RESTResult<String> generateScript(@RequestBody Map<String, Object> body, HttpServletRequest request) {
```

### PersonaViralFusionController
```
@RequestMapping("/api/v1/short-video/persona-fusion")
@PostMapping("/match-personas")
public RESTResult<List<Map<String, Object>>> matchPersonas(
@PostMapping("/generate-fused-script")
public RESTResult<Map<String, Object>> generateFusedScript(
@PostMapping("/generate-hotspot-fused")
public RESTResult<Map<String, Object>> generateHotspotFused(
```

### QualityDashboardController
```
@RequestMapping("/api/v1/short-video/quality-dashboard")
@PostMapping("/overview")
public RESTResult<Map<String, Object>> overview(HttpServletRequest request) {
@PostMapping("/trend")
public RESTResult<List<Map<String, Object>>> trend(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/model-ranking")
public RESTResult<List<Map<String, Object>>> modelRanking(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/camera-ranking")
public RESTResult<List<Map<String, Object>>> cameraRanking(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/ai-reflections")
public RESTResult<List<String>> aiReflections(HttpServletRequest request) {
```

### RemakeTemplateController
```
@RequestMapping("/api/v1/short-video/remake-template")
@PostMapping("/list")
public RESTResult<PageResultVO<SvRemakeTemplate>> list(@RequestBody(required = false) RemakeTemplateSearchVO vo, @CurrentUserId Long userId) {
@PostMapping("/save")
public RESTResult<SvRemakeTemplate> save(@RequestBody @Valid RemakeTemplateSaveVO vo, @CurrentUserId Long userId) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestBody Map<String, Long> body, @CurrentUserId Long userId) {
@PostMapping("/create-from-viral")
public RESTResult<SvRemakeTemplate> createFromViral(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
@PostMapping("/generate")
public RESTResult<String> generate(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
```

### ShortVideoAiController
```
@RequestMapping("/api/v1/short-video/ai")
@PostMapping("/check-violation")
@PostMapping("/generate-copy")
@PostMapping("/generate-script")
@PostMapping("/generate-title")
@PostMapping("/generate-plan")
```

### ShortVideoController
```
@RequestMapping("/api/v1/short-video")
@PostMapping("/content/search")
public RESTResult<PageResultVO<SvVideoVO>> search(HttpServletRequest request,
@PostMapping("/content/get")
public RESTResult<SvVideoVO> get(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/content/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody SvVideoSaveVO vo) {
@PostMapping("/content/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/content/increment-view-count")
public RESTResult<Void> incrementViewCount(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/content/data-trend")
public RESTResult<java.util.List<java.util.Map<String, Object>>> dataTrend(HttpServletRequest request,
@PostMapping("/content/calendar")
public RESTResult<java.util.Map<String, Object>> contentCalendar(HttpServletRequest request,
@PostMapping("/content/calendar-stats")
public RESTResult<java.util.Map<String, Object>> contentCalendarStats(HttpServletRequest request,
@PostMapping("/content/publish-time-recommend")
public RESTResult<java.util.List<java.util.Map<String, Object>>> publishTimeRecommend(HttpServletRequest request,
@PostMapping("/category/list")
public RESTResult<List<SvCategoryVO>> categoryList(HttpServletRequest request) {
@PostMapping("/category/get")
public RESTResult<SvCategoryVO> categoryGet(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/category/save")
public RESTResult<Long> categorySave(HttpServletRequest request, @Valid @RequestBody SvCategorySaveVO vo) {
@PostMapping("/category/delete")
public RESTResult<Void> categoryDelete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/comment/search")
public RESTResult<PageResultVO<SvCommentVO>> commentSearch(HttpServletRequest request,
@PostMapping("/comment/get")
public RESTResult<SvCommentVO> commentGet(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/comment/save")
public RESTResult<Long> commentSave(HttpServletRequest request, @Valid @RequestBody SvCommentSaveVO vo) {
@PostMapping("/comment/delete")
public RESTResult<Void> commentDelete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/comment/increment-like-count")
public RESTResult<Void> commentLike(HttpServletRequest request, @RequestParam Long id) {
```

### ShortVideoDashboardController
```
@RequestMapping("/api/v1/short-video/dashboard")
@PostMapping("/stats")
public RESTResult<Map<String, Object>> stats(HttpServletRequest request) {
@PostMapping("/trend")
public RESTResult<List<Map<String, Object>>> trend(
@PostMapping("/projects")
public RESTResult<List<Map<String, Object>>> projects(
@PostMapping("/cost-breakdown")
public RESTResult<Map<String, Object>> costBreakdown(
```

### ShortVideoDataController
```
@RequestMapping("/api/v1/short-video/data")
@PostMapping("/collect-hot-videos")
public RESTResult<Long> collectHotVideos(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/analyze-viral")
public RESTResult<String> analyzeViral(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
```

### ShortVideoEditController
```
@RequestMapping("/api/v1/short-video/edit")
@PostMapping("/auto-compose")
public RESTResult<Map<String, Object>> autoCompose(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/generate-subtitles")
public RESTResult<Map<String, Object>> generateSubtitles(@RequestBody Map<String, Object> body, HttpServletRequest request) {
```

### ShortVideoFeedbackController
```
@RequestMapping("/api/v1/short-video/feedback")
@PostMapping("/analyze-performance")
public RESTResult<Map<String, Object>> analyzePerformance(@RequestBody(required = false) Map<String, Object> body,
@PostMapping("/reflection-report")
public RESTResult<Map<String, Object>> reflectionReport(@RequestBody(required = false) Map<String, Object> body,
@PostMapping("/weekly-report")
public RESTResult<Map<String, Object>> weeklyReport(HttpServletRequest request) {
```

### ShortVideoMaterialController
```
@RequestMapping("/api/v1/short-video/material")
@PostMapping(value = "/generate-keyframes-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/generate-keyframes")
public RESTResult<Map<String, Object>> generateKeyframes(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/generate-voice-batch")
public RESTResult<Map<String, Object>> generateVoiceBatch(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping(value = "/img2video-batch-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/img2video-batch")
public RESTResult<Map<String, Object>> img2videoBatch(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/recommend-camera")
public RESTResult<Map<String, Object>> recommendCamera(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/evaluate-video-quality")
public RESTResult<Map<String, Object>> evaluateVideoQuality(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/retry-keyframe")
public RESTResult<Map<String, Object>> retryKeyframe(@RequestBody Map<String, Object> body, HttpServletRequest request) {
```

### ShortVideoMaterialLibraryController
```
@RequestMapping("/api/v1/short-video/library")
@PostMapping("/list")
public RESTResult<PageResultVO<Map<String, Object>>> list(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
```

### ShortVideoProjectController
```
@RequestMapping("/api/v1/short-video/project")
@PostMapping("/list")
public RESTResult<PageResultVO<SvProjectVO>> list(@RequestBody SvProjectSearchVO vo, HttpServletRequest request) {
@PostMapping("/get")
public RESTResult<SvProjectVO> get(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/save")
public RESTResult<Long> save(@RequestBody @Valid SvProjectSaveVO vo, HttpServletRequest request) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/generate-daily")
public RESTResult<java.util.Map<String, Object>> generateDaily(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/daily-list")
public RESTResult<java.util.List<java.util.Map<String, Object>>> dailyList(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/update-shoot-status")
public RESTResult<Void> updateShootStatus(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/export-script")
public RESTResult<String> exportScript(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
```

### ShortVideoPublishController
```
@RequestMapping("/api/v1/short-video/publish")
@PostMapping("/generate-title")
public RESTResult<Map<String, Object>> generateTitle(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/ai-review")
public RESTResult<Map<String, Object>> aiReview(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/douyin")
public RESTResult<Map<String, Object>> publishDouyin(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/publish")
public RESTResult<Map<String, Object>> publish(@RequestBody Map<String, Object> body, HttpServletRequest request) {
```

### ShortVideoQuickController
```
@RequestMapping("/api/v1/short-video/quick")
@PostMapping("/generate")
public RESTResult<Map<String, Object>> generate(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
```

### ShortVideoScriptController
```
@RequestMapping("/api/v1/short-video/script")
@PostMapping("/list")
public RESTResult<PageResultVO<SvScriptVO>> list(@RequestBody SvScriptSearchVO vo, HttpServletRequest request) {
@PostMapping("/get")
public RESTResult<SvScriptVO> get(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/save")
public RESTResult<Long> save(@RequestBody @Valid SvScriptSaveVO vo, HttpServletRequest request) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/generate")
public RESTResult<String> generate(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/analyze-viral")
public RESTResult<String> analyzeViral(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
```

### ShortVideoSeoController
```
@RequestMapping("/api/v1/short-video/seo")
@PostMapping("/suggest-tags")
public RESTResult<List<String>> suggestTags(@RequestBody(required = false) Map<String, Object> body,
@PostMapping("/suggest-publish-time")
public RESTResult<List<String>> suggestPublishTime(@RequestBody(required = false) Map<String, Object> body,
@PostMapping("/suggest-cover")
public RESTResult<String> suggestCover(@RequestBody(required = false) Map<String, Object> body,
@PostMapping("/suggest-ab-titles")
public RESTResult<List<String>> suggestAbTestTitles(@RequestBody(required = false) Map<String, Object> body,
```

### ShortVideoShootingTaskController
```
@RequestMapping("/api/v1/short-video/shooting-task")
@PostMapping("/list")
public RESTResult<PageResultVO<SvShootingTaskVO>> list(@RequestBody(required = false) SvShootingTaskSearchVO vo,
@PostMapping("/get")
public RESTResult<SvShootingTaskVO> get(@RequestBody(required = false) Map<String, Object> body,
@PostMapping("/save")
public RESTResult<Long> save(@RequestBody @Valid SvShootingTaskSaveVO vo, HttpServletRequest request) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
```

### ShortVideoShotListController
```
@RequestMapping("/api/v1/short-video/shot-list")
@PostMapping("/list")
public RESTResult<PageResultVO<SvShotListVO>> list(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/get")
public RESTResult<SvShotListVO> get(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/get-by-script")
public RESTResult<SvShotListVO> getByScript(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/save")
public RESTResult<Long> save(@RequestBody SvShotSaveVO vo, HttpServletRequest request) {
@PostMapping("/generate")
public RESTResult<Map<String, Object>> generate(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/review")
public RESTResult<Map<String, Object>> review(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
```

### ShortVideoUploadController
```
@RequestMapping("/api/v1/short-video/upload")
@PostMapping("/keyframe")
public RESTResult<Map<String, String>> uploadKeyframe(
@PostMapping("/keyframes/batch")
public RESTResult<Map<String, Object>> batchUploadKeyframes(
@PostMapping("/video")
public RESTResult<Map<String, String>> uploadVideo(
@PostMapping("/audio")
public RESTResult<Map<String, String>> uploadAudio(
@PostMapping("/thumbnail")
public RESTResult<Map<String, String>> uploadThumbnail(
@PostMapping("/final-video")
public RESTResult<Map<String, String>> uploadFinalVideo(
@PostMapping("/reference/character")
public RESTResult<Map<String, String>> uploadCharacterReference(
@PostMapping("/reference/scene")
public RESTResult<Map<String, String>> uploadSceneReference(
@PostMapping("/reference/list")
public RESTResult<List<Map<String, Object>>> listReferenceImages(HttpServletRequest request) {
```

### SvContentCalendarController
```
@RequestMapping("/api/v1/short-video/content-calendar")
@PostMapping("/list")
public RESTResult<PageResultVO<SvContentCalendarVO>> list(@RequestBody(required = false) SvContentCalendarSearchVO searchVO,
@PostMapping("/get")
public RESTResult<SvContentCalendarVO> get(@RequestBody Map<String, Long> body, @CurrentUserId Long userId) {
@PostMapping("/save")
public RESTResult<Long> save(@Valid @RequestBody SvContentCalendarSaveVO saveVO, @CurrentUserId Long userId) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestBody Map<String, Long> body, @CurrentUserId Long userId) {
@PostMapping("/date-range")
public RESTResult<List<SvContentCalendarVO>> dateRange(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
@PostMapping("/auto-generate")
public RESTResult<Integer> autoGenerate(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
```

### SvScriptTemplateController
```
@RequestMapping("/api/v1/short-video/script-template")
@PostMapping("/search")
public RESTResult<PageResultVO<SvScriptTemplateVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<SvScriptTemplateVO> get(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody SvScriptTemplateSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
@PostMapping("/use-count")
public RESTResult<Void> incrementUseCount(HttpServletRequest request,
@PostMapping("/by-scene")
public RESTResult<List<SvScriptTemplateVO>> listByScene(HttpServletRequest request,
```

### VideoGenerationTaskController
```
@RequestMapping("/api/v1/short-video/video-task")
@PostMapping("/submit")
public RESTResult<Map<String, Object>> submit(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/status")
public RESTResult<VideoGenerationTaskService.TaskStatusVO> status(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/cancel")
public RESTResult<Void> cancel(@RequestBody Map<String, Object> body, HttpServletRequest request) {
@PostMapping("/retry")
public RESTResult<Map<String, Object>> retry(@RequestBody Map<String, Object> body, HttpServletRequest request) {
```

### ViralRemakeController
```
@RequestMapping("/api/v1/short-video/viral-remake")
@PostMapping("/recommend")
public RESTResult<Void> recommend(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
@PostMapping("/batch-recommend")
public RESTResult<Integer> batchRecommend(@RequestBody(required = false) Map<String, Object> body, @CurrentUserId Long userId) {
@PostMapping("/confirm")
public RESTResult<Void> confirm(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
@PostMapping("/generate-script")
public RESTResult<java.util.Map<String, Object>> generateScript(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
@PostMapping("/assign-task")
public RESTResult<Long> assignTask(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
@PostMapping("/complete")
public RESTResult<Void> complete(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
```

### ViralVideoController
```
@RequestMapping("/api/v1/short-video/viral")
@PostMapping("/list")
public RESTResult<List<SvViralVideo>> list(
@PostMapping("/collect")
public RESTResult<Long> collect(@RequestBody ViralCollectVO vo, HttpServletRequest request) {
@PostMapping("/get")
public RESTResult<SvViralVideo> get(@RequestParam Long id, HttpServletRequest request) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestParam Long id, HttpServletRequest request) {
@PostMapping("/analyze")
public RESTResult<Void> analyze(@RequestParam Long id, HttpServletRequest request) {
@PostMapping("/replicate")
public RESTResult<String> replicate(@RequestParam Long id, HttpServletRequest request) {
@PostMapping("/recommended")
public RESTResult<List<SvViralVideo>> recommended(
```

### WorkflowController
```
@RequestMapping("/api/v1/short-video/workflow")
@PostMapping("/execute")
public RESTResult<Map<String, Object>> execute(@RequestBody Map<String, Object> body,
@PostMapping("/status")
public RESTResult<Map<String, Object>> status(@RequestBody Map<String, Object> body) {
@PostMapping("/ai-assist")
public RESTResult<Map<String, Object>> aiAssist(@RequestBody Map<String, Object> body, HttpServletRequest request) {
```

### WorkflowTemplateController
```
@RequestMapping("/api/v1/short-video/workflow-template")
@PostMapping("/list")
public RESTResult<List<Map<String, Object>>> list(@CurrentUserId Long userId) {
@PostMapping("/get")
public RESTResult<SvWorkflowTemplate> get(@RequestBody Map<String, Long> body, @CurrentUserId Long userId) {
```

## Entity 字段

### SvAccountCollectTask
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "account_id")
private Long accountId;
@Column(name = "account_url", length = 512)
private String accountUrl;
@Column(name = "account_name", length = 128)
private String accountName;
@Column(name = "sec_uid", length = 256)
private String secUid;
@Column(name = "input_type", length = 32)
private String inputType;
@Column(name = "original_input", length = 1024)
private String originalInput;
@Column(name = "status", nullable = false, length = 32)
private String status = "pending";
@Column(name = "total_videos", nullable = false)
private Integer totalVideos = 0;
@Column(name = "collected_videos", nullable = false)
private Integer collectedVideos = 0;
@Column(name = "analyzed_videos", nullable = false)
private Integer analyzedVideos = 0;
@Column(name = "indexed_videos", nullable = false)
private Integer indexedVideos = 0;
@Column(name = "target_kb_id")
private Long targetKbId;
@Column(name = "error_message", columnDefinition = "TEXT")
private String errorMessage;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvAudienceProfileImport
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "data_source", nullable = false, length = 64)
private String dataSource = "third_party_csv";
@Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
private String payloadJson;
@Column(name = "row_count", nullable = false)
private Integer rowCount = 0;
@Column(name = "create_time", nullable = false)
private Timestamp createTime;
@Column(name = "update_time", nullable = false)
private Timestamp updateTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### SvBgmLibrary
```
@Id
private Long id;
@Column(name = "bgm_name", nullable = false, length = 200)
private String bgmName;
@Column(name = "artist", length = 100)
private String artist;
@Column(name = "style", nullable = false, length = 64)
private String style;
@Column(name = "bpm")
private Integer bpm;
@Column(name = "mood", length = 64)
private String mood;
@Column(name = "energy_level")
private Integer energyLevel;
@Column(name = "emotion_curve_match", length = 200)
private String emotionCurveMatch;
@Column(name = "usage_count")
private Integer usageCount = 0;
@Column(name = "avg_viral_score")
private Double avgViralScore;
@Column(name = "best_content_types", columnDefinition = "jsonb")
private List<String> bestContentTypes;
@Column(name = "license_type", length = 32)
private String licenseType;
@Column(name = "source_platform", length = 32)
private String sourcePlatform;
@Column(name = "expire_date")
private LocalDate expireDate;
@Column(name = "duration_seconds")
private Integer durationSeconds;
@Column(name = "tags", columnDefinition = "jsonb")
private List<String> tags;
@Column(name = "create_time")
private LocalDateTime createTime;
@Column(name = "deleted")
private Integer deleted = 0;
```

### SvCategory
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "name", nullable = false, length = 64)
private String name;
@Column(name = "description", length = 256)
private String description;
@Column(name = "sort_order")
private Integer sortOrder = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvCinematicPreset
```
@Id
private Long id;
@Column(name = "name", nullable = false, length = 100)
private String name;
@Column(name = "category", length = 50)
private String category;
@Column(name = "camera_type", nullable = false, length = 50)
private String cameraType;
@Column(name = "prompt_template", nullable = false, columnDefinition = "TEXT")
private String promptTemplate;
@Column(name = "negative_prompt", columnDefinition = "TEXT")
private String negativePrompt;
@Column(name = "quality_level", length = 20)
private String qualityLevel = "premium-fhd";
@Column(name = "best_model", length = 50)
private String bestModel;
@Column(name = "success_rate", precision = 5, scale = 2)
private BigDecimal successRate = BigDecimal.ZERO;
@Column(name = "avg_quality_score", precision = 5, scale = 2)
private BigDecimal avgQualityScore = BigDecimal.ZERO;
@Column(name = "use_count")
private Integer useCount = 0;
@Column(name = "sample_video_url", length = 500)
private String sampleVideoUrl;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### SvComment
```
@Id
private Long id;
@Column(name = "video_id", nullable = false)
private Long videoId;
@Column(name = "douyin_comment_id", length = 128)
private String douyinCommentId;
@Column(name = "content", nullable = false, columnDefinition = "TEXT")
private String content;
@Column(name = "author_name", length = 128)
private String authorName;
@Column(name = "author_avatar", length = 512)
private String authorAvatar;
@Column(name = "like_count")
private Integer likeCount = 0;
@Column(name = "reply_count")
private Integer replyCount = 0;
@Column(name = "sentiment", length = 16)
private String sentiment;
@Column(name = "sentiment_score", precision = 3, scale = 2)
private BigDecimal sentimentScore;
@Column(name = "comment_time")
private Timestamp commentTime;
@Column(name = "video_source", length = 64)
private String videoSource;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### SvCompetitor
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "competitor_name", nullable = false, length = 100)
private String competitorName;
@Column(name = "platform", length = 32)
private String platform = "douyin";
@Column(name = "account_id", length = 100)
private String accountId;
@Column(name = "account_url", length = 500)
private String accountUrl;
@Column(name = "category", length = 64)
private String category;
@Column(name = "fan_count")
private Long fanCount = 0L;
@Column(name = "notes", columnDefinition = "TEXT")
private String notes;
@Column(name = "is_active")
private Boolean isActive = true;
@Column(name = "create_time")
private LocalDateTime createTime;
@Column(name = "update_time")
private LocalDateTime updateTime;
@Column(name = "deleted")
private Integer deleted = 0;
```

### SvCompetitorSnapshot
```
@Id
private Long id;
@Column(name = "competitor_id", nullable = false)
private Long competitorId;
@Column(name = "snapshot_date", nullable = false)
private LocalDate snapshotDate;
@Column(name = "fan_count")
private Long fanCount;
@Column(name = "fan_delta")
private Integer fanDelta;
@Column(name = "video_count")
private Integer videoCount;
@Column(name = "avg_view_count")
private Long avgViewCount;
@Column(name = "avg_like_rate")
private Double avgLikeRate;
@Column(name = "avg_completion_rate")
private Double avgCompletionRate;
@Column(name = "top_video_titles", columnDefinition = "jsonb")
private List<String> topVideoTitles;
@Column(name = "content_strategy_summary", columnDefinition = "TEXT")
private String contentStrategySummary;
@Column(name = "create_time")
private LocalDateTime createTime;
```

### SvContentCalendar
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "persona_id")
private Long personaId;
@Column(name = "plan_date", nullable = false)
private LocalDate planDate;
@Column(name = "content_type", length = 32, nullable = false)
private String contentType;
@Column(name = "title", length = 256)
private String title;
@Column(name = "brief", columnDefinition = "TEXT")
private String brief;
@Column(name = "script_id")
private Long scriptId;
@Column(name = "project_id")
private Long projectId;
@Column(name = "shooting_task_id")
private Long shootingTaskId;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "priority", nullable = false)
private Integer priority = 0;
@Column(name = "publish_time", length = 16)
private String publishTime;
@Column(name = "account_id")
private Long accountId;
@Column(name = "tags", columnDefinition = "TEXT")
private String tags;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private LocalDateTime createTime;
@Column(name = "update_time")
private LocalDateTime updateTime;
```

### SvDailyBatch
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "persona_id")
private Long personaId;
@Column(name = "source_type", length = 32, nullable = false)
private String sourceType;
@Column(name = "batch_size", nullable = false)
private int batchSize = 3;
@Column(name = "status", length = 16, nullable = false)
private String status = "pending";
@Column(name = "result_summary", columnDefinition = "jsonb")
private String resultSummary;
@Column(name = "deleted", nullable = false)
private int deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvDigitalHumanTask
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "project_id")
private Long projectId;
@Column(nullable = false, length = 20)
private String provider;
@Column(name = "external_task_id", nullable = false)
private String externalTaskId;
@Column(nullable = false, length = 20)
private String status = "SUBMITTED";
@Column(name = "video_url", columnDefinition = "TEXT")
private String videoUrl;
@Column(name = "error_message", columnDefinition = "TEXT")
private String errorMessage;
@Column(name = "params_json", columnDefinition = "TEXT")
private String paramsJson;
@Column(name = "result_json", columnDefinition = "TEXT")
private String resultJson;
@Column(name = "retry_count")
private Integer retryCount = 0;
@Column(name = "max_retries")
private Integer maxRetries = 10;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
@Column
private Integer deleted = 0;
```

### SvDrama
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(nullable = false, length = 200)
private String title;
@Column(columnDefinition = "TEXT")
private String description;
@Column(length = 50)
private String genre;
@Column(name = "total_episodes")
private Integer totalEpisodes = 1;
@Column(length = 20)
private String status = "draft";
@Column(name = "cover_url", length = 500)
private String coverUrl;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
private Integer episodesWithSynopsis;
private Integer episodesWithProject;
```

### SvDramaCharacter
```
@Id
private Long id;
@Column(name = "drama_id", nullable = false)
private Long dramaId;
@Column(name = "character_name", nullable = false, length = 100)
private String characterName;
@Column(columnDefinition = "TEXT")
private String description;
@Column(name = "reference_image_url", length = 500)
private String referenceImageUrl;
@Column(name = "reference_bos_key", length = 500)
private String referenceBosKey;
@Column(name = "voice_id", length = 100)
private String voiceId;
@Column(name = "reference_images", columnDefinition = "jsonb")
private String referenceImages;
@Column(name = "lora_model_path", length = 500)
private String loraModelPath;
@Column(name = "prompt_tags", length = 500)
private String promptTags;
@Column(name = "voice_sample_url", length = 500)
private String voiceSampleUrl;
@Column(name = "cloned_voice_id", length = 100)
private String clonedVoiceId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvDramaEpisode
```
@Id
private Long id;
@Column(name = "drama_id", nullable = false)
private Long dramaId;
@Column(name = "episode_number", nullable = false)
private Integer episodeNumber;
@Column(length = 200)
private String title;
@Column(name = "project_id")
private Long projectId;
@Column(columnDefinition = "TEXT")
private String synopsis;
@Column(columnDefinition = "TEXT")
private String cliffhanger;
@Column(length = 20)
private String status = "draft";
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvGenerationLog
```
@Id
private Long id;
@Column(name = "project_id")
private Long projectId;
@Column(name = "shot_id")
private Long shotId;
@Column(name = "camera_type", length = 50)
private String cameraType;
@Column(name = "quality_level", length = 20)
private String qualityLevel;
@Column(name = "prompt", columnDefinition = "TEXT")
private String prompt;
@Column(name = "ai_provider", length = 50)
private String aiProvider;
@Column(name = "success", nullable = false)
private Boolean success = false;
@Column(name = "quality_score", precision = 5, scale = 2)
private BigDecimal qualityScore;
@Column(name = "generation_time_ms")
private Long generationTimeMs;
@Column(name = "video_url", length = 500)
private String videoUrl;
@Column(name = "error_message", columnDefinition = "TEXT")
private String errorMessage;
@Column(name = "content_type", length = 50)
@Column(name = "route_reason", length = 200)
@Column(name = "cost_cents")
@Column(name = "has_audio")
@Column(name = "create_time")
private Timestamp createTime;
```

### SvHotTopic
```
@Id
private Long id;
@Column(name = "source", nullable = false, length = 32)
@Column(name = "douyin_hot_id", length = 128)
private String douyinHotId;
@Column(name = "title", nullable = false, length = 256)
private String title;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "heat_score")
private Long heatScore = 0L;
@Column(name = "category", length = 64)
private String category;
@Column(name = "related_tags", length = 512)
private String relatedTags;
@Column(name = "status", nullable = false, length = 16)
@Column(name = "expiry_time")
private Timestamp expiryTime;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvMaterial
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "material_type", nullable = false, length = 50)
@Column(name = "url", nullable = false, length = 500)
@Column(name = "bos_key", length = 500)
@Column(name = "shot_id")
private Long shotId;
@Column(name = "project_id")
private Long projectId;
@Column(name = "file_size")
private Long fileSize;
@Column(name = "duration")
private Integer duration;
@Column(name = "width")
private Integer width;
@Column(name = "height")
private Integer height;
@Column(name = "format_type", length = 50)
@Column(name = "generation_type", length = 50)
@Column(name = "ai_prompt", columnDefinition = "TEXT")
private String aiPrompt;
@Column(name = "ai_model", length = 100)
private String aiModel;
@Column(name = "post_processing_config", columnDefinition = "TEXT")
@Column(name = "ai_provider", length = 50)
@Column(name = "perceptual_hash", length = 64)
private String perceptualHash;
@Column(name = "thumbnail_url", length = 512)
private String thumbnailUrl;
@Column(name = "content_sha256", length = 64)
private String contentSha256;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### SvMaterialTagCatalog
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "tag", nullable = false, length = 64)
private String tag;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvOpsReportTemplate
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(nullable = false, length = 256)
private String name;
@Column(name = "definition_json", columnDefinition = "TEXT")
private String definitionJson;
@Column(nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvPlan
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "title", nullable = false, length = 256)
private String title;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "category_id")
private Long categoryId;
@Column(name = "persona_id")
private Long personaId;
@Column(name = "target_platform", length = 32)
private String targetPlatform;
@Column(name = "plan_status", nullable = false)
@Column(name = "ai_generated", nullable = false)
private Integer aiGenerated = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvPlanAsset
```
@Id
private Long id;
@Column(name = "plan_id", nullable = false)
private Long planId;
@Column(name = "asset_type", nullable = false, length = 32)
@Column(name = "asset_name", length = 256)
private String assetName;
@Column(name = "content", columnDefinition = "TEXT")
private String content;
@Column(name = "file_url", length = 512)
private String fileUrl;
@Column(name = "bos_key", length = 500)
@Column(name = "version", nullable = false)
private Integer version = 1;
@Column(name = "is_selected", nullable = false)
private Integer isSelected = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvProject
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "account_id")
private Long accountId;
@Column(name = "title", nullable = false, length = 255)
private String title;
@Column(name = "project_type", nullable = false, length = 50)
@Column(name = "persona_id")
@Column(name = "schedule_date")
@Column(name = "shoot_status", length = 32)
@Column(name = "status", nullable = false, length = 50)
@Column(name = "script_id")
private Long scriptId;
@Column(name = "shot_list_id")
private Long shotListId;
@Column(name = "final_video_url", length = 500)
@Column(name = "thumbnail_url", length = 500)
@Column(name = "character_reference_url", length = 500)
@Column(name = "scene_reference_url", length = 500)
@Column(name = "duration")
private Integer duration;
@Column(name = "publish_title", length = 255)
private String publishTitle;
@Column(name = "publish_platforms", columnDefinition = "TEXT")
@Column(name = "publish_time")
private Timestamp publishTime;
@Column(name = "review_status", length = 50)
private String reviewStatus;
@Column(name = "reviewer_id")
private Long reviewerId;
@Column(name = "review_time")
private Timestamp reviewTime;
@Column(name = "review_comment", columnDefinition = "TEXT")
private String reviewComment;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "version", nullable = false)
private Integer version = 1;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvPublishTimeAnalysis
```
@Id
private Long id;
@Column(name = "account_id", nullable = false)
private Long accountId;
@Column(name = "day_of_week", nullable = false)
@Column(name = "hour_of_day", nullable = false)
@Column(name = "avg_view_count")
private Long avgViewCount = 0L;
@Column(name = "video_count")
private Integer videoCount = 0;
@Column(name = "recommended")
private Boolean recommended = false;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvRemakeTemplate
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId = 0L;
@Column(name = "template_name", nullable = false, length = 100)
private String templateName;
@Column(name = "remake_type", nullable = false, length = 32)
private String remakeType;
@Column(name = "source_viral_id")
private Long sourceViralId;
@Column(name = "structure_template", nullable = false, columnDefinition = "jsonb")
private Object structureTemplate;
@Column(name = "emotion_curve", length = 200)
private String emotionCurve;
@Column(name = "bgm_style", length = 64)
private String bgmStyle;
@Column(name = "duration_range", length = 32)
private String durationRange;
@Column(name = "adaptation_guide", columnDefinition = "TEXT")
private String adaptationGuide;
@Column(name = "variable_slots", columnDefinition = "jsonb")
private List<Object> variableSlots;
@Column(name = "usage_count")
private Integer usageCount = 0;
@Column(name = "avg_viral_score")
private Double avgViralScore;
@Column(name = "content_types", columnDefinition = "jsonb")
private List<String> contentTypes;
@Column(name = "tags", columnDefinition = "jsonb")
private List<String> tags;
@Column(name = "create_time")
private LocalDateTime createTime;
@Column(name = "update_time")
private LocalDateTime updateTime;
@Column(name = "deleted")
private Integer deleted = 0;
```

### SvSceneCameraMapping
```
@Id
private Long id;
@Column(name = "scene_keyword", nullable = false, length = 100)
private String sceneKeyword;
@Column(name = "recommended_camera", nullable = false, length = 50)
private String recommendedCamera;
@Column(name = "confidence", precision = 5, scale = 2)
private BigDecimal confidence;
@Column(name = "source", length = 20)
private String source;
@Column(name = "create_time")
private Timestamp createTime;
```

### SvScript
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "title", nullable = false, length = 255)
private String title;
@Column(name = "content", nullable = false, columnDefinition = "TEXT")
private String content;
@Column(name = "script_type", nullable = false, length = 50)
@Column(name = "generation_type", length = 50)
@Column(name = "reference_viral_id")
private Long referenceViralId;
@Column(name = "theme", length = 255)
private String theme;
@Column(name = "style", length = 50)
@Column(name = "duration")
private Integer duration;
@Column(name = "word_count")
private Integer wordCount;
@Column(name = "tags", columnDefinition = "TEXT")
@Column(name = "ai_prompt", columnDefinition = "TEXT")
private String aiPrompt;
@Column(name = "ai_model", length = 100)
private String aiModel;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvScriptTemplate
```
@Id
private Long id;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "template_name", nullable = false, length = 256)
private String templateName;
@Column(name = "template_type", nullable = false, length = 32)
private String templateType = "system";
@Column(name = "scene", length = 64)
private String scene;
@Column(name = "category_id")
private Long categoryId;
@Column(name = "content", nullable = false, columnDefinition = "TEXT")
private String content;
@Column(name = "description", length = 512)
private String description;
@Column(name = "duration_hint")
private Integer durationHint;
@Column(name = "use_count", nullable = false)
private Long useCount = 0L;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvShootingTask
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "anchor_user_id")
private Long anchorUserId;
@Column(name = "persona_id")
private Long personaId;
@Column(name = "photographer_id")
private Long photographerId;
@Column(name = "script_id")
private Long scriptId;
@Column(name = "project_id")
private Long projectId;
@Column(name = "title", nullable = false, length = 256)
private String title;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "script_content", columnDefinition = "TEXT")
private String scriptContent;
@Column(name = "shooting_brief", columnDefinition = "TEXT")
private String shootingBrief;
@Column(name = "shoot_date", nullable = false)
private Date shootDate;
@Column(name = "priority", nullable = false)
private Integer priority = 0;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "material_urls", columnDefinition = "TEXT")
private String materialUrls;
@Column(name = "review_notes", columnDefinition = "TEXT")
private String reviewNotes;
@Column(name = "reviewed_by")
private Long reviewedBy;
@Column(name = "reference_video_url", length = 512)
private String referenceVideoUrl;
@Column(name = "reference_video_task_id")
private Long referenceVideoTaskId;
@Column(name = "reference_generated_at")
private Timestamp referenceGeneratedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvShot
```
@Id
private Long id;
@Column(name = "shot_list_id", nullable = false)
private Long shotListId;
@Column(name = "shot_number", nullable = false)
private Integer shotNumber;
@Column(name = "time_range", length = 50)
@Column(name = "scene_description", columnDefinition = "TEXT")
private String sceneDescription;
@Column(name = "camera_angle", length = 100)
private String cameraAngle;
@Column(name = "camera_type", length = 50)
@Column(name = "camera_params", columnDefinition = "TEXT")
@Column(name = "quality_level", length = 20)
@Column(name = "ai_model", length = 50)
@Column(name = "quality_score", precision = 5, scale = 2)
@Column(name = "action", length = 255)
private String action;
@Column(name = "dialogue", columnDefinition = "TEXT")
private String dialogue;
@Column(name = "mood", length = 100)
private String mood;
@Column(name = "keyframe_url", length = 500)
@Column(name = "keyframe_bos_key", length = 500)
private String keyframeBosKey;
@Column(name = "end_frame_url", length = 500)
@Column(name = "end_frame_bos_key", length = 500)
private String endFrameBosKey;
@Column(name = "video_url", length = 500)
@Column(name = "video_bos_key", length = 500)
private String videoBosKey;
@Column(name = "audio_url", length = 500)
@Column(name = "audio_bos_key", length = 500)
private String audioBosKey;
@Column(name = "dialogue_text", columnDefinition = "TEXT")
@Column(name = "sfx_hints", length = 500)
@Column(name = "tts_url", length = 500)
@Column(name = "bgm_url", length = 500)
@Column(name = "duration")
private Integer duration;
@Column(name = "review_status", length = 16)
@Column(name = "reviewer_note", columnDefinition = "TEXT")
private String reviewerNote;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvShotList
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "shot_count", nullable = false)
private Integer shotCount;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvStoryFormulaTemplate
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(nullable = false, length = 64)
private String code;
@Column(nullable = false, length = 256)
private String name;
@Column(columnDefinition = "TEXT")
private String description;
@Column(name = "formula_json", columnDefinition = "TEXT")
private String formulaJson;
@Column(nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvVideo
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "account_id", nullable = false)
private Long accountId;
@Column(name = "douyin_video_id", length = 128)
private String douyinVideoId;
@Column(name = "title", length = 512)
private String title;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "cover_url", length = 512)
private String coverUrl;
@Column(name = "video_url", length = 512)
private String videoUrl;
@Column(name = "duration")
private Integer duration = 0;
@Column(name = "tags", length = 512)
private String tags;
@Column(name = "category_id")
private Long categoryId;
@Column(name = "publish_time")
private Timestamp publishTime;
@Column(name = "view_count")
private Long viewCount = 0L;
@Column(name = "like_count")
private Integer likeCount = 0;
@Column(name = "comment_count")
private Integer commentCount = 0;
@Column(name = "share_count")
private Integer shareCount = 0;
@Column(name = "favorite_count")
private Integer favoriteCount = 0;
@Column(name = "is_viral")
private Boolean isViral = false;
@Column(name = "ai_call_log_id")
@Column(name = "ai_generated")
private Boolean aiGenerated = false;
@Column(name = "plan_id")
private Long planId;
@Column(name = "sync_status", length = 16)
private String syncStatus = "synced";
@Column(name = "last_sync_time")
private Timestamp lastSyncTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvVideoData
```
@Id
private Long id;
@Column(name = "video_id", nullable = false)
private Long videoId;
@Column(name = "snapshot_date", nullable = false)
private Date snapshotDate;
@Column(name = "view_count")
private Long viewCount = 0L;
@Column(name = "like_count")
private Integer likeCount = 0;
@Column(name = "comment_count")
private Long commentCount = 0L;
@Column(name = "share_count")
private Long shareCount = 0L;
@Column(name = "favorite_count")
private Integer favoriteCount = 0;
@Column(name = "new_followers")
private Integer newFollowers = 0;
@Column(name = "view_delta")
private Long viewDelta = 0L;
@Column(name = "like_delta")
private Integer likeDelta = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### SvVideoGeneration
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "plan_id")
private Long planId;
@Column(name = "task_type", nullable = false, length = 32)
@Column(name = "input_params", columnDefinition = "TEXT")
@Column(name = "output_result", columnDefinition = "TEXT")
@Column(name = "task_status", nullable = false)
@Column(name = "error_message", length = 512)
private String errorMessage;
@Column(name = "model_used", length = 64)
private String modelUsed;
@Column(name = "tokens_used")
private Long tokensUsed = 0L;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvVideoGenerationTask
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "project_id")
private Long projectId;
@Column(name = "shot_list_id")
private Long shotListId;
@Column(name = "request_json", nullable = false, columnDefinition = "TEXT")
private String requestJson;
@Column(name = "status", nullable = false, length = 20)
private String status = "pending";
@Column(name = "progress_current")
private Integer progressCurrent = 0;
@Column(name = "progress_total")
private Integer progressTotal = 0;
@Column(name = "result_json", columnDefinition = "TEXT")
private String resultJson;
@Column(name = "error_message", columnDefinition = "TEXT")
private String errorMessage;
@Column(name = "processing_started_at")
private Timestamp processingStartedAt;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvViralFavorite
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "viral_video_id", nullable = false)
private Long viralVideoId;
@Column(name = "create_time")
private Timestamp createTime;
```

### SvViralRemakeLog
```
@Id
private Long id;
@Column(name = "viral_video_id", nullable = false)
private Long viralVideoId;
@Column(name = "from_status")
private Integer fromStatus;
@Column(name = "to_status", nullable = false)
private Integer toStatus;
@Column(name = "operator_id")
private Long operatorId;
@Column(name = "remark", columnDefinition = "TEXT")
private String remark;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### SvViralVideo
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "source_video_id")
private Long sourceVideoId;
@Column(name = "douyin_video_id", length = 128)
private String douyinVideoId;
@Column(name = "title", length = 512)
private String title;
@Column(name = "cover_url", length = 512)
private String coverUrl;
@Column(name = "video_url", length = 512)
private String videoUrl;
@Column(name = "author_name", length = 128)
private String authorName;
@Column(name = "view_count")
private Long viewCount = 0L;
@Column(name = "like_count")
private Long likeCount = 0L;
@Column(name = "share_count")
private Long shareCount = 0L;
@Column(name = "category_id")
private Long categoryId;
@Column(name = "tags", length = 512)
private String tags;
@Column(name = "viral_score")
private Integer viralScore = 0;
@Column(name = "analysis_result", columnDefinition = "TEXT")
private String analysisResult;
@Column(name = "publish_time")
private Timestamp publishTime;
@Column(name = "deep_analyze_status", length = 16)
private String deepAnalyzeStatus = "pending";
@Column(name = "remake_status")
private Integer remakeStatus = 0;
@Column(name = "industry_tags", length = 512)
private String industryTags;
@Column(name = "auto_collected", nullable = false)
private boolean autoCollected = false;
@Column(name = "collect_source", length = 64)
private String collectSource;
@Column(name = "collect_task_id")
private Long collectTaskId;
@Column(name = "cover_bos_url", length = 512)
private String coverBosUrl;
@Column(name = "video_bos_url", length = 512)
private String videoBosUrl;
@Column(name = "comment_count")
private Long commentCount;
@Column(name = "favorite_count")
private Long favoriteCount;
@Column(name = "video_duration")
private Integer videoDuration;
@Column(name = "transcript", columnDefinition = "TEXT")
private String transcript;
@Column(name = "deep_analysis_result", columnDefinition = "TEXT")
private String deepAnalysisResult;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "hashtags", length = 512)
private String hashtags;
@Column(name = "scene_descriptions", columnDefinition = "TEXT")
private String sceneDescriptions;
@Column(name = "deep_analyze_progress", length = 256)
private String deepAnalyzeProgress;
@Column(name = "remake_variable_table", columnDefinition = "TEXT")
private String remakeVariableTable;
@Column(name = "author_id", length = 128)
private String authorId;
@Column(name = "author_followers")
private Long authorFollowers;
@Column(name = "music_name", length = 128)
private String musicName;
@Column(name = "metadata_json", columnDefinition = "TEXT")
private String metadataJson;
@Column(name = "video_bos_key", length = 512)
private String videoBosKey;
@Column(name = "keyframe_bos_keys", columnDefinition = "TEXT")
private String keyframeBosKeys;
@Column(name = "keyframe_bos_urls", columnDefinition = "TEXT")
private String keyframeBosUrls;
@Column(name = "cover_bos_key", length = 512)
private String coverBosKey;
@Column(name = "deep_analyzed_at")
private java.sql.Timestamp deepAnalyzedAt;
@Column(name = "deep_analyze_steps", columnDefinition = "TEXT")
private String deepAnalyzeSteps;
@Column(name = "remake_suggestions", columnDefinition = "TEXT")
private String remakeSuggestions;
@Column(name = "matched_persona_ids", length = 512)
private String matchedPersonaIds;
@Column(name = "confirmed_by")
private Long confirmedBy;
@Column(name = "confirmed_at")
private java.sql.Timestamp confirmedAt;
@Column(name = "confirmed_remake_type", length = 32)
private String confirmedRemakeType;
@Column(name = "remake_script_id")
private Long remakeScriptId;
@Column(name = "remake_task_id")
private Long remakeTaskId;
@Column(name = "deep_analyze_started_at")
private java.sql.Timestamp deepAnalyzeStartedAt;
@Column(name = "light_analysis_result", columnDefinition = "TEXT")
private String lightAnalysisResult;
@Column(name = "deep_analyze_error", length = 512)
private String deepAnalyzeError;
@Column(name = "deep_analyze_finished_at")
private java.sql.Timestamp deepAnalyzeFinishedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvWebhookDlq
```
@Id
private Long id;
@Column(name = "create_time", nullable = false)
private Timestamp createTime;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "task_id", nullable = false)
private Long taskId;
@Column(name = "webhook_url_sha256", nullable = false, length = 64)
private String webhookUrlSha256;
@Column(name = "last_http_status")
private Integer lastHttpStatus;
@Column(name = "attempt_count", nullable = false)
private Integer attemptCount;
@Column(name = "error_preview", length = 512)
private String errorPreview;
@Column(name = "event_code", length = 64)
private String eventCode;
```

### SvWorkflowTask
```
@Id
private Long id;
@Column(name = "task_id", nullable = false, unique = true, length = 128)
private String taskId;
@Column(name = "project_id")
private Long projectId;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "status", nullable = false, length = 32)
private String status = "processing";
@Column(name = "current_step", length = 64)
private String currentStep = "script";
@Column(name = "progress")
private Integer progress = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SvWorkflowTemplate
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId = 0L;
@Column(name = "template_name", nullable = false, length = 128)
private String templateName;
@Column(name = "description", length = 512)
private String description;
@Column(name = "steps", columnDefinition = "TEXT")
private String steps;
@Column(name = "is_system", nullable = false)
private Integer isSystem = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

