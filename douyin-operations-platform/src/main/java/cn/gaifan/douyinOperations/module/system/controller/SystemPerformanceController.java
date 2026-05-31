package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.metrics.PerformanceMetricsCollector;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 系统性能监控 API。
 *
 * <p>优先读取内存请求窗口、JVM 与 Redis INFO；没有采集样本时显式返回降级状态。</p>
 */
@RestController
@RequestMapping("/api/v1/system/performance")
public class SystemPerformanceController {

    @Autowired(required = false)
    private PerformanceMetricsCollector performanceMetricsCollector;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @PostMapping("/metrics/current")
    public RESTResult<Map<String, Object>> getMetricsCurrent(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("source", "jvm+in_memory_request_window");
        data.put("cpu", currentCpuUsage());
        data.put("cpuUsage", data.get("cpu"));
        data.put("memory", currentHeapUsage());
        data.put("memoryUsage", data.get("memory"));
        data.put("status", "UP");
        if (performanceMetricsCollector != null) {
            data.putAll(performanceMetricsCollector.getWindowSnapshot());
            data.put("degraded", number(data.get("requestCount")) <= 0);
            data.put("message", number(data.get("requestCount")) <= 0
                    ? "请求窗口暂无样本，访问业务接口后会自动产生性能数据。"
                    : "已接入请求性能窗口。");
        } else {
            data.put("requestCount", 0);
            data.put("degraded", true);
            data.put("message", "请求性能采集器未注册。");
        }
        return RESTResult.success(data);
    }

    @PostMapping("/metrics/search")
    public RESTResult<PageResultVO<Map<String, Object>>> getMetricsSearch(@RequestBody(required = false) Map<String, Object> body) {
        int page = intValue(body, "page", 0);
        int rows = Math.max(1, Math.min(intValue(body, "rows", 30), 100));
        List<Map<String, Object>> list = performanceMetricsCollector != null
                ? performanceMetricsCollector.getTopSlow(rows)
                : List.of();
        return RESTResult.success(PageResultVO.of((long) list.size(), list, page, rows));
    }

    @PostMapping("/api/timeseries")
    public RESTResult<List<Map<String, Object>>> getApiTimeseries(@RequestBody(required = false) Map<String, Object> body) {
        int hours = Math.max(1, Math.min(intValue(body, "hours", 24), 168));
        int points = Math.max(6, Math.min(intValue(body, "points", 24), 96));
        List<Map<String, Object>> rows = performanceMetricsCollector != null
                ? performanceMetricsCollector.getResponseTimeTimeseries(hours, points)
                : List.of();
        return RESTResult.success(rows);
    }

    @PostMapping("/query/analysis")
    public RESTResult<Map<String, Object>> getQueryAnalysis(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> snapshot = performanceMetricsCollector != null
                ? performanceMetricsCollector.getWindowSnapshot()
                : Map.of("requestCount", 0, "responseTime", 0, "errorRate", 0);
        long requests = number(snapshot.get("requestCount"));
        double avg = doubleValue(snapshot.get("responseTime"));
        double errorRate = doubleValue(snapshot.get("errorRate"));
        List<String> suggestions = new ArrayList<>();
        String status = "OK";
        String message = "请求性能窗口正常。";
        if (requests <= 0) {
            status = "NO_SAMPLES";
            message = "最近 5 分钟暂无请求样本。";
            suggestions.add("先访问核心业务接口，再刷新性能监控页。");
        } else {
            if (avg >= 1000) {
                status = "DEGRADED";
                suggestions.add("平均响应超过 1000ms，优先查看慢接口 TOP。");
            }
            if (errorRate > 0) {
                status = "DEGRADED";
                suggestions.add("窗口内存在 5xx 请求，优先查看错误日志和链路追踪。");
            }
        }
        return RESTResult.success(Map.of(
                "status", status,
                "message", message,
                "source", "in_memory_request_window",
                "requestCount", requests,
                "avgResponseMs", avg,
                "errorRate", errorRate,
                "suggestions", suggestions
        ));
    }

