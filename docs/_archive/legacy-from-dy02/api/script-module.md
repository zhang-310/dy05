# script 模块 API 文档

## 文件结构
```
controller/ComplianceController.java
controller/ComplianceWordAdminController.java
controller/HybridSearchController.java
controller/ScriptController.java
controller/ScriptGenerationController.java
controller/ScriptTemplateAdminController.java
controller/ScriptTemplateController.java
controller/UserViolationWordController.java
controller/ViolationWordAdminController.java
entity/ComplianceWord.java
entity/ScriptCheck.java
entity/ScriptGeneration.java
entity/ScriptLibrary.java
entity/ScriptTemplate.java
entity/ScriptVariant.java
entity/ScriptVectorEmbedding.java
entity/SearchAnalytics.java
entity/SearchResult.java
entity/SearchSuggestion.java
entity/UserViolationWord.java
entity/ViolationWord.java
repository/ComplianceWordRepository.java
repository/ScriptCheckRepository.java
repository/ScriptGenerationRepository.java
repository/ScriptLibraryRepository.java
repository/ScriptTemplateRepository.java
repository/ScriptVariantRepository.java
repository/ScriptVectorEmbeddingRepository.java
repository/SearchAnalyticsRepository.java
repository/SearchResultRepository.java
repository/SearchSuggestionRepository.java
repository/UserViolationWordRepository.java
repository/ViolationWordRepository.java
service/AiService.java
service/ComplianceWordService.java
service/IndustryComplianceService.java
service/ScriptCacheService.java
service/ScriptGenerationService.java
service/ScriptLibraryService.java
service/ScriptTemplateService.java
service/SearchSuggestionService.java
service/UserViolationWordService.java
service/VectorEmbeddingService.java
service/VectorSearchService.java
service/ViolationWordService.java
service/impl/ComplianceWordServiceImpl.java
service/impl/DeepSeekAiServiceImpl.java
service/impl/IndustryComplianceServiceImpl.java
service/impl/LlmClientAiServiceImpl.java
service/impl/RedisCacheServiceImpl.java
service/impl/ScriptGenerationServiceImpl.java
service/impl/ScriptLibraryServiceImpl.java
service/impl/ScriptTemplateServiceImpl.java
service/impl/SearchAnalyticsTask.java
service/impl/SearchSuggestionServiceImpl.java
service/impl/UserViolationWordServiceImpl.java
service/impl/VectorEmbeddingServiceImpl.java
service/impl/VectorEmbeddingTask.java
service/impl/VectorSearchServiceImpl.java
service/impl/ViolationWordServiceImpl.java
vo/HybridSearchRequestVO.java
vo/HybridSearchResultVO.java
vo/ScriptGenerationRequestVO.java
vo/ScriptGenerationVO.java
vo/ScriptSaveVO.java
vo/ScriptSearchVO.java
vo/ScriptTemplateSaveVO.java
vo/ScriptTemplateSearchVO.java
vo/ScriptTemplateVO.java
vo/ScriptVO.java
vo/ScriptVariantVO.java
vo/SearchAnalyticsVO.java
vo/SearchSuggestionVO.java
vo/UserViolationWordSaveVO.java
vo/UserViolationWordSearchVO.java
vo/UserViolationWordVO.java
vo/ViolationCheckBatchResultVO.java
vo/ViolationCheckBatchVO.java
vo/ViolationCheckResultVO.java
vo/ViolationCheckVO.java
vo/ViolationReplacementRequestVO.java
vo/ViolationReplacementResultVO.java
vo/ViolationWordSaveVO.java
vo/ViolationWordSearchVO.java
vo/ViolationWordVO.java
```

## API 接口

### ComplianceController
```
@RequestMapping("/api/v1/script/compliance")
@PostMapping("/check")
public RESTResult<List<Map<String, Object>>> check(@RequestBody Map<String, String> body, HttpServletRequest request) {
@PostMapping("/rules")
public RESTResult<List<Map<String, Object>>> rules(@RequestBody Map<String, String> body, HttpServletRequest request) {
@PostMapping("/industry-codes")
public RESTResult<Map<String, Object>> industryCodes(HttpServletRequest request) {
@PostMapping("/douyin-official-references")
public RESTResult<Map<String, Object>> douyinOfficialReferences(HttpServletRequest request) {
```

### ComplianceWordAdminController
```
@RequestMapping("/api/v1/script/admin/compliance")
@PostMapping("/refresh")
public RESTResult<Void> refresh(HttpServletRequest request) {
```

### HybridSearchController
```
@RequestMapping("/api/v1/script/search")
@PostMapping("/hybrid")
public RESTResult<HybridSearchResultVO> hybridSearch(
@PostMapping("/semantic")
public RESTResult<HybridSearchResultVO> semanticSearch(
@PostMapping("/lexical")
public RESTResult<HybridSearchResultVO> lexicalSearch(
@PostMapping("/suggest")
public RESTResult<SearchSuggestionVO> getSearchSuggestions(
@PostMapping("/analytics")
public RESTResult<List<SearchAnalyticsVO>> getSearchAnalytics(
@PostMapping("/feedback")
public RESTResult<Void> recordClickFeedback(
```

