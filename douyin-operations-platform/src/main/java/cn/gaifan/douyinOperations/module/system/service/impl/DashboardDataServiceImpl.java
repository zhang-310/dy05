package cn.gaifan.douyinOperations.module.system.service.impl;

import cn.gaifan.douyinOperations.module.log.repository.OperationLogRepository;
import cn.gaifan.douyinOperations.module.system.repository.SysAlertRecordRepository;
import cn.gaifan.douyinOperations.module.system.service.DashboardDataService;
import cn.gaifan.douyinOperations.module.system.service.MetricsCollectorService;
import cn.gaifan.douyinOperations.module.system.vo.DashboardDataVO;
import cn.gaifan.douyinOperations.module.system.vo.MetricsVO;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * 仪表板数据服务实现
 * P0-5: 对接真实数据源，移除硬编码假数据
 */
@Service
public class DashboardDataServiceImpl implements DashboardDataService {

    @Resource
    private MetricsCollectorService metricsCollectorService;

    @Resource
    private MeterRegistry meterRegistry;

    @Resource
    private SysAlertRecordRepository alertRecordRepository;

    @Resource
    private OperationLogRepository operationLogRepository;

    @Override
    public DashboardDataVO getSystemOverview() {
        List<MetricsVO> metrics = metricsCollectorService.collectAllMetrics();
        Map<String, Object> overview = new HashMap<>();
        overview.put("metrics", metrics);
        overview.put("timestamp", System.currentTimeMillis());

        return DashboardDataVO.builder()
                .title("系统整体统计")
                .data(overview)
                .type("overview")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public DashboardDataVO getRealtimeAlerts() {
        Map<String, Object> alerts = new HashMap<>();
        Map<String, Long> severityCounts = toCountMap(alertRecordRepository.countBySeverity());
        Map<String, Long> statusCounts = toCountMap(alertRecordRepository.countByStatus());
        alerts.put("total", severityCounts.values().stream().mapToLong(Long::longValue).sum());
        alerts.put("critical", severityCounts.getOrDefault("critical", 0L));
        alerts.put("warning", severityCounts.getOrDefault("warning", 0L));
        alerts.put("triggered", statusCounts.getOrDefault("triggered", 0L));
        alerts.put("resolved", statusCounts.getOrDefault("resolved", 0L));
        alerts.put("degraded", false);
        alerts.put("source", "sys_alert_record");

        return DashboardDataVO.builder()
                .title("实时告警数据")
                .data(alerts)
                .type("status")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public DashboardDataVO getPerformanceTrends() {
        Map<String, Object> trends = new HashMap<>();

        trends.put("cpu_current", readGauge("process.cpu.usage", 100.0).orElse(null));
        trends.put("memory_current", readHeapUsage().orElse(null));
        trends.put("degraded", true);
        trends.put("source", "micrometer_snapshot");
        trends.put("fallbackReason", "当前返回实时指标快照；历史趋势需接入 Prometheus/时序库");

        return DashboardDataVO.builder()
                .title("性能指标趋势")
                .data(trends)
                .type("chart")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public DashboardDataVO getLogStatistics() {
        Map<String, Object> logStats = new HashMap<>();
        logStats.put("total_logs", operationLogRepository.count());
        logStats.put("degraded", false);
        logStats.put("source", "sys_operation_log");

        return DashboardDataVO.builder()
                .title("日志聚合统计")
                .data(logStats)
                .type("table")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public DashboardDataVO getTracesSummary() {
        Map<String, Object> traces = new HashMap<>();
        traces.put("degraded", true);
        traces.put("source", "not_configured");
        traces.put("fallbackReason", "需要对接 OpenTelemetry 后端");
        traces.put("total_traces", 0);
        traces.put("slow_traces", 0);
        traces.put("error_traces", 0);
        traces.put("avg_duration", "N/A");

        return DashboardDataVO.builder()
                .title("链路追踪摘要")
                .data(traces)
                .type("overview")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public DashboardDataVO getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("degraded", true);
        health.put("source", "overview");
        health.put("fallbackReason", "组件级健康状态由 /api/v1/monitoring/health/* 查询 Actuator");

        return DashboardDataVO.builder()
                .title("健康检查状态")
                .data(health)
                .type("status")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> out = new HashMap<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] instanceof Number n) {
                out.put(String.valueOf(row[0]), n.longValue());
            }
        }
        return out;
    }

    private Optional<Double> readGauge(String name, double multiplier) {
        try {
            for (var gauge : meterRegistry.find(name).gauges()) {
                double value = gauge.value();
                if (!Double.isNaN(value) && Double.isFinite(value)) {
                    return Optional.of(value * multiplier);
                }
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<Double> readHeapUsage() {
        try {
            double used = meterRegistry.get("jvm.memory.used").gauge().value();
            double max = meterRegistry.get("jvm.memory.max").gauge().value();
            return max > 0 ? Optional.of(used / max * 100) : Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
