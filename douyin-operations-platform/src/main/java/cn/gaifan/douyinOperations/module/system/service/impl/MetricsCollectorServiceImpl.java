package cn.gaifan.douyinOperations.module.system.service.impl;

import cn.gaifan.douyinOperations.module.system.service.MetricsCollectorService;
import cn.gaifan.douyinOperations.module.system.vo.MetricsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 指标收集服务实现
 */
@Service
public class MetricsCollectorServiceImpl implements MetricsCollectorService {

    private static final Logger log = LoggerFactory.getLogger(MetricsCollectorServiceImpl.class);

    @Override
    public MetricsVO collectCpuMetrics() {
        // 简单实现：随机模拟 CPU 使用率
        double cpuLoad = Math.random() * 100;

        String status = "normal";
        if (cpuLoad > 80) {
            status = "critical";
        } else if (cpuLoad > 60) {
            status = "warning";
        }

        return MetricsVO.builder()
                .name("cpu_usage")
                .value(String.format("%.2f", cpuLoad))
                .unit("%")
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public MetricsVO collectMemoryMetrics() {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memBean.getHeapMemoryUsage().getUsed();
        long heapMax = memBean.getHeapMemoryUsage().getMax();
        double heapPercent = (double) heapUsed / heapMax * 100;

        String status = "normal";
        if (heapPercent > 90) {
            status = "critical";
        } else if (heapPercent > 70) {
            status = "warning";
        }

        return MetricsVO.builder()
                .name("memory_usage")
                .value(String.format("%.2f", heapPercent))
                .unit("%")
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public MetricsVO collectDiskMetrics() {
        java.io.File root = new java.io.File("/");
        long total = root.getTotalSpace();
        long free = root.getFreeSpace();
        long used = total - free;
        double diskPercent = (double) used / total * 100;

        String status = "normal";
        if (diskPercent > 90) {
            status = "critical";
        } else if (diskPercent > 70) {
            status = "warning";
        }

        return MetricsVO.builder()
                .name("disk_usage")
                .value(String.format("%.2f", diskPercent))
                .unit("%")
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public MetricsVO collectJvmMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double jvmPercent = (double) usedMemory / maxMemory * 100;

        return MetricsVO.builder()
                .name("jvm_memory_usage")
                .value(String.format("%.2f", jvmPercent))
                .unit("%")
                .status("normal")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public MetricsVO collectDatabaseMetrics() {
        // 数据库连接池指标收集
        // 注：实际实现需要通过 DataSource 获取连接池信息
        return MetricsVO.builder()
                .name("database_connections")
                .value("10/20")
                .unit("active/max")
                .status("normal")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public List<MetricsVO> collectAllMetrics() {
        List<MetricsVO> metrics = new ArrayList<>();
        metrics.add(collectCpuMetrics());
        metrics.add(collectMemoryMetrics());
        metrics.add(collectDiskMetrics());
        metrics.add(collectJvmMetrics());
        metrics.add(collectDatabaseMetrics());

        log.info("Collected {} metrics", metrics.size());
        return metrics;
    }

    @Override
    public MetricsVO getMetricsByName(String name) {
        return collectAllMetrics().stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
