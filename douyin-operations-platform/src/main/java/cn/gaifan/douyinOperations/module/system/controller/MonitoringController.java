package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.system.service.AlertEngineService;
import cn.gaifan.douyinOperations.module.system.service.DashboardDataService;
import cn.gaifan.douyinOperations.module.system.vo.AlertRecordVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleVO;
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
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 监控模块 API（前后端对齐）
 * 代理 /system/alert 与 /system/dashboard，并提供指标/日志/健康等接口。
 * 已对接：/metrics/realtime（MeterRegistry + JVM）、/health/*（Actuator HealthContributorRegistry）。
 * 桩接口（stub=true）：日志、告警确认等尚未接入真实数据源，前端可展示「暂无真实数据」。
 */
@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    @Resource
    private AlertEngineService alertEngineService;

    @Resource
    private DashboardDataService dashboardDataService;

    @Resource
    private MeterRegistry meterRegistry;

    @Resource
    private HealthContributorRegistry healthContributorRegistry;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired(required = false)
    private cn.gaifan.douyinOperations.common.metrics.PerformanceMetricsCollector performanceMetricsCollector;

    // ─── 实时指标（对接 Actuator/MeterRegistry + JVM）──────────────────────

    @PostMapping("/metrics/realtime")
    public RESTResult<Map<String, Object>> getRealtimeMetrics() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("stub", false);

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

        long requestCount = 0;
        try {
            for (var t : meterRegistry.find("http.server.requests").timers()) {
                requestCount += (long) t.count();
            }
        } catch (Exception ignored) { /* Micrometer 可能未注册该指标 */ }
        data.put("requestCount", requestCount);

        return RESTResult.success(data);
    }

    @PostMapping("/metrics/historical")
    public RESTResult<Map<String, Object>> getHistoricalMetrics(@RequestBody Map<String, Object> body) {
        return RESTResult.success(Map.of("data", List.of(), "stub", true, "note", "暂无历史指标数据，需接入 Prometheus/时序库"));
    }

    @PostMapping("/metrics/trend")
    public RESTResult<Map<String, Object>> getPerformanceTrend(@RequestBody Map<String, Object> body) {
        return RESTResult.success(Map.of("data", List.of(), "stub", true, "note", "暂无趋势数据，需接入 Prometheus"));
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

    /** 桩实现：需对接 AlertEngineService 实现真实确认逻辑 */
    @PostMapping("/alerts/acknowledge")
    public RESTResult<Void> acknowledgeAlert(@RequestBody Map<String, Object> body) {
        return RESTResult.success();
    }

    /** 桩实现：需对接 AlertEngineService */
    @PostMapping("/alerts/resolve")
    public RESTResult<Void> resolveAlert(@RequestBody Map<String, Object> body) {
        return RESTResult.success();
    }

    /** 桩实现：需对接 AlertEngineService */
    @PostMapping("/alerts/close")
    public RESTResult<Void> closeAlert(@RequestBody Map<String, Object> body) {
        return RESTResult.success();
    }

    /** 桩实现：需对接 AlertEngineService */
    @PostMapping("/alerts/batch-acknowledge")
    public RESTResult<Void> acknowledgeAlerts(@RequestBody Map<String, Object> body) {
        return RESTResult.success();
    }

    @PostMapping("/alerts/statistics")
    public RESTResult<Map<String, Object>> getAlertStatistics(@RequestBody Map<String, Object> body) {
        return RESTResult.success(Map.of("total", 0, "critical", 0, "warning", 0, "stub", true, "note", "需对接真实告警统计"));
    }

    // ─── 日志（桩）──────────────────────────────────────────────────────

    @PostMapping("/logs/search")
    public RESTResult<PageResultVO<Map<String, Object>>> searchLogs(@RequestBody Map<String, Object> body) {
        PageResultVO<Map<String, Object>> stub = PageResultVO.of(0L, List.of(), 0, 30);
        return RESTResult.success(stub);
    }

    @PostMapping("/logs/detail")
    public RESTResult<Map<String, Object>> getLogDetail(@RequestBody Map<String, Object> body) {
        return RESTResult.success(Map.of("stub", true, "note", "日志详情为桩实现"));
    }

    @PostMapping(value = "/logs/export", produces = "text/csv;charset=UTF-8")
    public org.springframework.http.ResponseEntity<byte[]> exportLogs(@RequestBody Map<String, Object> body) {
        byte[] csv = "time,level,message\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=logs.csv")
                .body(csv);
    }

    @PostMapping("/logs/statistics")
    public RESTResult<Map<String, Object>> getLogStatistics(@RequestBody Map<String, Object> body) {
        return RESTResult.success(Map.of("total", 0, "stub", true, "note", "日志统计为桩实现"));
    }

    @PostMapping("/logs/error-aggregation")
    public RESTResult<List<Map<String, Object>>> getErrorAggregation(@RequestBody Map<String, Object> body) {
        return RESTResult.success(List.of());
    }

    @PostMapping("/logs/cleanup")
    public RESTResult<Map<String, Object>> cleanupOldLogs(@RequestBody Map<String, Object> body) {
        return RESTResult.success(Map.of("deletedCount", 0, "stub", true, "note", "日志清理为桩实现"));
    }

    // ─── 健康检查（委托 dashboard）──────────────────────────────────────

    @PostMapping("/health/status")
    public RESTResult<Map<String, Object>> getHealthStatus() {
        try {
            Object overview = dashboardDataService.getSystemOverview();
            return RESTResult.success(overview != null ? Map.of("status", "UP", "overview", overview) : Map.of("status", "UP"));
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
        if (Boolean.TRUE.equals(out.get("stub")) && dataSource != null) {
            try (Connection c = dataSource.getConnection()) {
                out.put("status", "UP");
                out.put("stub", false);
            } catch (Exception e) {
                out.put("status", "DOWN");
                out.put("message", e.getMessage());
                out.put("stub", false);
            }
        }
        return RESTResult.success(out);
    }

    @PostMapping("/health/cache")
    public RESTResult<Map<String, Object>> getCacheStatus() {
        Map<String, Object> out = getHealthFromActuator("redis");
        if (Boolean.TRUE.equals(out.get("stub")) && redisConnectionFactory != null) {
            try {
                redisConnectionFactory.getConnection().ping();
                out.put("status", "UP");
                out.put("stub", false);
            } catch (Exception e) {
                out.put("status", "DOWN");
                out.put("message", e.getMessage());
                out.put("stub", false);
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
        out.put("stub", !fromActuator);
        return out;
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
        return RESTResult.success(Map.of("alertsCount", 0, "logsCount", 0, "stub", true, "note", "监控统计为桩实现"));
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
        return RESTResult.success(Map.of("cpu", 0, "memory", 0, "stub", true, "note", "系统资源为桩实现，/metrics/realtime 已对接真实数据"));
    }

    // ─── SSE 流（桩，避免 404）──────────────────────────────────────────

    @GetMapping(value = "/stream/realtime", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRealtime() {
        SseEmitter emitter = new SseEmitter(60_000L);
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @GetMapping(value = "/stream/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAlerts() {
        SseEmitter emitter = new SseEmitter(60_000L);
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
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
}
