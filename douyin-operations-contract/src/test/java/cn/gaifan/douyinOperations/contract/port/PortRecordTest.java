package cn.gaifan.douyinOperations.contract.port;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaymentPortTest {

    @Test
    void orderResultRecord() {
        var r = new PaymentPort.OrderResult("ORDER-001", 1000L, "PAID");
        assertEquals("ORDER-001", r.orderNo());
        assertEquals(1000L, r.amount());
        assertEquals("PAID", r.status());
    }
}

class SearchPortTest {

    @Test
    void searchResultRecord() {
        var r = new SearchPort.SearchResult("id1", "标题", "video", 0.95);
        assertEquals("id1", r.id());
        assertEquals("video", r.type());
        assertTrue(r.score() > 0.9);
    }
}

class MetricPortTest {

    @Test
    void metricPointRecord() {
        var p = new MetricPort.MetricPoint("gmv", 50000.0, "session:1", System.currentTimeMillis());
        assertEquals("gmv", p.name());
        assertEquals(50000.0, p.value(), 0.01);
    }
}

class NotificationPortTest {

    @Test
    void notificationRecord() {
        var n = new NotificationPort.Notification("标题", "内容", "web", "user-1");
        assertEquals("web", n.channel());
        assertEquals("标题", n.title());
    }
}