### ScriptController
```
@RequestMapping("/api/v1/script")
@PostMapping("/list")
public RESTResult<PageResultVO<ScriptVO>> list(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<ScriptVO> get(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody ScriptSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/use-count")
public RESTResult<Void> useCount(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/violation/check-batch")
public RESTResult<ViolationCheckBatchResultVO> checkBatch(HttpServletRequest request,
@PostMapping("/violation/check")
public RESTResult<ViolationCheckResultVO> check(HttpServletRequest request,
@PostMapping("/violation/public/list")
public RESTResult<PageResultVO<ViolationWordVO>> publicList(HttpServletRequest request,
@PostMapping("/violation/suggest-replacement")
public RESTResult<ViolationReplacementResultVO> suggestReplacement(HttpServletRequest request,
```

### ScriptGenerationController
```
@RequestMapping("/api/v1/script")
@PostMapping("/generate")
public RESTResult<ScriptGenerationVO> generateScript(
```

### ScriptTemplateAdminController
```
@RequestMapping("/api/v1/script/admin/template")
@PostMapping("/list")
public RESTResult<PageResultVO<ScriptTemplateVO>> list(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody ScriptTemplateSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
```

### ScriptTemplateController
```
@RequestMapping("/api/v1/script/template")
@PostMapping("/search")
public RESTResult<PageResultVO<ScriptTemplateVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<ScriptTemplateVO> get(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody ScriptTemplateSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
@PostMapping("/use-count")
public RESTResult<Void> incrementUseCount(HttpServletRequest request,
@PostMapping("/by-scene")
public RESTResult<List<ScriptTemplateVO>> listByScene(HttpServletRequest request,
```

### UserViolationWordController
```
@RequestMapping("/api/v1/script/user-violation")
@PostMapping("/search")
public RESTResult<PageResultVO<UserViolationWordVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<UserViolationWordVO> get(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody UserViolationWordSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
@PostMapping("/active")
public RESTResult<List<UserViolationWordVO>> listActive(HttpServletRequest request,
```

### ViolationWordAdminController
```
@RequestMapping("/api/v1/script/admin/violation")
@PostMapping("/list")
public RESTResult<PageResultVO<ViolationWordVO>> list(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody ViolationWordSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/active")
public RESTResult<List<ViolationWordVO>> active(HttpServletRequest request,
@PostMapping("/import")
public RESTResult<Map<String, Object>> importCsv(HttpServletRequest request,
@PostMapping("/export")
public ResponseEntity<byte[]> exportCsv(HttpServletRequest request) {
```

## Entity 字段

### ComplianceWord
```
@Id
private Long id;
@Column(name = "word_type", nullable = false, length = 16)
private String wordType;
@Column(name = "word_value", nullable = false, length = 128)
private String wordValue;
@Column(name = "replacement", length = 256)
private String replacement;
@Column(name = "is_enabled", nullable = false)
private Integer isEnabled = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### ScriptCheck
```
@Id
private Long id;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "check_time", nullable = false)
private Timestamp checkTime;
@Column(name = "violation_count", nullable = false)
private Integer violationCount = 0;
@Column(name = "violations", columnDefinition = "TEXT")
private String violations;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### ScriptGeneration
```
@Id
private Long id;
@Column(nullable = false)
private Long ownerId;
@Column(nullable = false, length = 100)
private String productName;
@Column(nullable = false)
private BigDecimal productPrice;
@Column(nullable = false, columnDefinition = "TEXT")
private String keyFeatures;
@Column(nullable = false)
private Integer duration;
@Column(nullable = false, length = 50)
private String style;
@Column(nullable = false)
private Integer variantCount;
@Column(nullable = false)
private LocalDateTime createdTime;
@Column(nullable = false)
private LocalDateTime updatedTime;
@Column(nullable = false)
private Integer deleted;
```

### ScriptLibrary
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "title", nullable = false, length = 256)
private String title;
@Column(name = "content", columnDefinition = "TEXT")
private String content;
@Column(name = "category", length = 32)
private String category;
@Column(name = "source", length = 16)
private String source = "manual";
@Column(name = "source_id")
private Long sourceId;
@Column(name = "tags", columnDefinition = "TEXT")
private String tags;
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

