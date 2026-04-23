# product 模块 API 文档

## 文件结构
```
PlaywrightFetchTest.java
controller/EffectivenessScoreController.java
controller/ProductController.java
controller/ProductScriptController.java
controller/ProductScriptVersionController.java
controller/ScriptOptimizationController.java
controller/StylePresetController.java
entity/DyProduct.java
entity/DyProductSalesHistory.java
entity/DyProductScript.java
entity/ProductScriptComparisonCache.java
entity/ProductScriptEffectivenessRecord.java
entity/ProductScriptSnapshot.java
entity/ProductScriptUsage.java
entity/ProductScriptVersion.java
entity/ScriptAnalysisResult.java
entity/ScriptOptimizationSuggestion.java
entity/ScriptRegeneratedVersion.java
entity/ScriptVersionHistory.java
entity/StylePreset.java
repository/DyProductRepository.java
repository/DyProductSalesHistoryRepository.java
repository/DyProductScriptRepository.java
repository/ProductScriptComparisonCacheRepository.java
repository/ProductScriptEffectivenessRecordRepository.java
repository/ProductScriptSnapshotRepository.java
repository/ProductScriptUsageRepository.java
repository/ProductScriptVersionRepository.java
repository/ScriptAnalysisResultRepository.java
repository/ScriptOptimizationSuggestionRepository.java
repository/ScriptRegeneratedVersionRepository.java
repository/ScriptVersionHistoryRepository.java
repository/StylePresetRepository.java
service/BatchProgressCallback.java
service/ComboProgressCallback.java
service/ComplianceService.java
service/EffectivenessScoreService.java
service/PaipingImportService.java
service/PlaywrightHtmlFetcher.java
service/ProductAiService.java
service/ProductLinkExtractService.java
service/ProductScriptRateLimitService.java
service/ProductScriptService.java
service/ProductScriptUsageSyncService.java
service/ProductScriptVersionService.java
service/ProductService.java
service/ScriptOptimizationService.java
service/ScriptVersionHistoryService.java
service/StylePresetService.java
service/impl/ComplianceServiceImpl.java
service/impl/EffectivenessScoreServiceImpl.java
service/impl/PaipingImportServiceImpl.java
service/impl/ProductLinkExtractServiceImpl.java
service/impl/ProductScriptRateLimitServiceImpl.java
service/impl/ProductScriptServiceImpl.java
service/impl/ProductScriptUsageSyncServiceImpl.java
service/impl/ProductScriptVersionServiceImpl.java
service/impl/ProductServiceImpl.java
service/impl/SalesHistoryServiceImpl.java
service/impl/ScriptOptimizationServiceImpl.java
service/impl/ScriptVersionHistoryServiceImpl.java
service/impl/StylePresetServiceImpl.java
task/EffectivenessScoreCalculationTask.java
vo/BatchGenerateRequestVO.java
vo/ComboGenerateRequestVO.java
vo/EnsureOptimizationVersionResultVO.java
vo/EnsureOptimizationVersionVO.java
vo/MultiStyleGenerateRequestVO.java
vo/MultiStyleGenerateResultVO.java
vo/OptimizationSuggestionVO.java
vo/ProductLiveUsageRowVO.java
vo/ProductReadinessSummaryVO.java
vo/ProductRelationsVO.java
vo/ProductSaveVO.java
vo/ProductScriptEffectSummaryVO.java
vo/ProductScriptRecommendVO.java
vo/ProductScriptSaveVO.java
vo/ProductScriptSearchVO.java
vo/ProductScriptSnapshotVO.java
vo/ProductScriptStatisticsVO.java
vo/ProductScriptUsageSaveVO.java
vo/ProductScriptVO.java
vo/ProductScriptVersionSaveVO.java
vo/ProductScriptVersionSearchVO.java
vo/ProductScriptVersionVO.java
vo/ProductSearchVO.java
vo/ProductVO.java
vo/ProductVersionDiffVO.java
vo/RegeneratedScriptVO.java
vo/SalesHistorySaveVO.java
vo/SalesHistorySearchVO.java
vo/SalesHistoryVO.java
vo/ScriptAnalysisResultVO.java
vo/ScriptComparisonVO.java
vo/ScriptEffectivenessAnalysisVO.java
vo/ScriptRankingVO.java
vo/ScriptTrendVO.java
```

## API 接口

