package cn.gaifan.douyinOperations.common.metrics;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 性能指标聚合器：滑动窗口统计最近 5 分钟 API 请求，供监控慢接口/错误接口排名。
 *
 * @author gaifan
 */
@Component
public class PerformanceMetricsCollector {

    private static final long WINDOW_MS = 5 * 60 * 1000L;
    private static final long WEEK_MS = 7 * 24 * 60 * 60 * 1000L;
    private static final int MAX_SAMPLES = 20_000;

    private static class EndpointStats {
        long count;
        long totalDurationMs;
        long errorCount;
        long maxDurationMs;
        long lastAccessTime;

        synchronized void record(long durationMs, boolean isError) {
            count++;
            totalDurationMs += durationMs;
            maxDurationMs = Math.max(maxDurationMs, durationMs);
            if (isError) errorCount++;
            lastAccessTime = System.currentTimeMillis();
        }

        double avgMs() {
            return count > 0 ? (double) totalDurationMs / count : 0;
        }
    }

    private final ConcurrentHashMap<String, EndpointStats> stats = new ConcurrentHashMap<>();
    private final Deque<RequestSample> samples = new ConcurrentLinkedDeque<>();
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    @Value("${app.common.metrics.eviction-enabled:true}")
    private boolean evictionEnabled;

    /**
     * 记录一次 API 请求
     *
     * @param endpoint   端点标识（如 ControllerName.methodName）
     * @param durationMs 耗时（毫秒）
     * @param isError    是否发生异常
     */
    public void record(String endpoint, long durationMs, boolean isError) {
        stats.computeIfAbsent(endpoint, k -> new EndpointStats()).record(durationMs, isError);
        samples.addLast(new RequestSample(System.currentTimeMillis(), Math.max(durationMs, 0), isError));
        trimSamples(System.currentTimeMillis() - WEEK_MS);
    }

    public void requestStarted() {
        activeRequests.incrementAndGet();
    }

    public void requestFinished() {
        activeRequests.updateAndGet(value -> Math.max(value - 1, 0));
    }

    public Map<String, Object> getWindowSnapshot() {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;
        List<RequestSample> window = samples.stream()
                .filter(sample -> sample.timestamp >= cutoff)
                .toList();
        long count = window.size();
        long errorCount = window.stream().filter(RequestSample::error).count();
        double avgMs = window.stream().mapToLong(RequestSample::durationMs).average().orElse(0);
        double errorRate = count > 0 ? errorCount * 100.0 / count : 0;
        double requestsPerSecond = count > 0 ? count / (WINDOW_MS / 1000.0) : 0;
        return Map.of(
                "requestCount", count,
                "responseTime", round(avgMs, 2),
                "errorRate", round(errorRate, 4),
                "requestsPerSecond", round(requestsPerSecond, 2),
                "activeConnections", activeRequests.get(),
                "windowMs", WINDOW_MS
        );
    }