### ScriptTemplate
```
@Id
private Long id;
@Column(name = "template_name", nullable = false, length = 256)
private String templateName;
@Column(name = "template_type", nullable = false, length = 32)
private String templateType = "system";
@Column(name = "scene", length = 64)
private String scene;
@Column(name = "content", nullable = false, columnDefinition = "TEXT")
private String content;
@Column(name = "description", length = 512)
private String description;
@Column(name = "user_id")
private Long userId;
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

### ScriptVariant
```
@Id
private Long id;
@Column(nullable = false)
private Long generationId;
@Column(nullable = false)
private Integer variantIndex;
@Column(nullable = false, columnDefinition = "TEXT")
private String content;
@Column(nullable = false)
private BigDecimal score;
@Column(columnDefinition = "TEXT")
private String keyPoints;
@Column(nullable = false)
private Integer likes;
@Column(nullable = false)
private Integer uses;
@Column(nullable = false)
private LocalDateTime createdTime;
@Column(nullable = false)
private Integer deleted;
```

### ScriptVectorEmbedding
```
@Id
private Long id;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "title", length = 256)
private String title;
@Column(name = "content_text", columnDefinition = "TEXT")
private String contentText;
@Column(name = "vector_embedding", nullable = false, columnDefinition = "BYTEA")
private byte[] vectorEmbedding;
@Column(name = "vector_dimension", nullable = false)
private Integer vectorDimension = 1024;
@Column(name = "embedding_model", length = 100)
private String embeddingModel = "BGE-M3";
@Column(name = "embedding_time_ms")
private Integer embeddingTimeMs = 0;
@Column(name = "is_indexed_milvus")
private Boolean isIndexedMilvus = false;
@Column(name = "milvus_collection_id")
private Long milvusCollectionId;
@Column(name = "last_indexed_at")
private Timestamp lastIndexedAt;
@Column(name = "created_at")
private Timestamp createdAt;
@Column(name = "updated_at")
private Timestamp updatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### SearchAnalytics
```
@Id
private Long id;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "analytics_date", nullable = false)
private Date analyticsDate;
@Column(name = "search_query", length = 512)
private String searchQuery;
@Column(name = "search_type", length = 50)
private String searchType;
@Column(name = "search_count", nullable = false)
private Integer searchCount = 0;
@Column(name = "avg_execution_time_ms", precision = 10, scale = 2)
private BigDecimal avgExecutionTimeMs = BigDecimal.ZERO;
@Column(name = "click_through_rate", precision = 5, scale = 2)
private BigDecimal clickThroughRate = BigDecimal.ZERO;
@Column(name = "satisfaction_score", precision = 4, scale = 2)
private BigDecimal satisfactionScore = BigDecimal.ZERO;
@Column(name = "top_result_id")
private Long topResultId;
@Column(name = "top_result_click_count")
private Integer topResultClickCount = 0;
@Column(name = "created_at")
private Timestamp createdAt;
@Column(name = "updated_at")
private Timestamp updatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### SearchResult
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "query_text", columnDefinition = "TEXT", nullable = false)
private String queryText;
@Column(name = "search_type", nullable = false, length = 50)
private String searchType;
@Column(name = "total_results")
private Integer totalResults = 0;
@Column(name = "top_result_id")
private Long topResultId;
@Column(name = "execution_time_ms")
private Integer executionTimeMs = 0;
@Column(name = "vector_weight", precision = 3, scale = 2)
private BigDecimal vectorWeight = new BigDecimal("0.50");
@Column(name = "lexical_weight", precision = 3, scale = 2)
private BigDecimal lexicalWeight = new BigDecimal("0.50");
@Column(name = "clicked_result_id")
private Long clickedResultId;
@Column(name = "clicked_at")
private Timestamp clickedAt;
@Column(name = "is_satisfied")
private Boolean isSatisfied;
@Column(name = "feedback_at")
private Timestamp feedbackAt;
@Column(name = "user_agent", length = 512)
private String userAgent;
@Column(name = "ip_address", length = 45)
private String ipAddress;
@Column(name = "created_at")
private Timestamp createdAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### SearchSuggestion
```
@Id
private Long id;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "suggestion_text", nullable = false, length = 256)
private String suggestionText;
@Column(name = "suggestion_type", nullable = false, length = 50)
private String suggestionType;
@Column(name = "search_count", nullable = false)
private Integer searchCount = 0;
@Column(name = "trending_score", precision = 4, scale = 3)
private BigDecimal trendingScore = BigDecimal.ZERO;
@Column(name = "result_count")
private Integer resultCount = 0;
@Column(name = "last_searched_at")
private Timestamp lastSearchedAt;
@Column(name = "last_updated")
private Timestamp lastUpdated;
@Column(name = "created_at")
private Timestamp createdAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### UserViolationWord
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "word", nullable = false, length = 256)
private String word;
@Column(name = "scope", length = 16)
private String scope = "all";
@Column(name = "level", nullable = false)
private Integer level = 2;
@Column(name = "reason", length = 128)
private String reason;
@Column(name = "replacement", length = 256)
private String replacement;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### ViolationWord
```
@Id
private Long id;
@Column(name = "word", nullable = false, length = 256)
private String word;
@Column(name = "scope", length = 16)
private String scope = "all";
@Column(name = "level", nullable = false)
private Integer level;
@Column(name = "reason", length = 128)
private String reason;
@Column(name = "replacement", length = 256)
private String replacement;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