### EffectivenessScoreController
```
@RequestMapping("/api/v1/product/script-effectiveness")
@PostMapping("/ranking")
public RESTResult<PageResultVO<ScriptRankingVO>> getRanking(
@PostMapping("/compare")
public RESTResult<ScriptComparisonVO> compareVersions(
@PostMapping("/trend")
public RESTResult<ScriptTrendVO> getTrend(
@PostMapping("/recalculate")
public RESTResult<Integer> recalculate(
@PostMapping("/style-comparison")
public RESTResult<ScriptComparisonVO> getStyleComparison(
@PostMapping("/analysis")
public RESTResult<ScriptEffectivenessAnalysisVO> getAnalysis(
@PostMapping("/record-snapshot")
public RESTResult<Boolean> recordSnapshot(
@PostMapping("/clear-cache")
public RESTResult<Integer> clearCache(
```

### ProductController
```
@RequestMapping("/api/v1/product")
@PostMapping("/search")
public RESTResult<PageResultVO<ProductVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<ProductVO> get(HttpServletRequest request,
@PostMapping("/infer-product-type")
public RESTResult<String> inferProductType(HttpServletRequest request,
@PostMapping("/trigger-extract")
public RESTResult<Void> triggerExtract(HttpServletRequest request,
@PostMapping("/extract-from-link")
public RESTResult<java.util.Map<String, String>> extractFromLink(HttpServletRequest request,
@PostMapping("/import-paiping")
public RESTResult<java.util.Map<String, Object>> importPaiping(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody ProductSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
@PostMapping("/update-inventory")
public RESTResult<Void> updateInventory(HttpServletRequest request,
@PostMapping("/publish")
public RESTResult<Void> publish(HttpServletRequest request,
@PostMapping("/unpublish")
public RESTResult<Void> unpublish(HttpServletRequest request,
@PostMapping("/set-featured")
public RESTResult<Void> setFeatured(HttpServletRequest request,
@PostMapping("/sales-history/search")
public RESTResult<PageResultVO<SalesHistoryVO>> searchSales(HttpServletRequest request,
@PostMapping("/sales-history/get")
public RESTResult<SalesHistoryVO> getSales(HttpServletRequest request,
@PostMapping("/sales-history/save")
public RESTResult<Long> saveSales(HttpServletRequest request, @Valid @RequestBody SalesHistorySaveVO vo) {
@PostMapping("/sales-history/total-sales-amount")
public RESTResult<BigDecimal> totalSalesAmount(HttpServletRequest request,
@PostMapping("/sales-history/total-sales-quantity")
public RESTResult<Long> totalSalesQuantity(HttpServletRequest request,
```

### ProductScriptController
```
@RequestMapping("/api/v1/product/script")
@PostMapping("/save")
public RESTResult<DyProductScript> saveScript(HttpServletRequest request,
@PostMapping("/list")
public RESTResult<List<DyProductScript>> listScripts(HttpServletRequest request,
@PostMapping("/list-by-type")
public RESTResult<List<DyProductScript>> listScriptsByType(HttpServletRequest request,
@PostMapping("/list-by-style")
public RESTResult<Map<String, List<DyProductScript>>> listScriptsByStyle(HttpServletRequest request,
@PostMapping("/active-by-style")
public RESTResult<Map<String, DyProductScript>> getActiveScriptsByStyle(HttpServletRequest request,
@PostMapping("/generate-multi-style")
public RESTResult<MultiStyleGenerateResultVO> generateMultiStyle(HttpServletRequest request,
@PostMapping(value = "/generate-batch-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/active")
public RESTResult<List<DyProductScript>> listActiveScripts(HttpServletRequest request,
@PutMapping("/activate/{scriptId}")
public RESTResult<Void> activateScript(HttpServletRequest request,
@PutMapping("/update/{scriptId}")
public RESTResult<DyProductScript> updateScript(HttpServletRequest request,
@DeleteMapping("/{scriptId}")
public RESTResult<Void> deleteScript(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<DyProductScript> getScript(HttpServletRequest request,
@PostMapping("/history")
public RESTResult<List<ScriptVersionHistory>> listVersionHistory(HttpServletRequest request,
@PostMapping("/rollback/{historyId}")
public RESTResult<DyProductScript> rollbackToVersion(HttpServletRequest request,
```

