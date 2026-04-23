package cn.gaifan.douyinOperations.common.metrics;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 性能指标聚合器：滑动窗口统计最近 5 分钟 API 请求，供监控慢接口/错误接口排名。
 *
 * @author gaifan
 */
@Component
public class PerformanceMetricsCollector {

    private static final long WINDOW_MS = 5 * 60 * 1000L;

    private static class EndpointStats {
        long count;
        long totalDurationMs;
        long errorCount;
        long lastAccessTime;

        synchronized void record(long durationMs, boolean isError) {
            count++;
            totalDurationMs += durationMs;
            if (isError) errorCount++;
            lastAccessTime = System.currentTimeMillis();
        }

        double avgMs() {
            return count > 0 ? (double) totalDurationMs / count : 0;
        }
    }

    private final ConcurrentHashMap<String, EndpointStats> stats = new ConcurrentHashMap<>();

    /**
     * 记录一次 API 请求
     *
     * @param endpoint   端点标识（如 ControllerName.methodName）
     * @param durationMs 耗时（毫秒）
     * @param isError    是否发生异常
     */
    public void record(String endpoint, long durationMs, boolean isError) {
        stats.computeIfAbsent(endpoint, k -> new EndpointStats()).record(durationMs, isError);
    }

    /**
     * 获取慢接口排名（按平均耗时降序）
     *
     * @param limit 返回条数
     * @return 每项含 endpoint, avgMs, count
     */
    public List<Map<String, Object>> getTopSlow(int limit) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        List<Map<String, Object>> result = new ArrayList<>();
        stats.entrySet().stream()
                .filter(e -> e.getValue().lastAccessTime >= cutoff && e.getValue().count > 0)
                .map(e -> Map.<String, Object>of(
                        "endpoint", e.getKey(),
                        "avgMs", Math.round(e.getValue().avgMs()),
                        "count", e.getValue().count))
                .sorted((a, b) -> Long.compare((Long) b.get("avgMs"), (Long) a.get("avgMs")))
                .limit(Math.max(1, limit))
                .forEach(result::add);
        return result;
    }

    /**
     * 获取错误接口排名（按错误数降序）
     *
     * @param limit 返回条数
     * @return 每项含 endpoint, errorCount, count
     */
    public List<Map<String, Object>> getTopError(int limit) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        List<Map<String, Object>> result = new ArrayList<>();
        stats.entrySet().stream()
                .filter(e -> e.getValue().lastAccessTime >= cutoff && e.getValue().errorCount > 0)
                .map(e -> Map.<String, Object>of(
                        "endpoint", e.getKey(),
                        "errorCount", e.getValue().errorCount,
                        "count", e.getValue().count))
                .sorted((a, b) -> Long.compare((Long) b.get("errorCount"), (Long) a.get("errorCount")))
                .limit(Math.max(1, limit))
                .forEach(result::add);
        return result;
    }

    @Scheduled(fixedRate = 300_000)
    public void evictExpired() {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        stats.entrySet().removeIf(e -> e.getValue().lastAccessTime < cutoff);
    }
}
