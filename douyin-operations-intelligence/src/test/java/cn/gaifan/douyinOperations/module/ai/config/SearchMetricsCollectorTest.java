package cn.gaifan.douyinOperations.module.ai.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SearchMetricsCollectorTest {

    @Test
    void recordLatency_incrementsSloViolationWhenExceedsTarget() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SearchMetricsCollector collector = new SearchMetricsCollector(registry);
        KbRagProperties props = new KbRagProperties();
        props.setSloHybridSearchP95MsTarget(100L);
        ReflectionTestUtils.setField(collector, "kbRagProperties", props);

        collector.recordLatency(50L, 1L);
        assertThat(registry.counter("ai.search.slo.hybrid.violation").count()).isZero();

        collector.recordLatency(150L, 1L);
        assertThat(registry.counter("ai.search.slo.hybrid.violation").count()).isEqualTo(1.0);
    }

    @Test
    void recordLatency_noViolationWhenTargetDisabled() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SearchMetricsCollector collector = new SearchMetricsCollector(registry);
        KbRagProperties props = new KbRagProperties();
        props.setSloHybridSearchP95MsTarget(0L);
        ReflectionTestUtils.setField(collector, "kbRagProperties", props);

        collector.recordLatency(99999L, null);
        assertThat(registry.counter("ai.search.slo.hybrid.violation").count()).isZero();
    }
}
