package cn.gaifan.douyinOperations.module.system.service.impl;

import cn.gaifan.douyinOperations.module.system.service.MetricsCollectorService;
import cn.gaifan.douyinOperations.module.system.vo.MetricsVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 指标收集服务实现
 */
@Service
public class MetricsCollectorServiceImpl implements MetricsCollectorService {

    private static final Logger log = LoggerFactory.getLogger(MetricsCollectorServiceImpl.class);

    // P1-2: Caffeine 缓存指标数据，TTL 30 秒，最大 10 个条目
    private final Cache<String, List<MetricsVO>> metricsCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(10)
            .build();

    private static final String CACHE_KEY = "all_metrics";

    @Override
    public MetricsVO collectCpuMetrics() {
        // P1-5: 使用真实 CPU 使用率，而非随机模拟
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = 0.0;

        // 尝试获取系统 CPU 负载（需要 com.sun.management.OperatingSystemMXBean）
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            cpuLoad = sunOsBean.getSystemCpuLoad() * 100;

            // getSystemCpuLoad() 返回负数表示不可用，降级为进程 CPU
            if (cpuLoad < 0) {
                cpuLoad = sunOsBean.getProcessCpuLoad() * 100;
            }
        }

        // 如果仍然无法获取，使用系统平均负载作为近似值
        if (cpuLoad <= 0) {
            double loadAverage = osBean.getSystemLoadAverage();
            int availableProcessors = osBean.getAvailableProcessors();
            if (loadAverage >= 0 && availableProcessors > 0) {
                cpuLoad = (loadAverage / availableProcessors) * 100;
            }
        }

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
        // P1-2: 从缓存读取，缓存未命中时重新收集
        return metricsCache.get(CACHE_KEY, key -> {
            List<MetricsVO> metrics = new ArrayList<>();
            metrics.add(collectCpuMetrics());
            metrics.add(collectMemoryMetrics());
            metrics.add(collectDiskMetrics());
            metrics.add(collectJvmMetrics());
            metrics.add(collectDatabaseMetrics());

            log.info("Collected {} metrics (cache miss)", metrics.size());
            return metrics;
        });
    }

    @Override
    public MetricsVO getMetricsByName(String name) {
        return collectAllMetrics().stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
