# system 模块 API 文档

## 文件结构
```
config/ApiCallLogCleanupScheduler.java
config/ApiCallLogInterceptor.java
config/ExternalApiHealthCheckScheduler.java
config/SystemRestTemplateConfig.java
controller/AlertController.java
controller/ExternalApiConfigController.java
controller/MetricsController.java
controller/MonitoringController.java
controller/SystemController.java
controller/SystemPerformanceController.java
controller/TaxonomyController.java
entity/ExternalApiCallLog.java
entity/ExternalApiConfig.java
entity/SysApiCallLog.java
entity/SysSyncLog.java
entity/SysTaxonomyNode.java
repository/ExternalApiCallLogRepository.java
repository/ExternalApiConfigRepository.java
repository/SysApiCallLogRepository.java
repository/SysSyncLogRepository.java
repository/SysTaxonomyNodeRepository.java
service/AlertEngineService.java
service/DashboardDataService.java
service/ExternalApiConfigService.java
service/ExternalApiGateway.java
service/MetricsCollectorService.java
service/SystemService.java
service/TaxonomyService.java
service/impl/AlertEngineServiceImpl.java
service/impl/DashboardDataServiceImpl.java
service/impl/ExternalApiConfigServiceImpl.java
service/impl/ExternalApiGatewayImpl.java
service/impl/MetricsCollectorServiceImpl.java
service/impl/SystemServiceImpl.java
service/impl/TaxonomyServiceImpl.java
vo/AlertRecordVO.java
vo/AlertRuleVO.java
vo/DashboardDataVO.java
vo/ExternalApiConfigSaveVO.java
vo/ExternalApiConfigSearchVO.java
vo/MetricsVO.java
```

## API 接口

### AlertController
```
@RequestMapping("/api/v1/system")
@PostMapping("/alert/rule/create")
public RESTResult<?> createAlertRule(@Valid @RequestBody AlertRuleVO vo) {
@PostMapping("/alert/rule/update")
public RESTResult<?> updateAlertRule(@RequestBody Map<String, Object> request) {
@PostMapping("/alert/rule/delete")
public RESTResult<?> deleteAlertRule(@RequestBody Map<String, Long> request) {
@PostMapping("/alert/rule/get")
public RESTResult<?> getAlertRule(@RequestBody Map<String, Long> request) {
@PostMapping("/alert/rule/list")
public RESTResult<?> listAlertRules(@RequestBody Map<String, Integer> request) {
@PostMapping("/alert/rule/enable")
public RESTResult<?> enableAlertRule(@RequestBody Map<String, Long> request) {
@PostMapping("/alert/rule/disable")
public RESTResult<?> disableAlertRule(@RequestBody Map<String, Long> request) {
@PostMapping("/alert/record/get")
public RESTResult<?> getAlertRecord(@RequestBody Map<String, Long> request) {
@PostMapping("/alert/record/list")
public RESTResult<?> listAlertRecords(@RequestBody Map<String, Integer> request) {
@GetMapping("/dashboard/overview")
public RESTResult<?> getSystemOverview() {
@GetMapping("/dashboard/alerts")
public RESTResult<?> getRealtimeAlerts() {
@GetMapping("/dashboard/performance")
public RESTResult<?> getPerformanceTrends() {
@GetMapping("/dashboard/logs")
public RESTResult<?> getLogStatistics() {
@GetMapping("/dashboard/traces")
public RESTResult<?> getTracesSummary() {
@GetMapping("/dashboard/health")
public RESTResult<?> getHealthStatus() {
```

### ExternalApiConfigController
```
@RequestMapping("/api/v1/system/external-api")
@PostMapping("/list")
public RESTResult<PageResultVO<ExternalApiConfig>> list(
@PostMapping("/get")
public RESTResult<ExternalApiConfig> get(
@PostMapping("/save")
public RESTResult<ExternalApiConfig> save(
@PostMapping("/delete")
public RESTResult<Void> delete(
@PostMapping("/health-status")
public RESTResult<Void> updateHealthStatus(
@PostMapping("/by-category")
public RESTResult<List<ExternalApiConfig>> byCategory(
```