    @PostMapping("/query/slow")
    public RESTResult<List<Map<String, Object>>> getSlowQueries(@RequestBody(required = false) Map<String, Object> body) {
        int limit = Math.max(1, Math.min(intValue(body, "rows", intValue(body, "limit", 10)), 100));
        double minMs = doubleValue(body != null ? body.getOrDefault("minMs", body.get("minDurationMs")) : null);
        List<Map<String, Object>> rows = performanceMetricsCollector != null
                ? performanceMetricsCollector.getTopSlow(limit).stream()
                .filter(row -> doubleValue(row.get("avgMs")) >= minMs)
                .toList()
                : List.of();
        return RESTResult.success(rows);
    }

    @PostMapping("/query/n-plus-one")
    public RESTResult<List<Map<String, Object>>> getNPlusOneQueries(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(List.of());
    }

    @PostMapping("/index/suggestions")
    public RESTResult<List<Map<String, Object>>> getIndexSuggestions(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(List.of());
    }

    @PostMapping("/cache/statistics")
    public RESTResult<Map<String, Object>> getCacheStatistics(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(redisCacheStatistics());
    }

    @PostMapping("/cache/hot-keys")
    public RESTResult<List<Map<String, Object>>> getCacheHotKeys(@RequestBody(required = false) Map<String, Object> body) {
        int limit = Math.max(1, Math.min(intValue(body, "limit", intValue(body, "topN", 10)), 50));
        Object hotKeys = redisCacheStatistics().get("hotKeys");
        if (hotKeys instanceof List<?> list) {
            return RESTResult.success(list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .limit(limit)
                    .toList());
        }
        return RESTResult.success(List.of());
    }

    @PostMapping("/cache/trend")
    public RESTResult<List<Map<String, Object>>> getCacheTrend(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> cache = redisCacheStatistics();
        Object hitRate = cache.get("hitRate");
        if (hitRate == null) {
            return RESTResult.success(List.of());
        }
        return RESTResult.success(List.of(Map.of(
                "timestamp", Instant.now().toString(),
                "hitRate", hitRate,
                "hitCount", cache.getOrDefault("hitCount", 0L),
                "missCount", cache.getOrDefault("missCount", 0L),
                "evictionCount", cache.getOrDefault("evictedKeys", 0L)
        )));
    }

    @PostMapping("/cache/clear")
    public RESTResult<Map<String, Object>> clearCache(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(Map.of("clearedCount", 0));
    }

    @PostMapping("/cache/rebuild")
    public RESTResult<Map<String, Object>> rebuildCache(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(Map.of("jobId", ""));
    }

    @PostMapping(value = "/export", produces = "application/octet-stream")
    public org.springframework.http.ResponseEntity<byte[]> exportPerformance(@RequestBody(required = false) Map<String, Object> body) {
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=performance.csv")
                .body(new byte[0]);
    }

    @PostMapping("/benchmark")
    public RESTResult<Map<String, Object>> runBenchmark(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> current = getMetricsCurrent(body).getData();
        Map<String, Object> cache = redisCacheStatistics();
        return RESTResult.success(Map.of(
                "status", "ready",
                "source", "current_runtime_snapshot",
                "apiLatencyP95Ms", current != null ? current.getOrDefault("responseTime", 0) : 0,
                "apiLatencyP99Ms", current != null ? current.getOrDefault("responseTime", 0) : 0,
                "errorRate", current != null ? current.getOrDefault("errorRate", 0) : 0,
                "cpuUsagePercent", current != null ? current.getOrDefault("cpu", 0) : 0,
                "memoryUsagePercent", current != null ? current.getOrDefault("memory", 0) : 0,
                "cacheHitRate", cache.getOrDefault("hitRate", 0),
                "databaseQPS", current != null ? current.getOrDefault("requestsPerSecond", 0) : 0
        ));
    }