    public Map<String, Object> getTrend(String metricName, String timeRange, int dataPointCount) {
        String metric = metricName == null || metricName.isBlank() ? "responseTime" : metricName;
        String range = timeRange == null || timeRange.isBlank() ? "hour" : timeRange;
        int points = Math.max(1, Math.min(dataPointCount, 240));
        long now = System.currentTimeMillis();
        long durationMs = durationForRange(range);
        long intervalMs = Math.max(durationMs / points, 1000);
        long start = now - durationMs;
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        for (int i = 0; i < points; i++) {
            long bucketStart = start + i * intervalMs;
            long bucketEnd = i == points - 1 ? now + 1 : bucketStart + intervalMs;
            List<RequestSample> bucket = samples.stream()
                    .filter(sample -> sample.timestamp >= bucketStart && sample.timestamp < bucketEnd)
                    .toList();
            Double value = bucketValue(metric, bucket, intervalMs);
            if (value != null) {
                dataPoints.add(Map.of(
                        "timestamp", bucketEnd,
                        "value", round(value, "errorRate".equals(metric) ? 4 : 2),
                        "label", String.valueOf(bucketEnd)
                ));
            }
        }

        List<Double> values = dataPoints.stream()
                .map(point -> ((Number) point.get("value")).doubleValue())
                .sorted(Comparator.naturalOrder())
                .toList();
        Map<String, Object> summary = values.isEmpty()
                ? Map.of("average", 0, "min", 0, "max", 0, "percentile95", 0, "percentile99", 0)
                : Map.of(
                "average", round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0), 4),
                "min", values.get(0),
                "max", values.get(values.size() - 1),
                "percentile95", percentile(values, 0.95),
                "percentile99", percentile(values, 0.99)
        );

        return Map.of(
                "metricName", metric,
                "unit", "errorRate".equals(metric) ? "%" : "ms",
                "timeRange", range,
                "dataPoints", dataPoints,
                "summary", summary,
                "source", "in_memory_request_window",
                "degraded", false
        );
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
                        "path", e.getKey(),
                        "avgMs", Math.round(e.getValue().avgMs()),
                        "count", e.getValue().count,
                        "callCount", e.getValue().count,
                        "maxMs", e.getValue().maxDurationMs))
                .sorted((a, b) -> Long.compare((Long) b.get("avgMs"), (Long) a.get("avgMs")))
                .limit(Math.max(1, limit))
                .forEach(result::add);
        return result;
    }

    public List<Map<String, Object>> getResponseTimeTimeseries(int hours, int dataPointCount) {
        int normalizedHours = Math.max(1, Math.min(hours, 168));
        int points = Math.max(1, Math.min(dataPointCount, 240));
        long now = System.currentTimeMillis();
        long durationMs = normalizedHours * 60L * 60L * 1000L;
        long intervalMs = Math.max(durationMs / points, 1000L);
        long start = now - durationMs;
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 0; i < points; i++) {
            long bucketStart = start + i * intervalMs;
            long bucketEnd = i == points - 1 ? now + 1 : bucketStart + intervalMs;
            List<Double> durations = samples.stream()
                    .filter(sample -> sample.timestamp >= bucketStart && sample.timestamp < bucketEnd)
                    .map(sample -> (double) sample.durationMs)
                    .sorted(Comparator.naturalOrder())
                    .toList();
            if (durations.isEmpty()) {
                continue;
            }
            result.add(Map.of(
                    "time", java.time.Instant.ofEpochMilli(bucketEnd).toString(),
                    "ts", bucketEnd,
                    "p50", percentile(durations, 0.50),
                    "p95", percentile(durations, 0.95),
                    "p99", percentile(durations, 0.99),
                    "avg", round(durations.stream().mapToDouble(Double::doubleValue).average().orElse(0), 2),
                    "count", durations.size()
            ));
        }
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

    @Scheduled(fixedRateString = "${app.common.metrics.eviction-fixed-rate-ms:300000}")
    public void evictExpired() {
        if (!evictionEnabled) {
            return;
        }
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        stats.entrySet().removeIf(e -> e.getValue().lastAccessTime < cutoff);
        trimSamples(System.currentTimeMillis() - WEEK_MS);
    }

    private void trimSamples(long cutoff) {
        while (samples.size() > MAX_SAMPLES) {
            samples.pollFirst();
        }
        RequestSample first = samples.peekFirst();
        while (first != null && first.timestamp < cutoff) {
            samples.pollFirst();
            first = samples.peekFirst();
        }
    }

    private static long durationForRange(String range) {
        return switch (range) {
            case "day" -> 24 * 60 * 60 * 1000L;
            case "week" -> WEEK_MS;
            default -> 60 * 60 * 1000L;
        };
    }

    private static Double bucketValue(String metric, List<RequestSample> bucket, long intervalMs) {
        if (bucket.isEmpty()) {
            return "errorRate".equals(metric) ? 0.0 : null;
        }
        if ("errorRate".equals(metric)) {
            long errors = bucket.stream().filter(RequestSample::error).count();
            return errors * 100.0 / bucket.size();
        }
        if ("requestsPerSecond".equals(metric)) {
            return bucket.size() / (intervalMs / 1000.0);
        }
        return bucket.stream().mapToLong(RequestSample::durationMs).average().orElse(0);
    }

    private static double percentile(List<Double> sortedValues, double p) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(p * sortedValues.size()) - 1;
        return round(sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1))), 4);
    }

    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private record RequestSample(long timestamp, long durationMs, boolean error) {
    }
}