### MetricsController
```
@RequestMapping("/api/v1/system/metrics")
@GetMapping("/prometheus")
@GetMapping("/all")
public RESTResult<?> getAllMetrics() {
@PostMapping("/get")
public RESTResult<?> getMetric(@RequestBody Map<String, String> request) {
@GetMapping("/cpu")
public RESTResult<?> getCpuMetrics() {
@GetMapping("/memory")
public RESTResult<?> getMemoryMetrics() {
@GetMapping("/disk")
public RESTResult<?> getDiskMetrics() {
@GetMapping("/jvm")
public RESTResult<?> getJvmMetrics() {
@GetMapping("/database")
public RESTResult<?> getDatabaseMetrics() {
```

### MonitoringController
```
@RequestMapping("/api/v1/monitoring")
@PostMapping("/metrics/realtime")
public RESTResult<Map<String, Object>> getRealtimeMetrics() {
@PostMapping("/metrics/historical")
public RESTResult<Map<String, Object>> getHistoricalMetrics(@RequestBody Map<String, Object> body) {
@PostMapping("/metrics/trend")
public RESTResult<Map<String, Object>> getPerformanceTrend(@RequestBody Map<String, Object> body) {
@PostMapping("/alert-rules/search")
public RESTResult<PageResultVO<AlertRuleVO>> searchAlertRules(@RequestBody Map<String, Object> body) {
@PostMapping("/alert-rules/detail")
public RESTResult<AlertRuleVO> getAlertRuleDetail(@RequestBody Map<String, Object> body) {
@PostMapping("/alert-rules/create")
public RESTResult<AlertRuleVO> createAlertRule(@RequestBody Map<String, Object> body) {
@PostMapping("/alert-rules/update")
public RESTResult<Void> updateAlertRule(@RequestBody Map<String, Object> body) {
@PostMapping("/alert-rules/delete")
public RESTResult<Void> deleteAlertRule(@RequestBody Map<String, Object> body) {
@PostMapping("/alert-rules/enable")
public RESTResult<Void> enableAlertRule(@RequestBody Map<String, Object> body) {
@PostMapping("/alert-rules/disable")
public RESTResult<Void> disableAlertRule(@RequestBody Map<String, Object> body) {
@PostMapping(value = "/alert-rules/export", produces = "text/csv;charset=UTF-8")
public org.springframework.http.ResponseEntity<byte[]> exportAlertRules(@RequestBody Map<String, Object> body) {
@PostMapping("/alert-rules/import")
public RESTResult<Map<String, Object>> importAlertRules(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/alerts/search")
public RESTResult<PageResultVO<AlertRecordVO>> searchAlerts(@RequestBody Map<String, Object> body) {
@PostMapping("/alerts/active")
public RESTResult<Map<String, Object>> getActiveAlerts(@RequestBody Map<String, Object> body) {
@PostMapping("/alerts/detail")
public RESTResult<AlertRecordVO> getAlertDetail(@RequestBody Map<String, Object> body) {
@PostMapping("/alerts/acknowledge")
public RESTResult<Void> acknowledgeAlert(@RequestBody Map<String, Object> body) {
@PostMapping("/alerts/resolve")
public RESTResult<Void> resolveAlert(@RequestBody Map<String, Object> body) {
@PostMapping("/alerts/close")
public RESTResult<Void> closeAlert(@RequestBody Map<String, Object> body) {
@PostMapping("/alerts/batch-acknowledge")
public RESTResult<Void> acknowledgeAlerts(@RequestBody Map<String, Object> body) {
@PostMapping("/alerts/statistics")
public RESTResult<Map<String, Object>> getAlertStatistics(@RequestBody Map<String, Object> body) {
@PostMapping("/logs/search")
public RESTResult<PageResultVO<Map<String, Object>>> searchLogs(@RequestBody Map<String, Object> body) {
@PostMapping("/logs/detail")
public RESTResult<Map<String, Object>> getLogDetail(@RequestBody Map<String, Object> body) {
@PostMapping(value = "/logs/export", produces = "text/csv;charset=UTF-8")
public org.springframework.http.ResponseEntity<byte[]> exportLogs(@RequestBody Map<String, Object> body) {
@PostMapping("/logs/statistics")
public RESTResult<Map<String, Object>> getLogStatistics(@RequestBody Map<String, Object> body) {
@PostMapping("/logs/error-aggregation")
public RESTResult<List<Map<String, Object>>> getErrorAggregation(@RequestBody Map<String, Object> body) {
@PostMapping("/logs/cleanup")
public RESTResult<Map<String, Object>> cleanupOldLogs(@RequestBody Map<String, Object> body) {
@PostMapping("/health/status")
public RESTResult<Map<String, Object>> getHealthStatus() {
@PostMapping("/health/component")
public RESTResult<Map<String, Object>> getComponentHealth(@RequestBody Map<String, Object> body) {
@PostMapping("/health/database")
public RESTResult<Map<String, Object>> getDatabasePoolStatus() {
@PostMapping("/health/cache")
public RESTResult<Map<String, Object>> getCacheStatus() {
@PostMapping("/dashboard/data")
public RESTResult<Map<String, Object>> getDashboardData(@RequestBody Map<String, Object> body) {
@PostMapping("/statistics")
public RESTResult<Map<String, Object>> getMonitoringStatistics() {
@PostMapping("/top-error-endpoints")
public RESTResult<List<Map<String, Object>>> getTopErrorEndpoints(@RequestBody Map<String, Object> body) {
@PostMapping("/top-slow-endpoints")
public RESTResult<List<Map<String, Object>>> getTopSlowEndpoints(@RequestBody Map<String, Object> body) {
@PostMapping("/system/resources")
public RESTResult<Map<String, Object>> getSystemResources() {
@GetMapping(value = "/stream/realtime", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@GetMapping(value = "/stream/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
```

