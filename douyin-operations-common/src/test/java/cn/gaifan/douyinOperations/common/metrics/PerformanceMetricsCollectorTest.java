package cn.gaifan.douyinOperations.common.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceMetricsCollectorTest {

    @Test
    void evictExpired_shouldSkipWhenDisabled() {
        PerformanceMetricsCollector collector = new PerformanceMetricsCollector();
        ReflectionTestUtils.setField(collector, "evictionEnabled", false);
        collector.record("test.endpoint", 12L, false);

        collector.evictExpired();

        List<java.util.Map<String, Object>> slow = collector.getTopSlow(10);
        assertThat(slow).hasSize(1);
        assertThat(slow.get(0)).containsEntry("endpoint", "test.endpoint");
    }

    @Test
    void snapshotAndTrend_shouldExposeResponseTimeErrorRateAndRps() {
        PerformanceMetricsCollector collector = new PerformanceMetricsCollector();
        collector.requestStarted();
        collector.record("GET /api/a", 100L, false);
        collector.record("GET /api/b", 300L, true);
        collector.requestFinished();

        java.util.Map<String, Object> snapshot = collector.getWindowSnapshot();
        assertThat(snapshot).containsEntry("requestCount", 2L);
        assertThat((Double) snapshot.get("responseTime")).isEqualTo(200.0);
        assertThat((Double) snapshot.get("errorRate")).isEqualTo(50.0);
        assertThat((Integer) snapshot.get("activeConnections")).isEqualTo(0);

        java.util.Map<String, Object> trend = collector.getTrend("responseTime", "hour", 2);
        assertThat(trend).containsEntry("metricName", "responseTime");
        assertThat((java.util.List<?>) trend.get("dataPoints")).isNotEmpty();
    }
}