### ProductScriptVersionController
```
@RequestMapping("/api/v1/product/script-version")
@PostMapping("/save")
public RESTResult<ProductScriptVersionVO> save(
@PostMapping("/list")
public RESTResult<PageResultVO<ProductScriptVersionVO>> list(
@PostMapping("/search")
public RESTResult<PageResultVO<ProductScriptVersionVO>> search(
@PostMapping("/detail/{id}")
public RESTResult<ProductScriptVersionVO> getDetail(
@PostMapping("/recommend")
public RESTResult<ProductScriptRecommendVO> recommend(
@PostMapping("/update-effectiveness")
public RESTResult<ProductScriptVersionVO> updateEffectiveness(
@PostMapping("/apply-from-library")
public RESTResult<ProductScriptSnapshotVO> applyFromLibrary(
@PostMapping("/delete/{id}")
public RESTResult<Boolean> delete(
@PostMapping("/update-status")
public RESTResult<ProductScriptVersionVO> updateStatus(
@PostMapping("/list-by-product")
public RESTResult<List<ProductScriptVersionVO>> listByProductId(
@PostMapping("/best")
public RESTResult<ProductScriptVersionVO> findBestVersion(
@PostMapping("/increase-usage")
public RESTResult<ProductScriptVersionVO> increaseUsageCount(
```

### ScriptOptimizationController
```
@RequestMapping("/api/v1/product/script")
@PostMapping("/analyze")
public RESTResult<ScriptAnalysisResultVO> analyzeScript(@RequestBody Map<String, Object> request,
@PostMapping("/suggestions")
public RESTResult<List<OptimizationSuggestionVO>> getOptimizationSuggestions(
@PostMapping("/regenerate")
public RESTResult<RegeneratedScriptResponse> regenerateOptimized(@RequestBody Map<String, Object> request,
@PostMapping("/optimization-history")
public RESTResult<PageResultVO<ScriptAnalysisResultVO>> optimizationHistory(
@PostMapping("/accept-suggestion")
public RESTResult<Boolean> acceptSuggestion(@RequestBody Map<String, Object> request,
@PostMapping("/reject-suggestion")
public RESTResult<Boolean> rejectSuggestion(@RequestBody Map<String, Object> request,
@PostMapping("/apply-regenerated")
public RESTResult<Boolean> applyRegeneratedVersion(@RequestBody Map<String, Object> request,
@PostMapping("/approve-regenerated")
public RESTResult<Boolean> approveRegeneratedVersion(@RequestBody Map<String, Object> request,
```

### StylePresetController
```
@RequestMapping("/api/v1/product/style-preset")
@PostMapping("/list")
public RESTResult<List<StylePreset>> listEnabled(HttpServletRequest request,
@PostMapping("/list-all")
public RESTResult<List<StylePreset>> listAll(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<StylePreset> getById(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<StylePreset> save(HttpServletRequest request,
@DeleteMapping("/{id}")
public RESTResult<Void> delete(HttpServletRequest request,
```

## Entity 字段

### DyProduct
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "product_name", nullable = false, length = 256)
private String productName;
@Column(name = "product_category", length = 128)
private String productCategory;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "image_url", length = 256)
private String imageUrl;
@Column(name = "price", nullable = false, precision = 12, scale = 2)
private BigDecimal price;
@Column(name = "cost_price", precision = 12, scale = 2)
private BigDecimal costPrice;
@Column(name = "inventory")
private Long inventory = 0L;
@Column(name = "sku", length = 64)
private String sku;
@Column(name = "barcode", length = 128)
private String barcode;
@Column(name = "manufacturer", length = 256)
private String manufacturer;
@Column(name = "tags", length = 512)
private String tags;
@Column(name = "profit_margin_pct", precision = 5, scale = 4)
private BigDecimal profitMarginPct;
@Column(name = "loss_per_unit", precision = 10, scale = 2)
private BigDecimal lossPerUnit;
@Column(name = "control_strategy", length = 128)
private String controlStrategy;
@Column(name = "product_link", length = 512)
private String productLink;
@Column(name = "ai_selling_points", columnDefinition = "TEXT")
private String aiSellingPoints;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "featured", nullable = false)
private Integer featured = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "version", nullable = false)
private Integer version = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### DyProductSalesHistory
```
@Id
private Long id;
@Column(name = "product_id", nullable = false)
private Long productId;
@Column(name = "sale_quantity")
private Long saleQuantity = 0L;
@Column(name = "sale_amount", nullable = false, precision = 12, scale = 2)
private BigDecimal saleAmount;
@Column(name = "sale_time", nullable = false)
private Timestamp saleTime;
@Column(name = "channel_source", length = 64)
private String channelSource;
@Column(name = "session_id", length = 128)
private String sessionId;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### DyProductScript
```
@Id
private Long id;
@Column(name = "product_id")
private Long productId;
@Column(name = "is_emotional")
private Boolean isEmotional = false;
@Column(name = "script_type", nullable = false, length = 32)
private String scriptType;
@Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
private String scriptContent;
@Column(name = "persona_id")
private Long personaId;
@Column(name = "style", length = 32)
private String style;
@Column(name = "version")
private Integer version = 1;
@Column(name = "is_active")
private Boolean isActive = false;
@Column(name = "duration")
private Integer duration;
@Column(name = "token_usage")
private Integer tokenUsage;
@Column(name = "created_by")
private Long createdBy;
@Column(name = "source", length = 16)
private String source = "ai";
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### ProductScriptComparisonCache
```
private static final long serialVersionUID = 1L;
@Id
private Long id;
@Column(name = "product_id", nullable = false)
private Long productId;
@Column(name = "comparison_type", length = 64, nullable = false)
private String comparisonType;
@Column(name = "comparison_data", nullable = false)
private String comparisonData;
@Column(name = "cached_at", nullable = false)
private LocalDateTime cachedAt;
@Column(name = "ttl_minutes", nullable = false)
private Integer ttlMinutes;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted;
```