### SystemController
```
@RequestMapping("/api/v1/system")
@PostMapping("/api-log/list")
public RESTResult<PageResultVO<Map<String, Object>>> apiLogList(
@PostMapping("/api-log/stats")
public RESTResult<Map<String, Object>> apiLogStats(
@PostMapping("/api-log/get")
public RESTResult<Map<String, Object>> apiLogGet(
@PostMapping("/sync-log/list")
public RESTResult<PageResultVO<Map<String, Object>>> syncLogList(
@PostMapping("/health")
public RESTResult<Map<String, Object>> health(HttpServletRequest request) {
@PostMapping("/info")
public RESTResult<Map<String, Object>> info(HttpServletRequest request) {
```

### SystemPerformanceController
```
@RequestMapping("/api/v1/system/performance")
@PostMapping("/metrics/current")
public RESTResult<Map<String, Object>> getMetricsCurrent(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/metrics/search")
public RESTResult<PageResultVO<Map<String, Object>>> getMetricsSearch(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/api/timeseries")
public RESTResult<List<Map<String, Object>>> getApiTimeseries(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/query/analysis")
public RESTResult<Map<String, Object>> getQueryAnalysis(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/query/slow")
public RESTResult<List<Map<String, Object>>> getSlowQueries(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/query/n-plus-one")
public RESTResult<List<Map<String, Object>>> getNPlusOneQueries(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/index/suggestions")
public RESTResult<List<Map<String, Object>>> getIndexSuggestions(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/cache/statistics")
public RESTResult<Map<String, Object>> getCacheStatistics(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/cache/hot-keys")
public RESTResult<List<Map<String, Object>>> getCacheHotKeys(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/cache/trend")
public RESTResult<List<Map<String, Object>>> getCacheTrend(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/cache/clear")
public RESTResult<Map<String, Object>> clearCache(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/cache/rebuild")
public RESTResult<Map<String, Object>> rebuildCache(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping(value = "/export", produces = "application/octet-stream")
public org.springframework.http.ResponseEntity<byte[]> exportPerformance(@RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/benchmark")
public RESTResult<Map<String, Object>> runBenchmark(@RequestBody(required = false) Map<String, Object> body) {
```

### TaxonomyController
```
@RequestMapping("/api/v1/system/taxonomy")
@PostMapping("/list")
public RESTResult<List<Map<String, Object>>> list(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
```

