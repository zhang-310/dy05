package cn.gaifan.douyinOperations.contract.port;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortContractTest {

    @Test
    void liveDataPortRecordsWork() {
        var gmv = new cn.gaifan.douyinOperations.contract.payment.SessionGmvSummary(
                1L, null, 100L, 0L, 100L, 1, 0, null, "test");
        assertEquals(100L, gmv.netGmv());
    }

    @Test
    void paymentPortRecordWorks() {
        var r = new PaymentPort.OrderResult("O-001", 500L, "PAID");
        assertEquals("O-001", r.orderNo());
        assertEquals(500L, r.amount());
    }

    @Test
    void metricPortRecordWorks() {
        var p = new MetricPort.MetricPoint("cpu", 75.5, "server:1", System.currentTimeMillis());
        assertEquals("cpu", p.name());
        assertEquals(75.5, p.value(), 0.01);
        assertTrue(p.timestamp() > 0);
    }

    @Test
    void notificationPortRecordWorks() {
        var n = new NotificationPort.Notification("晨报", "今日GMV: 100万", "wecom", "user-1");
        assertEquals("晨报", n.title());
        assertEquals("wecom", n.channel());
    }

    @Test
    void searchPortRecordWorks() {
        var s = new SearchPort.SearchResult("id-1", "爆款视频", "video", 0.95);
        assertTrue(s.score() > 0.9);
        assertEquals("video", s.type());
    }

    @Test
    void governancePortRecordWorks() {
        // All Port records must be non-null
        var mockResult = cn.gaifan.douyinOperations.contract.product.EntitlementDecision.granted("x", "y");
        assertTrue(mockResult.granted());
    }
}
