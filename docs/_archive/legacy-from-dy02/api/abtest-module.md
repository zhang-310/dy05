# abtest 模块 API 文档

## 文件结构
```
controller/AbTestAutoConvergeScheduler.java
controller/AbTestController.java
entity/AbEvent.java
entity/AbExperiment.java
entity/AbVariant.java
repository/AbEventRepository.java
repository/AbExperimentRepository.java
repository/AbVariantRepository.java
service/AbTestService.java
service/ScriptStyleAbService.java
service/SessionTemplateAbService.java
service/impl/AbTestServiceImpl.java
service/impl/ScriptStyleAbServiceImpl.java
service/impl/SessionTemplateAbServiceImpl.java
vo/AbDailyTrendVO.java
vo/AbEventSaveVO.java
vo/AbExperimentSaveVO.java
vo/AbExperimentSearchVO.java
vo/AbExperimentStatisticsVO.java
vo/AbExperimentVO.java
vo/AbSetWinnerVO.java
vo/AbStatisticalTestVO.java
vo/AbVariantSaveVO.java
vo/AbVariantStatsVO.java
vo/AbVariantVO.java
vo/ScriptStyleAssignRequest.java
vo/ScriptStyleAssignVO.java
vo/ScriptStyleConversionRequest.java
vo/SessionTemplateAssignVO.java
```

## API 接口

### AbTestAutoConvergeScheduler
```
```

### AbTestController
```
@RequestMapping("/api/v1/abtest")
@PostMapping("/experiment/list")
public RESTResult<PageResultVO<AbExperimentVO>> list(HttpServletRequest request,
@PostMapping("/experiment/get")
public RESTResult<AbExperimentVO> get(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/experiment/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody AbExperimentSaveVO vo) {
@PostMapping("/experiment/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/experiment/update-status")
public RESTResult<Void> updateStatus(HttpServletRequest request,
@PostMapping("/experiment/set-winner")
public RESTResult<Void> setWinner(HttpServletRequest request, @Valid @RequestBody AbSetWinnerVO vo) {
@PostMapping("/variant/save")
public RESTResult<Long> saveVariant(HttpServletRequest request, @Valid @RequestBody AbVariantSaveVO vo) {
@PostMapping("/variant/delete")
public RESTResult<Void> deleteVariant(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/event/record")
public RESTResult<Void> recordEvent(HttpServletRequest request, @Valid @RequestBody AbEventSaveVO vo) {
@PostMapping("/experiment/result")
public RESTResult<AbExperimentStatisticsVO> getExperimentResult(HttpServletRequest request, @RequestParam Long experimentId) {
@PostMapping("/experiment/daily-trend")
public RESTResult<java.util.List<AbDailyTrendVO>> getDailyTrend(HttpServletRequest request,
@PostMapping("/script-style/assign")
public RESTResult<ScriptStyleAssignVO> assignStyle(HttpServletRequest request,
@PostMapping("/script-style/record-conversion")
public RESTResult<Void> recordConversion(HttpServletRequest request,
```

## Entity 字段

### AbEvent
```
@Id
private Long id;
@Column(name = "experiment_id", nullable = false)
private Long experimentId;
@Column(name = "variant_id", nullable = false)
private Long variantId;
@Column(name = "event_type", nullable = false, length = 16)
private String eventType;
@Column(name = "user_fingerprint", nullable = false, length = 64)
private String userFingerprint;
@Column(name = "session_id", length = 128)
private String sessionId;
@Column(name = "create_time", nullable = false)
private Timestamp createTime;
```

### AbExperiment
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "name", nullable = false, length = 128)
private String name;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "experiment_type", nullable = false, length = 16)
private String experimentType;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "start_time")
private Timestamp startTime;
@Column(name = "end_time")
private Timestamp endTime;
@Column(name = "winner_variant_id")
private Long winnerVariantId;
@Column(name = "target_entity_type", length = 32)
private String targetEntityType;
@Column(name = "target_entity_id")
private Long targetEntityId;
@Column(name = "conclusion", columnDefinition = "TEXT")
private String conclusion;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time", nullable = false)
private Timestamp createTime;
@Column(name = "update_time", nullable = false)
private Timestamp updateTime;
```

### AbVariant
```
@Id
private Long id;
@Column(name = "experiment_id", nullable = false)
private Long experimentId;
@Column(name = "variant_name", nullable = false, length = 64)
private String variantName;
@Column(name = "variant_type", nullable = false, length = 2)
private String variantType;
@Column(name = "content", columnDefinition = "TEXT")
private String content;
@Column(name = "entity_type", length = 32)
private String entityType;
@Column(name = "entity_id")
private Long entityId;
@Column(name = "style_code", length = 64)
private String styleCode;
@Column(name = "view_count", nullable = false)
private Long viewCount = 0L;
@Column(name = "click_count", nullable = false)
private Long clickCount = 0L;
@Column(name = "conversion_count", nullable = false)
private Long conversionCount = 0L;
@Column(name = "conversion_rate", nullable = false, precision = 5, scale = 4)
private BigDecimal conversionRate = BigDecimal.ZERO;
@Column(name = "is_winner", nullable = false)
private Integer isWinner = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time", nullable = false)
private Timestamp createTime;
@Column(name = "update_time", nullable = false)
private Timestamp updateTime;
```

