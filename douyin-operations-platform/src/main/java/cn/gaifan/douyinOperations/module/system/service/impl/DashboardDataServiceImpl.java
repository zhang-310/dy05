package cn.gaifan.douyinOperations.module.system.service.impl;

import cn.gaifan.douyinOperations.module.system.service.DashboardDataService;
import cn.gaifan.douyinOperations.module.system.service.MetricsCollectorService;
import cn.gaifan.douyinOperations.module.system.vo.DashboardDataVO;
import cn.gaifan.douyinOperations.module.system.vo.MetricsVO;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * 仪表板数据服务实现
 */
@Service
public class DashboardDataServiceImpl implements DashboardDataService {

    @Resource
    private MetricsCollectorService metricsCollectorService;

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
        alerts.put("total", 25);
        alerts.put("critical", 3);
        alerts.put("warning", 8);
        alerts.put("normal", 14);

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
        List<Double> cpuTrend = Arrays.asList(45.2, 48.5, 52.3, 50.1, 55.8, 58.2, 56.5);
        List<Double> memoryTrend = Arrays.asList(62.1, 63.5, 65.2, 64.8, 68.3, 70.1, 69.5);
        trends.put("cpu_trend", cpuTrend);
        trends.put("memory_trend", memoryTrend);

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
        logStats.put("total_logs", 125480);
        logStats.put("error_logs", 245);
        logStats.put("warning_logs", 1520);
        logStats.put("info_logs", 123715);

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
        traces.put("total_traces", 50000);
        traces.put("slow_traces", 125);
        traces.put("error_traces", 45);
        traces.put("avg_duration", "145ms");

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
        health.put("database", "UP");
        health.put("cache", "UP");
        health.put("message_queue", "UP");
        health.put("elasticsearch", "UP");

        return DashboardDataVO.builder()
                .title("健康检查状态")
                .data(health)
                .type("status")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