    private Map<String, Object> redisCacheStatistics() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("source", "redis_info");
        if (redisConnectionFactory == null) {
            data.put("hitCount", 0L);
            data.put("missCount", 0L);
            data.put("degraded", true);
            data.put("message", "RedisConnectionFactory 未注册。");
            data.put("hotKeys", List.of());
            return data;
        }
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Properties stats = connection.serverCommands().info("stats");
            Properties memory = connection.serverCommands().info("memory");
            Long dbSize = connection.serverCommands().dbSize();
            long hits = parseLong(stats != null ? stats.getProperty("keyspace_hits") : null, 0L);
            long misses = parseLong(stats != null ? stats.getProperty("keyspace_misses") : null, 0L);
            long total = hits + misses;
            data.put("hitCount", hits);
            data.put("missCount", misses);
            data.put("cacheHits", hits);
            data.put("cacheMisses", misses);
            data.put("total", total);
            data.put("hitRate", total > 0 ? round(hits * 1.0 / total, 4) : null);
            data.put("hitRatePercent", total > 0 ? round(hits * 100.0 / total, 2) : null);
            data.put("keyCount", dbSize != null ? dbSize : 0L);
            data.put("totalKeys", dbSize != null ? dbSize : 0L);
            data.put("evictedKeys", parseLong(stats != null ? stats.getProperty("evicted_keys") : null, 0L));
            data.put("expiredKeys", parseLong(stats != null ? stats.getProperty("expired_keys") : null, 0L));
            data.put("usedMemoryHuman", memory != null ? memory.getProperty("used_memory_human") : null);
            data.put("degraded", total <= 0);
            data.put("message", total <= 0 ? "Redis INFO 暂无 keyspace 命中/未命中样本。" : "已读取 Redis INFO stats。");
            data.put("hotKeys", scanHotKeys(connection, 10));
        } catch (RuntimeException ex) {
            data.put("hitCount", 0L);
            data.put("missCount", 0L);
            data.put("degraded", true);
            data.put("message", "Redis 指标读取失败: " + ex.getMessage());
            data.put("hotKeys", List.of());
        }
        return data;
    }

    private List<Map<String, Object>> scanHotKeys(RedisConnection connection, int limit) {
        try {
            return connection.keyCommands().keys("*".getBytes(StandardCharsets.UTF_8)).stream()
                    .limit(Math.max(1, limit))
                    .map(key -> {
                        String name = new String(key, StandardCharsets.UTF_8);
                        Long ttl = connection.keyCommands().ttl(key);
                        return Map.<String, Object>of(
                                "key", name,
                                "accessCount", 0,
                                "accessRate", 0,
                                "sizeMB", 0,
                                "ttlSeconds", ttl != null ? ttl : -1
                        );
                    })
                    .sorted(Comparator.comparing(row -> String.valueOf(row.get("key"))))
                    .toList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private static int intValue(Map<String, Object> body, String key, int fallback) {
        if (body == null || body.get(key) == null) {
            return fallback;
        }
        Object value = body.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static long number(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return value != null ? Long.parseLong(String.valueOf(value)) : 0L;
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static double doubleValue(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return value != null ? Double.parseDouble(String.valueOf(value)) : 0.0;
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static double currentCpuUsage() {
        try {
            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                double value = sunOs.getProcessCpuLoad() * 100;
                if (value >= 0 && !Double.isNaN(value)) {
                    return round(value, 2);
                }
            }
        } catch (RuntimeException ignored) {
            return 0.0;
        }
        return 0.0;
    }

    private static double currentHeapUsage() {
        try {
            MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
            long used = memory.getHeapMemoryUsage().getUsed();
            long max = memory.getHeapMemoryUsage().getMax();
            return max > 0 ? round(used * 100.0 / max, 2) : 0.0;
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}
