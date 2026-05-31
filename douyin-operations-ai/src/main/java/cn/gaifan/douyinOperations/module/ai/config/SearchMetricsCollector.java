package cn.gaifan.douyinOperations.module.ai.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * P1 检索性能监控：记录延迟、QPS，并注册到 Prometheus
 */
@Component
public class SearchMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(SearchMetricsCollector.class);

    private static final int BUFFER_SIZE = 200;
    private final long[] latencies = new long[BUFFER_SIZE];
    private int writeIdx = 0;
    private int count = 0;
    private final Object lock = new Object();

    private final AtomicLong totalRequests = new AtomicLong(0);
    private volatile long lastResetTime = System.currentTimeMillis();

    private final Timer searchTimer;
    private final Counter searchCounter;
    private final Counter sloViolationCounter;

    @Autowired(required = false)
    private KbRagProperties kbRagProperties;

    public SearchMetricsCollector(@Autowired(required = false) MeterRegistry registry) {
        if (registry != null) {
            searchTimer = Timer.builder("ai.search.duration")
                    .tag("type", "hybrid")
                    .description("AI knowledge search latency")
                    .register(registry);
            searchCounter = Counter.builder("ai.search.total")
                    .tag("type", "hybrid")
                    .description("AI knowledge search count")
                    .register(registry);
            sloViolationCounter = Counter.builder("ai.search.slo.hybrid.violation")
                    .description("Hybrid search requests exceeding app.ai.kb.rag.slo-hybrid-search-p95-ms-target")
                    .register(registry);
        } else {
            searchTimer = null;
            searchCounter = null;
            sloViolationCounter = null;
        }
    }

    public void recordLatency(long ms) {
        recordLatency(ms, null);
    }

    /**
     * @param kbId 用于 SLO 超标日志定位，可为 null
     */
    public void recordLatency(long ms, Long kbId) {
        totalRequests.incrementAndGet();
        if (searchCounter != null) searchCounter.increment();
        if (searchTimer != null) searchTimer.record(ms, java.util.concurrent.TimeUnit.MILLISECONDS);
        long targetMs = kbRagProperties != null ? kbRagProperties.getSloHybridSearchP95MsTarget() : 0L;
        if (targetMs > 0 && ms > targetMs && sloViolationCounter != null) {
            sloViolationCounter.increment();
            log.warn("混合检索超过 SLO：kbId={}, durationMs={}, targetMs={}", kbId, ms, targetMs);
        }
        synchronized (lock) {
            latencies[writeIdx] = ms;
            writeIdx = (writeIdx + 1) % BUFFER_SIZE;
            if (count < BUFFER_SIZE) count++;
        }
    }

    public void recordVectorEmpty() {}

    public void recordFallbackEsOnly() {}

    public void recordFallbackMilvusOnly() {}

    public void recordHydeUsed() {}

    public java.util.Map<String, Object> getStats() {
        long[] copy;
        int n;
        synchronized (lock) {
            copy = Arrays.copyOf(latencies, count);
            n = count;
        }
        java.util.Map<String, Object> out = new java.util.HashMap<>();
        out.put("totalRequests", totalRequests.get());
        out.put("sampleCount", n);
        long elapsedSec = Math.max(1, (System.currentTimeMillis() - lastResetTime) / 1000);
        out.put("qps", String.format("%.2f", totalRequests.get() / (double) elapsedSec));

        if (n > 0) {
            Arrays.sort(copy);
            int p50Idx = (int) (n * 0.5);
            if (p50Idx >= n) p50Idx = n - 1;
            int p95Idx = (int) (n * 0.95);
            if (p95Idx >= n) p95Idx = n - 1;
            int p99Idx = (int) (n * 0.99);
            if (p99Idx >= n) p99Idx = n - 1;
            out.put("p50Ms", copy[p50Idx]);
            out.put("p95Ms", copy[p95Idx]);
            out.put("p99Ms", copy[p99Idx]);
            out.put("minMs", copy[0]);
            out.put("maxMs", copy[n - 1]);
            int lt100 = 0, r100_500 = 0, r500_1000 = 0, gt1000 = 0;
            for (long v : copy) {
                if (v < 100) lt100++;
                else if (v < 500) r100_500++;
                else if (v < 1000) r500_1000++;
                else gt1000++;
            }
            out.put("latencyDistribution", java.util.Map.of(
                    "lt100ms", Math.round(100.0 * lt100 / n),
                    "r100_500ms", Math.round(100.0 * r100_500 / n),
                    "r500_1000ms", Math.round(100.0 * r500_1000 / n),
                    "gt1000ms", Math.round(100.0 * gt1000 / n)
            ));
        } else {
            out.put("p50Ms", 0);
            out.put("p95Ms", 0);
            out.put("p99Ms", 0);
            out.put("latencyDistribution", java.util.Map.of("lt100ms", 0, "r100_500ms", 0, "r500_1000ms", 0, "gt1000ms", 0));
        }
        return out;
    }
}