## Entity 字段

### ExternalApiCallLog
```
@Id
private Long id;
@Column(name = "provider_code", nullable = false, length = 64)
private String providerCode;
@Column(name = "endpoint", length = 512)
private String endpoint;
@Column(name = "method", length = 16)
private String method;
@Column(name = "request_summary", length = 512)
private String requestSummary;
@Column(name = "response_status")
private Integer responseStatus;
@Column(name = "latency_ms")
private Integer latencyMs;
@Column(name = "error_message", length = 1024)
private String errorMessage;
@Column(name = "caller_module", length = 64)
private String callerModule;
@Column(name = "caller_user_id")
private Long callerUserId;
@Column(name = "estimated_cost", precision = 12, scale = 6)
private BigDecimal estimatedCost;
@Column(name = "create_time")
private Timestamp createTime;
```

### ExternalApiConfig
```
@Id
private Long id;
@Column(name = "provider_code", nullable = false, length = 64)
private String providerCode;
@Column(name = "provider_name", nullable = false, length = 128)
private String providerName;
@Column(name = "category", length = 32)
private String category;
@Column(name = "base_url", length = 512)
private String baseUrl;
@Column(name = "api_key_encrypted", length = 512)
private String apiKeyEncrypted;
@Column(name = "api_secret_encrypted", length = 512)
private String apiSecretEncrypted;
@Column(name = "is_enabled", nullable = false)
private Boolean isEnabled = true;
@Column(name = "priority")
private Integer priority = 0;
@Column(name = "rate_limit_per_min")
private Integer rateLimitPerMin;
@Column(name = "daily_quota")
private Integer dailyQuota;
@Column(name = "monthly_quota")
private Integer monthlyQuota;
@Column(name = "last_health_check")
private Timestamp lastHealthCheck;
@Column(name = "health_status", length = 32)
private String healthStatus;
@Column(name = "avg_latency_ms")
private Integer avgLatencyMs;
@Column(name = "success_rate_pct")
private Float successRatePct;
@Column(name = "extra_config", columnDefinition = "jsonb")
private String extraConfig;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SysApiCallLog
```
@Id
private Long id;
@Column(name = "module", nullable = false, length = 64)
private String module;
@Column(name = "api_name", nullable = false, length = 256)
private String apiName;
@Column(name = "request_url", length = 512)
private String requestUrl;
@Column(name = "request_method", length = 16)
private String requestMethod;
@Column(name = "request_params", columnDefinition = "TEXT")
private String requestParams;
@Column(name = "response_status")
private Integer responseStatus;
@Column(name = "response_body", columnDefinition = "TEXT")
private String responseBody;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "error_message", length = 512)
private String errorMessage;
@Column(name = "duration_ms")
private Long durationMs;
@Column(name = "user_id")
private Long userId;
@Column(name = "create_time")
private Timestamp createTime;
```

### SysSyncLog
```
@Id
private Long id;
@Column(name = "sync_type", nullable = false, length = 64)
private String syncType;
@Column(name = "user_id")
private Long userId;
@Column(name = "account_id")
private Long accountId;
@Column(name = "status", nullable = false, length = 16)
private String status = "running";
@Column(name = "total_count")
private Integer totalCount = 0;
@Column(name = "success_count")
private Integer successCount = 0;
@Column(name = "fail_count")
private Integer failCount = 0;
@Column(name = "error_message", columnDefinition = "TEXT")
private String errorMessage;
@Column(name = "start_time")
private Timestamp startTime;
@Column(name = "end_time")
private Timestamp endTime;
@Column(name = "create_time")
private Timestamp createTime;
```

### SysTaxonomyNode
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId = 0L;
@Column(name = "module_scope", nullable = false, length = 64)
private String moduleScope;
@Column(name = "parent_id")
private Long parentId;
@Column(name = "code", nullable = false, length = 64)
private String code;
@Column(name = "name", nullable = false, length = 128)
private String name;
@Column(name = "sort_order", nullable = false)
private Integer sortOrder = 0;
@Column(name = "enabled", nullable = false)
private Integer enabled = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

