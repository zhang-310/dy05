package cn.gaifan.douyinOperations.contract.payment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionGmvSummaryTest {

    @Test
    void netGmvCalculation() {
        SessionGmvSummary s = new SessionGmvSummary(1L, null, 50000L, 5000L, 45000L, 10, 2, null, "测试场次");
        assertEquals(45000L, s.netGmv());
        assertEquals(4.5, s.netGmvInWan(), 0.01);
    }

    @Test
    void zeroGmv() {
        SessionGmvSummary s = new SessionGmvSummary(1L, null, 0L, 0L, 0L, 0, 0, null, null);
        assertEquals(0L, s.netGmv());
        assertEquals(0.0, s.netGmvInWan(), 0.01);
    }
}

class SessionCompletionRateTest {

    @Test
    void estimatedFallback() {
        var rate = SessionCompletionRate.estimated(1L);
        assertEquals(0.85, rate.fullWatchRatio(), 0.01);
        assertEquals("estimated", rate.dataSource());
    }
}