### ProductScriptEffectivenessRecord
```
private static final long serialVersionUID = 1L;
@Id
private Long id;
@Column(name = "product_id", nullable = false)
private Long productId;
@Column(name = "script_version_id", nullable = false)
private Long scriptVersionId;
@Column(name = "calculated_at", nullable = false)
private LocalDateTime calculatedAt;
@Column(name = "score_value", precision = 5, scale = 2, nullable = false)
private BigDecimal scoreValue;
@Column(name = "score_level", length = 10)
private String scoreLevel;
@Column(name = "usage_count_snapshot", nullable = false)
private Integer usageCountSnapshot;
@Column(name = "conversion_rate_snapshot", precision = 5, scale = 2)
private BigDecimal conversionRateSnapshot;
@Column(name = "likes_snapshot", nullable = false)
private Integer likesSnapshot;
@Column(name = "comments_snapshot", nullable = false)
private Integer commentsSnapshot;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted;
```

### ProductScriptSnapshot
```
private static final long serialVersionUID = 1L;
@Id
private Long id;
@Column(name = "live_session_id", nullable = false)
private Long liveSessionId;
@Column(name = "product_script_version_id", nullable = false)
private Long productScriptVersionId;
@Column(name = "content_snapshot", columnDefinition = "TEXT NOT NULL")
private String contentSnapshot;
@Column(name = "referenced_at")
private LocalDateTime referencedAt;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
@Column(name = "deleted", nullable = false)
private Integer deleted;
```

