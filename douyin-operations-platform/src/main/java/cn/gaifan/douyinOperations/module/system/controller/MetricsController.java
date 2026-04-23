package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.system.service.MetricsCollectorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * Prometheus 兼容的指标端点控制器
 */
@RestController
@RequestMapping("/api/v1/system/metrics")
public class MetricsController {

    @Resource
    private MetricsCollectorService metricsCollectorService;

    /**
     * Prometheus scrape endpoint
     */
    @GetMapping("/prometheus")
    public String prometheusMetrics() {
        var metrics = metricsCollectorService.collectAllMetrics();
        StringBuilder sb = new StringBuilder();

        for (var metric : metrics) {
            // Prometheus 格式：HELP 和 TYPE 注释 + 指标行
            sb.append("# HELP ").append(metric.getName()).append(" ").append(metric.getUnit()).append("\n");
            sb.append("# TYPE ").append(metric.getName()).append(" gauge\n");

            Double value;
            try {
                value = Double.parseDouble(metric.getValue().split("/")[0]);
            } catch (Exception e) {
                value = 0.0;
            }

            sb.append(metric.getName()).append(" ").append(value).append("\n");
        }

        return sb.toString();
    }

    /**
     * 获取所有指标（JSON 格式）
     */
    @GetMapping("/all")
    public RESTResult<?> getAllMetrics() {
        return RESTResult.success(metricsCollectorService.collectAllMetrics());
    }

    /**
     * 获取指定指标
     */
    @PostMapping("/get")
    public RESTResult<?> getMetric(@RequestBody Map<String, String> request) {
        String metricName = request.get("name");
        return RESTResult.success(metricsCollectorService.getMetricsByName(metricName));
    }

    /**
     * 获取 CPU 指标
     */
    @GetMapping("/cpu")
    public RESTResult<?> getCpuMetrics() {
        return RESTResult.success(metricsCollectorService.collectCpuMetrics());
    }

    /**
     * 获取内存指标
     */
    @GetMapping("/memory")
    public RESTResult<?> getMemoryMetrics() {
        return RESTResult.success(metricsCollectorService.collectMemoryMetrics());
    }

    /**
     * 获取磁盘指标
     */
    @GetMapping("/disk")
    public RESTResult<?> getDiskMetrics() {
        return RESTResult.success(metricsCollectorService.collectDiskMetrics());
    }

    /**
     * 获取 JVM 指标
     */
    @GetMapping("/jvm")
    public RESTResult<?> getJvmMetrics() {
        return RESTResult.success(metricsCollectorService.collectJvmMetrics());
    }

    /**
     * 获取数据库连接池指标
     */
    @GetMapping("/database")
    public RESTResult<?> getDatabaseMetrics() {
        return RESTResult.success(metricsCollectorService.collectDatabaseMetrics());
    }
}
