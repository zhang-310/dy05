package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.port.MetricPort;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetricPortImplTest {

    private final MetricPort metric = new MetricPortImpl();

    @Test
    void recordAndQuery() {
        metric.record("gmv", 15000.0, "session:1");
        metric.record("gmv", 25000.0, "session:2");
        var results = metric.query("gmv", "default", 60);
        assertEquals(2, results.size());
    }

    @Test
    void queryOldDataReturnsEmpty() {
        metric.record("test", 1.0, "old");
        var results = metric.query("test", "default", -1);
        assertTrue(results.isEmpty());
    }

    @Test
    void unknownMetricReturnsEmpty() {
        var results = metric.query("nonexistent", "default", 60);
        assertTrue(results.isEmpty());
    }
}