### ProductScriptUsage
```
@Id
private Long id;
@Column(name = "product_script_id", nullable = false)
private Long productScriptId;
@Column(name = "live_script_id")
private Long liveScriptId;
@Column(name = "session_id")
private Long sessionId;
@Column(name = "live_product_id")
private Long liveProductId;
@Column(name = "applied_time", nullable = false)
private Timestamp appliedTime;
@Column(name = "effectiveness_score", precision = 5, scale = 2)
private BigDecimal effectivenessScore;
@Column(name = "conversion_rate", precision = 5, scale = 2)
private BigDecimal conversionRate;
@Column(name = "sales_amount", precision = 12, scale = 2)
private BigDecimal salesAmount;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### ProductScriptVersion
```
private static final long serialVersionUID = 1L;
@Id
private Long id;
@Column(name = "product_id", nullable = false)
private Long productId;
@Column(name = "script_id")
private Long scriptId;
@Column(name = "version_number", nullable = false)
private Integer versionNumber;
@Column(name = "content", columnDefinition = "TEXT NOT NULL")
private String content;
@Column(name = "style", length = 64)
private String style;
@Column(name = "effectiveness_score", precision = 5, scale = 2)
private BigDecimal effectivenessScore;
@Column(name = "usage_count")
private Integer usageCount;
@Column(name = "conversion_rate", precision = 5, scale = 2)
private BigDecimal conversionRate;
@Column(name = "likes_count")
private Integer likesCount;
@Column(name = "comments_count")
private Integer commentsCount;
@Column(name = "is_active")
private Boolean isActive;
@Column(name = "is_recommended")
private Boolean isRecommended;
@Column(name = "archived")
private Boolean archived;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
@Column(name = "deleted_at")
private LocalDateTime deletedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted;
```

### ScriptAnalysisResult
```
private static final long serialVersionUID = 1L;
@Id
private Long id;
@Column(name = "script_version_id", nullable = false)
private Long scriptVersionId;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
private BigDecimal overallScore;
@Column(name = "interaction_rate", nullable = false, precision = 5, scale = 2)
private BigDecimal interactionRate;
@Column(name = "conversion_rate", nullable = false, precision = 5, scale = 2)
private BigDecimal conversionRate;
@Column(name = "fan_growth")
private Integer fanGrowth;
@Column(name = "comment_sentiment", precision = 3, scale = 2)
private BigDecimal commentSentiment;
@Column(name = "dominant_style", length = 64)
private String dominantStyle;
@Column(name = "weak_points", columnDefinition = "JSONB")
private String weakPoints;
@Column(name = "analysis_type", nullable = false, length = 64)
private String analysisType;
@Column(name = "data_source", nullable = false, length = 64)
private String dataSource;
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
@Column(name = "deleted_at")
private LocalDateTime deletedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted;
```

### ScriptOptimizationSuggestion
```
private static final long serialVersionUID = 1L;
@Id
private Long id;
@Column(name = "script_version_id", nullable = false)
private Long scriptVersionId;
@Column(name = "analysis_result_id", nullable = false)
private Long analysisResultId;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "category", nullable = false, length = 64)
private String category;
@Column(name = "priority", nullable = false, length = 20)
private String priority;
@Column(name = "suggestion_content", nullable = false, columnDefinition = "TEXT NOT NULL")
private String suggestionContent;
@Column(name = "related_weak_point", length = 256)
private String relatedWeakPoint;
@Column(name = "expected_improvement", columnDefinition = "JSONB")
private String expectedImprovement;
@Column(name = "adoption_status", length = 20)
private String adoptionStatus;
@Column(name = "adopted_at")
private LocalDateTime adoptedAt;
@Column(name = "adoption_notes", columnDefinition = "TEXT")
private String adoptionNotes;
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
@Column(name = "deleted_at")
private LocalDateTime deletedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted;
```

### ScriptRegeneratedVersion
```
private static final long serialVersionUID = 1L;
@Id
private Long id;
@Column(name = "script_version_id", nullable = false)
private Long scriptVersionId;
@Column(name = "suggestion_id", nullable = false)
private Long suggestionId;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "generation_style", nullable = false, length = 64)
private String generationStyle;
@Column(name = "regenerated_content", nullable = false, columnDefinition = "TEXT NOT NULL")
private String regeneratedContent;
@Column(name = "ai_quality_score", precision = 5, scale = 2)
private BigDecimal aiQualityScore;
@Column(name = "estimated_metrics", columnDefinition = "JSONB")
private String estimatedMetrics;
@Column(name = "generation_prompt", columnDefinition = "TEXT")
private String generationPrompt;
@Column(name = "is_applied")
private Boolean isApplied;
@Column(name = "applied_at")
private LocalDateTime appliedAt;
@Column(name = "approval_status", length = 20)
private String approvalStatus;
@Column(name = "approved_by")
private Long approvedBy;
@Column(name = "approved_at")
private LocalDateTime approvedAt;
@Column(name = "approval_notes", columnDefinition = "TEXT")
private String approvalNotes;
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
@Column(name = "deleted_at")
private LocalDateTime deletedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted;
```

### ScriptVersionHistory
```
@Id
private Long id;
@Column(name = "script_id", nullable = false)
private Long scriptId;
@Column(name = "product_id", nullable = false)
private Long productId;
@Column(name = "script_type", nullable = false, length = 32)
private String scriptType;
@Column(name = "style", length = 64)
private String style;
@Column(name = "version", nullable = false)
private Integer version;
@Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
private String scriptContent;
@Column(name = "persona_id")
private Long personaId;
@Column(name = "duration")
private Integer duration;
@Column(name = "created_by")
private Long createdBy;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
```

### StylePreset
```
@Id
private Long id;
@Column(name = "preset_name", nullable = false, length = 64)
private String presetName;
@Column(name = "preset_code", nullable = false, unique = true, length = 64)
private String presetCode;
@Column(name = "style_value", nullable = false, length = 128)
private String styleValue;
@Column(name = "style_tags_json", columnDefinition = "jsonb")
private String styleTagsJson;
@Column(name = "category", length = 32)
private String category;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "prompt_template", columnDefinition = "TEXT")
private String promptTemplate;
@Column(name = "word_count_min")
private Integer wordCountMin = 150;
@Column(name = "word_count_max")
private Integer wordCountMax = 300;
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

