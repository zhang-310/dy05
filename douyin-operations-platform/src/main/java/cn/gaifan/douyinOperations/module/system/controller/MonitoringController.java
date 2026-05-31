package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.log.service.OperationLogService;
import cn.gaifan.douyinOperations.module.log.vo.OperationLogSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.OperationLogVO;
import cn.gaifan.douyinOperations.module.system.entity.ExternalApiConfig;
import cn.gaifan.douyinOperations.module.system.repository.ExternalApiConfigRepository;
import cn.gaifan.douyinOperations.module.system.service.AlertEngineService;
import cn.gaifan.douyinOperations.module.system.service.DashboardDataService;
import cn.gaifan.douyinOperations.module.system.vo.AlertRecordVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 监控模块 API（前后端对齐）
 * 代理 /system/alert 与 /system/dashboard，并提供指标/日志/健康等接口。
 * 已对接：/metrics/realtime（MeterRegistry + JVM）、告警规则/记录（DB）、日志（sys_operation_log）、/health/*（Actuator HealthContributorRegistry）。
 */
@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MonitoringController.class);
    private static final ScheduledExecutorService MONITORING_SSE_EXECUTOR = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "monitoring-sse");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicLong NET_LAST_RX_BYTES = new AtomicLong(-1);
    private static final AtomicLong NET_LAST_TX_BYTES = new AtomicLong(-1);
    private static final AtomicLong NET_LAST_NANOS = new AtomicLong(-1);

    @Resource
    private AlertEngineService alertEngineService;

    @Resource
    private DashboardDataService dashboardDataService;

    @Resource
    private OperationLogService operationLogService;

    @Resource
    private MeterRegistry meterRegistry;

    @Resource
    private HealthContributorRegistry healthContributorRegistry;

    @Resource
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired(required = false)
    private cn.gaifan.douyinOperations.common.metrics.PerformanceMetricsCollector performanceMetricsCollector;

    @Autowired(required = false)
    private ExternalApiConfigRepository externalApiConfigRepository;

    @Value("${spring.rabbitmq.host:${RABBITMQ_HOST:localhost}}")
    private String rabbitHost;

    @Value("${app.monitoring.rabbitmq.management-port:${RABBITMQ_MGMT_PORT:15672}}")
    private int rabbitManagementPort;

    @Value("${spring.rabbitmq.username:${RABBITMQ_USER:guest}}")
    private String rabbitUser;

    @Value("${spring.rabbitmq.password:${RABBITMQ_PASSWORD:guest}}")
    private String rabbitPassword;

    // ─── 实时指标（对接 Actuator/MeterRegistry + JVM）──────────────────────

    @PostMapping("/metrics/realtime")
    public RESTResult<Map<String, Object>> getRealtimeMetrics() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("degraded", false);
        data.put("source", "micrometer+jvm+request-window");

        double cpuUsage = 0;
        try {
            for (var g : meterRegistry.find("process.cpu.usage").gauges()) {
                double v = g.value();
                if (!Double.isNaN(v) && v >= 0) { cpuUsage = v * 100; break; }
            }
        } catch (Exception ignored) { /* fallback to 0 */ }
        if (cpuUsage == 0) {
            try {
                OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
                if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                    double v = sunOs.getProcessCpuLoad() * 100;
                    if (v >= 0 && !Double.isNaN(v)) cpuUsage = v;
                }
            } catch (Exception ignored2) { /* fallback to 0 */ }
        }
        data.put("cpuUsage", Math.round(cpuUsage * 100) / 100.0);

        double memoryUsage = 0;
        try {
            MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
            long used = mem.getHeapMemoryUsage().getUsed();
            long max = mem.getHeapMemoryUsage().getMax();
            if (max > 0) memoryUsage = (double) used / max * 100;
        } catch (Exception ignored) { /* fallback to 0 */ }
        data.put("memoryUsage", Math.round(memoryUsage * 100) / 100.0);

        double diskUsage = 0;
        try {
            java.io.File root = new java.io.File("/");
            long total = root.getTotalSpace();
            if (total > 0) {
                diskUsage = (double) (total - root.getFreeSpace()) / total * 100;
            }
        } catch (Exception ignored) { /* fallback to 0 */ }
        data.put("diskUsage", Math.round(diskUsage * 100) / 100.0);

        long requestCount = 0;
        try {
            for (var t : meterRegistry.find("http.server.requests").timers()) {
                requestCount += (long) t.count();
            }
        } catch (Exception ignored) { /* Micrometer 可能未注册该指标 */ }
        data.put("requestCount", requestCount);
        if (performanceMetricsCollector != null) {
            data.putAll(performanceMetricsCollector.getWindowSnapshot());
        }
        addNetworkMetrics(data);
        addDatabaseConnectionMetric(data);
        addRedisRuntimeMetrics(data);
        addRabbitQueueDepthMetric(data);

        return RESTResult.success(data);
    }

    @PostMapping("/metrics/historical")
    public RESTResult<Map<String, Object>> getHistoricalMetrics(@RequestBody Map<String, Object> body) {
        return RESTResult.success(Map.of(
                "data", List.of(),
                "degraded", true,
                "source", "not_configured",
                "fallbackReason", "历史指标需接入 Prometheus 或时序库"));
    }

    @PostMapping("/metrics/trend")
    public RESTResult<Map<String, Object>> getPerformanceTrend(@RequestBody Map<String, Object> body) {
        String metricName = body != null && body.get("metricName") instanceof String s ? s : "responseTime";
        String timeRange = body != null && body.get("timeRange") instanceof String s ? s : "hour";
        int dataPoints = body != null && body.get("dataPoints") instanceof Number n ? n.intValue() : 60;
        if (performanceMetricsCollector == null) {
            return RESTResult.success(Map.of(
                    "metricName", metricName,
                    "unit", "errorRate".equals(metricName) ? "%" : "ms",
                    "timeRange", timeRange,
                    "dataPoints", List.of(),
                    "summary", Map.of("average", 0, "min", 0, "max", 0, "percentile95", 0, "percentile99", 0),
                    "degraded", true,
                    "source", "not_configured",
                    "fallbackReason", "请求性能聚合器未注册"));
        }
        return RESTResult.success(performanceMetricsCollector.getTrend(metricName, timeRange, dataPoints));
    }

    // ─── 告警规则（委托 system/alert/rule）────────────────────────────────

    @PostMapping("/alert-rules/search")
    public RESTResult<PageResultVO<AlertRuleVO>> searchAlertRules(@RequestBody Map<String, Object> body) {
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int rows = body.get("rows") != null ? ((Number) body.get("rows")).intValue() : 30;
        PageResultVO<AlertRuleVO> result = alertEngineService.listAlertRules(page, rows);
        return RESTResult.success(result);
    }

    @PostMapping("/alert-rules/detail")
    public RESTResult<AlertRuleVO> getAlertRuleDetail(@RequestBody Map<String, Object> body) {
        Long ruleId = body.get("ruleId") != null ? ((Number) body.get("ruleId")).longValue() : null;
        if (ruleId == null) return RESTResult.fail(400, "ruleId 必填");
        AlertRuleVO rule = alertEngineService.getAlertRule(ruleId);
        return rule != null ? RESTResult.success(rule) : RESTResult.fail(404, "规则不存在");
    }

    @PostMapping("/alert-rules/create")
    public RESTResult<AlertRuleVO> createAlertRule(@RequestBody Map<String, Object> body) {
        AlertRuleVO vo = toAlertRuleVO(body);
        long id = alertEngineService.createAlertRule(vo);
        vo.setId(id);
        return RESTResult.success(vo);
    }

    @PostMapping("/alert-rules/update")
    public RESTResult<Void> updateAlertRule(@RequestBody Map<String, Object> body) {
        Long ruleId = body.get("ruleId") != null ? ((Number) body.get("ruleId")).longValue() : body.get("id") != null ? ((Number) body.get("id")).longValue() : null;
        if (ruleId == null) return RESTResult.fail(400, "ruleId 或 id 必填");
        alertEngineService.updateAlertRule(ruleId, toAlertRuleVO(body));
        return RESTResult.success();
    }

    @PostMapping("/alert-rules/delete")
    public RESTResult<Void> deleteAlertRule(@RequestBody Map<String, Object> body) {
        Long ruleId = body.get("ruleId") != null ? ((Number) body.get("ruleId")).longValue() : null;
        if (ruleId == null) return RESTResult.fail(400, "ruleId 必填");
        alertEngineService.deleteAlertRule(ruleId);
        return RESTResult.success();
    }

    @PostMapping("/alert-rules/enable")
    public RESTResult<Void> enableAlertRule(@RequestBody Map<String, Object> body) {
        Long ruleId = body.get("ruleId") != null ? ((Number) body.get("ruleId")).longValue() : null;
        if (ruleId == null) return RESTResult.fail(400, "ruleId 必填");
        alertEngineService.enableAlertRule(ruleId);
        return RESTResult.success();
    }

    @PostMapping("/alert-rules/disable")
    public RESTResult<Void> disableAlertRule(@RequestBody Map<String, Object> body) {
        Long ruleId = body.get("ruleId") != null ? ((Number) body.get("ruleId")).longValue() : null;
        if (ruleId == null) return RESTResult.fail(400, "ruleId 必填");
        alertEngineService.disableAlertRule(ruleId);
        return RESTResult.success();
    }

    @PostMapping(value = "/alert-rules/export", produces = "text/csv;charset=UTF-8")
    public org.springframework.http.ResponseEntity<byte[]> exportAlertRules(@RequestBody Map<String, Object> body) {
        byte[] csv = "id,name,metricName,enabled\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=alert-rules.csv")
                .body(csv);
    }

    @PostMapping("/alert-rules/import")
    public RESTResult<Map<String, Object>> importAlertRules(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(Map.of("importedCount", 0, "failedCount", 0));
    }

    // ─── 告警记录（委托 system/alert/record）────────────────────────────

    @PostMapping("/alerts/search")
    public RESTResult<PageResultVO<AlertRecordVO>> searchAlerts(@RequestBody Map<String, Object> body) {
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int rows = body.get("rows") != null ? ((Number) body.get("rows")).intValue() : 30;
        PageResultVO<AlertRecordVO> result = alertEngineService.listAlertRecords(page, rows);
        return RESTResult.success(result);
    }

    @PostMapping("/alerts/active")
    public RESTResult<Map<String, Object>> getActiveAlerts(@RequestBody Map<String, Object> body) {
        int limit = body.get("limit") != null ? ((Number) body.get("limit")).intValue() : 100;
        PageResultVO<AlertRecordVO> result = alertEngineService.listAlertRecords(0, limit);
        List<AlertRecordVO> list = result.getList() != null ? result.getList() : List.of();
        return RESTResult.success(Map.of("alerts", list, "total", list.size()));
    }

    @PostMapping("/alerts/detail")
    public RESTResult<AlertRecordVO> getAlertDetail(@RequestBody Map<String, Object> body) {
        Long alertId = body.get("alertId") != null ? ((Number) body.get("alertId")).longValue() : null;
        if (alertId == null) return RESTResult.fail(400, "alertId 必填");
        AlertRecordVO record = alertEngineService.getAlertRecord(alertId);
        return record != null ? RESTResult.success(record) : RESTResult.fail(404, "记录不存在");
    }

    @PostMapping("/alerts/acknowledge")
    public RESTResult<Void> acknowledgeAlert(@RequestBody Map<String, Object> body) {
        Long alertId = readAlertId(body);
        if (alertId == null) return RESTResult.fail(400, "alertId 必填");
        alertEngineService.acknowledgeAlertRecord(alertId);
        return RESTResult.success();
    }

    @PostMapping("/alerts/resolve")
    public RESTResult<Void> resolveAlert(@RequestBody Map<String, Object> body) {
        Long alertId = readAlertId(body);
        if (alertId == null) return RESTResult.fail(400, "alertId 必填");
        alertEngineService.resolveAlertRecord(alertId);
        return RESTResult.success();
    }

    @PostMapping("/alerts/close")
    public RESTResult<Void> closeAlert(@RequestBody Map<String, Object> body) {
        Long alertId = readAlertId(body);
        if (alertId == null) return RESTResult.fail(400, "alertId 必填");
        alertEngineService.closeAlertRecord(alertId);
        return RESTResult.success();
    }

    @PostMapping("/alerts/batch-acknowledge")
    public RESTResult<Void> acknowledgeAlerts(@RequestBody Map<String, Object> body) {
        Object rawIds = body != null ? body.get("alertIds") : null;
        if (!(rawIds instanceof List<?> ids) || ids.isEmpty()) {
            return RESTResult.fail(400, "alertIds 必填");
        }
        for (Object id : ids) {
            if (id instanceof Number n) {
                alertEngineService.acknowledgeAlertRecord(n.longValue());
            }
        }
        return RESTResult.success();
    }

    @PostMapping("/alerts/statistics")
    public RESTResult<Map<String, Object>> getAlertStatistics(@RequestBody Map<String, Object> body) {
        return RESTResult.success(Map.of("alerts", dashboardDataService.getRealtimeAlerts(), "degraded", false, "source", "sys_alert_record"));
    }

    // ─── 日志（sys_operation_log）──────────────────────────────────────

    @PostMapping("/logs/search")
    public RESTResult<PageResultVO<OperationLogVO>> searchLogs(@RequestBody Map<String, Object> body) {
        return RESTResult.success(operationLogService.search(toOperationLogSearchVO(body)));
    }

    @PostMapping("/logs/detail")
    public RESTResult<Map<String, Object>> getLogDetail(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("logId") == null) {
            return RESTResult.fail(400, "logId 必填");
        }
        OperationLogSearchVO query = toOperationLogSearchVO(body);
        query.setPage(0);
        query.setRows(1000);
        Long logId = ((Number) body.get("logId")).longValue();
        OperationLogVO matched = operationLogService.search(query).getList().stream()
                .filter(item -> logId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        return matched != null ? RESTResult.success(Map.of("log", matched, "degraded", false, "source", "sys_operation_log"))
                : RESTResult.fail(404, "日志不存在");
    }

    @PostMapping(value = "/logs/export", produces = "text/csv;charset=UTF-8")
    public org.springframework.http.ResponseEntity<byte[]> exportLogs(@RequestBody Map<String, Object> body) {
        OperationLogSearchVO query = toOperationLogSearchVO(body);
        query.setRows(Math.min(Math.max(query.getRows(), 1), 1000));
        PageResultVO<OperationLogVO> page = operationLogService.search(query);
        StringBuilder csv = new StringBuilder("id,time,module,action,status,durationMs,uri\n");
        for (OperationLogVO item : page.getList()) {
            csv.append(item.getId()).append(',')
                    .append(item.getCreateTime()).append(',')
                    .append(safeCsv(item.getModule())).append(',')
                    .append(safeCsv(item.getAction())).append(',')
                    .append(item.getStatus()).append(',')
                    .append(item.getDurationMs()).append(',')
                    .append(safeCsv(item.getRequestUri())).append('\n');
        }
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=logs.csv")
                .body(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @PostMapping("/logs/statistics")
    public RESTResult<Map<String, Object>> getLogStatistics(@RequestBody Map<String, Object> body) {
        return RESTResult.success(Map.of("statistics", dashboardDataService.getLogStatistics(), "degraded", false, "source", "sys_operation_log"));
    }

    @PostMapping("/logs/error-aggregation")
    public RESTResult<List<Map<String, Object>>> getErrorAggregation(@RequestBody Map<String, Object> body) {
        return RESTResult.success(List.of(Map.of(
                "degraded", true,
                "source", "sys_operation_log",
                "fallbackReason", "错误聚合需要按 error_msg 做数据库聚合，当前仅支持日志列表查询")));
    }

    @PostMapping("/logs/cleanup")
    public RESTResult<Map<String, Object>> cleanupOldLogs(@RequestBody Map<String, Object> body) {
        return RESTResult.success(Map.of(
                "deletedCount", 0,
                "degraded", true,
                "source", "scheduler",
                "fallbackReason", "日志清理由专用定时任务负责，接口不做在线删除"));
    }

    // ─── 健康检查（委托 dashboard）──────────────────────────────────────

    @PostMapping("/health/status")
    public RESTResult<Map<String, Object>> getHealthStatus() {
        try {
            Object overview = dashboardDataService.getSystemOverview();
            Map<String, Object> components = new LinkedHashMap<>();
            components.put("database", databaseComponent());
            components.put("cache", cacheComponent());
            components.put("messageQueue", actuatorComponent("rabbit"));
            components.put("elasticsearch", actuatorComponent("elasticsearch"));
            components.put("diskSpace", actuatorComponent("diskSpace"));
            components.put("external", externalApiComponent());

            String status = aggregateStatus(components);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", status);
            result.put("statusCode", "UP".equals(status) ? 200 : "DEGRADED".equals(status) ? 206 : 503);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("message", "UP".equals(status) ? "所有核心组件可用" : "存在降级或不可用组件");
            result.put("components", components);
            if (overview != null) {
                result.put("overview", overview);
            }
            return RESTResult.success(result);
        } catch (Exception e) {
            return RESTResult.success(Map.of("status", "DOWN", "message", e.getMessage()));
        }
    }

    @PostMapping("/health/component")
    public RESTResult<Map<String, Object>> getComponentHealth(@RequestBody Map<String, Object> body) {
        String name = body != null && body.get("name") instanceof String s ? s : null;
        return RESTResult.success(getHealthFromActuator(name != null ? name : "diskSpace"));
    }

    @PostMapping("/health/database")
    public RESTResult<Map<String, Object>> getDatabasePoolStatus() {
        Map<String, Object> out = getHealthFromActuator("db");
        if (Boolean.TRUE.equals(out.get("degraded")) && dataSource != null) {
            try (Connection c = dataSource.getConnection()) {
                out.put("status", "UP");
                out.put("degraded", false);
                out.put("source", "datasource");
            } catch (Exception e) {
                out.put("status", "DOWN");
                out.put("message", e.getMessage());
                out.put("degraded", false);
                out.put("source", "datasource");
            }
        }
        return RESTResult.success(out);
    }

    @PostMapping("/health/cache")
    public RESTResult<Map<String, Object>> getCacheStatus() {
        Map<String, Object> out = getHealthFromActuator("redis");
        if (Boolean.TRUE.equals(out.get("degraded")) && redisConnectionFactory != null) {
            try {
                redisConnectionFactory.getConnection().ping();
                out.put("status", "UP");
                out.put("degraded", false);
                out.put("source", "redisConnectionFactory");
            } catch (Exception e) {
                out.put("status", "DOWN");
                out.put("message", e.getMessage());
                out.put("degraded", false);
                out.put("source", "redisConnectionFactory");
            }
        }
        return RESTResult.success(out);
    }

    private Map<String, Object> getHealthFromActuator(String contributorName) {
        Map<String, Object> out = new HashMap<>();
        boolean fromActuator = false;
        try {
            var contributor = healthContributorRegistry.getContributor(contributorName);
            if (contributor instanceof HealthIndicator indicator) {
                Health health = indicator.health();
                out.put("status", health.getStatus().getCode());
                if (health.getDetails() != null && !health.getDetails().isEmpty()) {
                    out.put("details", health.getDetails());
                }
                fromActuator = true;
            }
        } catch (Exception ignored) { /* 无该 contributor 时返回空 */ }
        if (!out.containsKey("status")) out.put("status", "UNKNOWN");
        out.put("degraded", !fromActuator);
        out.put("source", fromActuator ? "actuator" : "not_configured");
        if (!fromActuator) {
            out.put("fallbackReason", "Actuator health contributor 未注册: " + contributorName);
        }
        return out;
    }

    private Map<String, Object> databaseComponent() {
        long start = System.currentTimeMillis();
        Map<String, Object> out = getHealthFromActuator("db");
        if (Boolean.TRUE.equals(out.get("degraded")) && dataSource != null) {
            try (Connection ignored = dataSource.getConnection()) {
                out.put("status", "UP");
                out.put("degraded", false);
                out.put("source", "datasource");
            } catch (Exception e) {
                out.put("status", "DOWN");
                out.put("message", e.getMessage());
                out.put("degraded", false);
                out.put("source", "datasource");
            }
        }
        out.put("responseTime", System.currentTimeMillis() - start);
        out.put("lastCheck", LocalDateTime.now().toString());
        out.putIfAbsent("message", "UP".equals(out.get("status")) ? "数据库连接正常" : "数据库健康状态异常");
        return out;
    }

    private Map<String, Object> cacheComponent() {
        long start = System.currentTimeMillis();
        Map<String, Object> out = getHealthFromActuator("redis");
        if (Boolean.TRUE.equals(out.get("degraded")) && redisConnectionFactory != null) {
            try {
                redisConnectionFactory.getConnection().ping();
                out.put("status", "UP");
                out.put("degraded", false);
                out.put("source", "redisConnectionFactory");
            } catch (Exception e) {
                out.put("status", "DOWN");
                out.put("message", e.getMessage());
                out.put("degraded", false);
                out.put("source", "redisConnectionFactory");
            }
        }
        out.put("responseTime", System.currentTimeMillis() - start);
        out.put("lastCheck", LocalDateTime.now().toString());
        out.putIfAbsent("message", "UP".equals(out.get("status")) ? "Redis 连接正常" : "Redis 健康状态异常");
        return out;
    }

    private Map<String, Object> actuatorComponent(String contributorName) {
        long start = System.currentTimeMillis();
        Map<String, Object> out = getHealthFromActuator(contributorName);
        out.put("responseTime", System.currentTimeMillis() - start);
        out.put("lastCheck", LocalDateTime.now().toString());
        out.putIfAbsent("message", Boolean.TRUE.equals(out.get("degraded"))
                ? "Actuator 未注册 " + contributorName
                : contributorName + " 健康状态已返回");
        return out;
    }

    private Map<String, Object> externalApiComponent() {
        Map<String, Object> out = new LinkedHashMap<>();
        if (externalApiConfigRepository == null) {
            out.put("status", "UNKNOWN");
            out.put("message", "外部 API 配置仓库未注册");
            out.put("lastCheck", LocalDateTime.now().toString());
            return out;
        }
        List<ExternalApiConfig> configs = externalApiConfigRepository.findByIsEnabledAndDeleted(true, 0);
        long healthy = configs.stream().filter(config -> "healthy".equalsIgnoreCase(config.getHealthStatus())).count();
        long down = configs.stream().filter(config -> "down".equalsIgnoreCase(config.getHealthStatus())).count();
        long degraded = configs.stream().filter(config -> {
            String status = config.getHealthStatus();
            return status == null || status.isBlank() || "unknown".equalsIgnoreCase(status) || "degraded".equalsIgnoreCase(status);
        }).count();
        String status = down > 0 ? "DOWN" : degraded > 0 ? "DEGRADED" : "UP";
        out.put("status", status);
        out.put("lastCheck", LocalDateTime.now().toString());
        out.put("message", "外部 API 健康检查: healthy=" + healthy + ", degraded=" + degraded + ", down=" + down);
        out.put("details", Map.of("total", configs.size(), "healthy", healthy, "degraded", degraded, "down", down));
        return out;
    }

    private void addDatabaseConnectionMetric(Map<String, Object> data) {
        try {
            double active = firstGaugeValue("hikaricp.connections.active", "hikaricp_connections_active");
            double max = firstGaugeValue("hikaricp.connections.max", "hikaricp_connections_max");
            if (active >= 0) {
                data.put("databaseConnections", Math.round(active));
                if (max >= 0) {
                    data.put("databaseConnectionsMax", Math.round(max));
                }
                return;
            }
        } catch (Exception ignored) { /* 继续尝试从 DataSource 读取 */ }

        if (dataSource == null) return;
        try {
            Object poolBean = dataSource;
            Method getHikariPoolMXBean = poolBean.getClass().getMethod("getHikariPoolMXBean");
            Object poolMxBean = getHikariPoolMXBean.invoke(poolBean);
            if (poolMxBean != null) {
                Method getActiveConnections = poolMxBean.getClass().getMethod("getActiveConnections");
                Object active = getActiveConnections.invoke(poolMxBean);
                if (active instanceof Number n) {
                    data.put("databaseConnections", n.intValue());
                }
            }
        } catch (Exception ignored) {
            // 非 Hikari DataSource 时不填，避免误造指标。
        }
    }

    private void addNetworkMetrics(Map<String, Object> data) {
        try {
            List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Path.of("/proc/net/dev"));
            long rxBytes = 0;
            long txBytes = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.contains(":")) continue;
                String iface = trimmed.substring(0, trimmed.indexOf(':')).trim();
                if ("lo".equals(iface)) continue;
                String[] parts = trimmed.substring(trimmed.indexOf(':') + 1).trim().split("\\s+");
                if (parts.length >= 16) {
                    rxBytes += parseLong(parts[0]);
                    txBytes += parseLong(parts[8]);
                }
            }
            long now = System.nanoTime();
            long prevRx = NET_LAST_RX_BYTES.getAndSet(rxBytes);
            long prevTx = NET_LAST_TX_BYTES.getAndSet(txBytes);
            long prevNanos = NET_LAST_NANOS.getAndSet(now);
            if (prevRx >= 0 && prevTx >= 0 && prevNanos > 0 && now > prevNanos) {
                double seconds = (now - prevNanos) / 1_000_000_000.0;
                data.put("networkIn", round2(((rxBytes - prevRx) * 8.0 / seconds) / 1_000_000.0));
                data.put("networkOut", round2(((txBytes - prevTx) * 8.0 / seconds) / 1_000_000.0));
            }
        } catch (Exception ignored) {
            // 非 Linux 环境或 /proc 不可读时不填。
        }
    }

    private void addRedisRuntimeMetrics(Map<String, Object> data) {
        if (redisConnectionFactory == null) return;
        try {
            var connection = redisConnectionFactory.getConnection();
            try {
                var info = connection.serverCommands().info("stats");
                long hits = parseLong(info != null ? info.getProperty("keyspace_hits") : null);
                long misses = parseLong(info != null ? info.getProperty("keyspace_misses") : null);
                long total = hits + misses;
                if (total > 0) {
                    data.put("cacheHitRate", Math.round(((double) hits / total * 100) * 100) / 100.0);
                    data.put("cacheHits", hits);
                    data.put("cacheMisses", misses);
                }
            } finally {
                connection.close();
            }
        } catch (Exception ignored) {
            // Redis INFO 不可用时不填，页面会明确显示未返回。
        }
    }

    private void addRabbitQueueDepthMetric(Map<String, Object> data) {
        double ready = sumGauges("rabbitmq.queue.messages.ready", "rabbitmq_queue_messages_ready");
        double unacked = sumGauges("rabbitmq.queue.messages.unacked", "rabbitmq_queue_messages_unacked");
        if (ready >= 0 || unacked >= 0) {
            data.put("queueDepth", Math.round(Math.max(ready, 0) + Math.max(unacked, 0)));
            return;
        }

        Map<String, Object> rabbit = getHealthFromActuator("rabbit");
        Object details = rabbit.get("details");
        if (details instanceof Map<?, ?> detailMap) {
            Object queues = detailMap.get("queues");
            if (queues instanceof Map<?, ?> queueMap) {
                long total = 0;
                boolean found = false;
                for (Object value : queueMap.values()) {
                    if (value instanceof Map<?, ?> q) {
                        total += parseLong(q.get("messageCount"));
                        total += parseLong(q.get("messages"));
                        found = true;
                    }
                }
                if (found) {
                    data.put("queueDepth", total);
                }
            }
        }
        if (!data.containsKey("queueDepth")) {
            addRabbitManagementQueueDepth(data);
        }
    }

    private void addRabbitManagementQueueDepth(Map<String, Object> data) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(2))
                    .build();
            String credentials = Base64.getEncoder().encodeToString((rabbitUser + ":" + rabbitPassword).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + rabbitHost + ":" + rabbitManagementPort + "/api/queues"))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .header("Authorization", "Basic " + credentials)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return;
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) return;
            long total = 0;
            for (JsonNode queue : root) {
                total += queue.path("messages").asLong(0);
            }
            data.put("queueDepth", total);
            data.put("queueDepthSource", "rabbitmq-management");
        } catch (Exception ignored) {
            // RabbitMQ management API 未开放或鉴权失败时不填。
        }
    }

    private double firstGaugeValue(String... names) {
        for (String name : names) {
            for (var gauge : meterRegistry.find(name).gauges()) {
                double value = gauge.value();
                if (!Double.isNaN(value) && value >= 0) return value;
            }
        }
        return -1;
    }

    private double sumGauges(String... names) {
        double sum = 0;
        boolean found = false;
        for (String name : names) {
            for (var gauge : meterRegistry.find(name).gauges()) {
                double value = gauge.value();
                if (!Double.isNaN(value) && value >= 0) {
                    sum += value;
                    found = true;
                }
            }
        }
        return found ? sum : -1;
    }

    private long parseLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static String aggregateStatus(Map<String, Object> components) {
        boolean degraded = false;
        for (Object value : components.values()) {
            if (!(value instanceof Map<?, ?> detail)) {
                degraded = true;
                continue;
            }
            Object status = detail.get("status");
            if ("DOWN".equals(status)) {
                return "DOWN";
            }
            if (!"UP".equals(status)) {
                degraded = true;
            }
        }
        return degraded ? "DEGRADED" : "UP";
    }

    // ─── 仪表板（委托 dashboard）────────────────────────────────────────

    @PostMapping("/dashboard/data")
    public RESTResult<Map<String, Object>> getDashboardData(@RequestBody Map<String, Object> body) {
        try {
            Object overview = dashboardDataService.getSystemOverview();
            Object alerts = dashboardDataService.getRealtimeAlerts();
            return RESTResult.success(Map.of("overview", overview != null ? overview : Map.of(), "alerts", alerts != null ? alerts : List.of()));
        } catch (Exception e) {
            return RESTResult.success(Map.of());
        }
    }

    @PostMapping("/statistics")
    public RESTResult<Map<String, Object>> getMonitoringStatistics() {
        return RESTResult.success(Map.of(
                "alerts", dashboardDataService.getRealtimeAlerts(),
                "logs", dashboardDataService.getLogStatistics(),
                "degraded", false,
                "source", "platform-db"
        ));
    }

    @PostMapping("/top-error-endpoints")
    public RESTResult<List<Map<String, Object>>> getTopErrorEndpoints(@RequestBody Map<String, Object> body) {
        int limit = body != null && body.get("limit") != null
                ? ((Number) body.get("limit")).intValue() : 10;
        List<Map<String, Object>> list = performanceMetricsCollector != null
                ? performanceMetricsCollector.getTopError(Math.max(1, Math.min(limit, 100)))
                : List.of();
        return RESTResult.success(list);
    }

    @PostMapping("/top-slow-endpoints")
    public RESTResult<List<Map<String, Object>>> getTopSlowEndpoints(@RequestBody Map<String, Object> body) {
        int limit = body != null && body.get("limit") != null
                ? ((Number) body.get("limit")).intValue() : 10;
        List<Map<String, Object>> list = performanceMetricsCollector != null
                ? performanceMetricsCollector.getTopSlow(Math.max(1, Math.min(limit, 100)))
                : List.of();
        return RESTResult.success(list);
    }

    @PostMapping("/system/resources")
    public RESTResult<Map<String, Object>> getSystemResources() {
        return getRealtimeMetrics();
    }

    // ─── SSE 流───────────────────────────────────────────────────────

    @GetMapping(value = "/stream/realtime", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRealtime() {
        SseEmitter emitter = createMonitoringEmitter("monitoring-realtime");
        startSsePump(emitter, () -> {
            sendSseEvent(emitter, "metrics", getRealtimeMetrics().getData());
            sendSseEvent(emitter, "health", getHealthStatus().getData());
        });
        sendSseEvent(emitter, "connected", "ok");
        sendSseEvent(emitter, "metrics", getRealtimeMetrics().getData());
        sendSseEvent(emitter, "health", getHealthStatus().getData());
        return emitter;
    }

    @GetMapping(value = "/stream/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAlerts() {
        SseEmitter emitter = createMonitoringEmitter("monitoring-alerts");
        startSsePump(emitter, () -> sendSseEvent(emitter, "heartbeat", Map.of("timestamp", System.currentTimeMillis())));
        sendSseEvent(emitter, "connected", "ok");
        return emitter;
    }

    private SseEmitter createMonitoringEmitter(String streamName) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> log.debug("SSE completed: {}", streamName));
        emitter.onTimeout(() -> log.debug("SSE timeout: {}", streamName));
        emitter.onError(error -> log.debug("SSE error: {}, {}", streamName, error.getMessage()));
        return emitter;
    }

    private void startSsePump(SseEmitter emitter, Runnable tick) {
        var closed = new java.util.concurrent.atomic.AtomicBoolean(false);
        var future = MONITORING_SSE_EXECUTOR.scheduleAtFixedRate(() -> {
            if (closed.get()) return;
            try {
                tick.run();
                emitter.send(SseEmitter.event().name("heartbeat").data(Map.of("timestamp", System.currentTimeMillis())));
            } catch (Exception e) {
                closed.set(true);
                emitter.completeWithError(e);
            }
        }, 5, 5, TimeUnit.SECONDS);
        Runnable cleanup = () -> {
            closed.set(true);
            future.cancel(true);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
    }

    private void sendSseEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private static AlertRuleVO toAlertRuleVO(Map<String, Object> m) {
        AlertRuleVO.AlertRuleVOBuilder b = AlertRuleVO.builder();
        if (m.get("name") != null) b.name((String) m.get("name"));
        if (m.get("metricName") != null) b.metricName((String) m.get("metricName"));
        if (m.get("type") != null) b.type((String) m.get("type"));
        if (m.get("threshold") != null) b.threshold(((Number) m.get("threshold")).doubleValue());
        if (m.get("operator") != null) b.operator((String) m.get("operator"));
        if (m.get("duration") != null) b.duration(((Number) m.get("duration")).intValue());
        if (m.get("severity") != null) b.severity((String) m.get("severity"));
        if (m.get("description") != null) b.description((String) m.get("description"));
        if (m.get("enabled") != null) b.enabled((Boolean) m.get("enabled"));
        return b.build();
    }

    private static OperationLogSearchVO toOperationLogSearchVO(Map<String, Object> body) {
        OperationLogSearchVO vo = new OperationLogSearchVO();
        if (body == null) return vo;
        if (body.get("page") instanceof Number n) vo.setPage(n.intValue());
        if (body.get("rows") instanceof Number n) vo.setRows(n.intValue());
        if (body.get("module") instanceof String s) vo.setModule(s);
        if (body.get("action") instanceof String s) vo.setAction(s);
        if (body.get("username") instanceof String s) vo.setUsername(s);
        if (body.get("status") instanceof Number n) vo.setStatus(n.intValue());
        if (body.get("startTime") instanceof String s) vo.setStartTime(s);
        if (body.get("endTime") instanceof String s) vo.setEndTime(s);
        if (body.get("userId") instanceof Number n) vo.setUserId(n.longValue());
        return vo;
    }

    private static Long readAlertId(Map<String, Object> body) {
        if (body == null) return null;
        Object value = body.get("alertId") != null ? body.get("alertId") : body.get("id");
        return value instanceof Number n ? n.longValue() : null;
    }

    private static String safeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
