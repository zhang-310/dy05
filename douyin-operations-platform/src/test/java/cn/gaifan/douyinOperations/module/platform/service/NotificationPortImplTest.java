package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.port.NotificationPort;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationPortImplTest {

    private final NotificationPort port = new NotificationPortImpl();

    @Test
    void sendDoesNotThrow() {
        var notif = new NotificationPort.Notification("测试", "内容", "web", "user-1");
        assertDoesNotThrow(() -> port.send(notif));
    }

    @Test
    void listPendingInitiallyEmpty() {
        var list = port.listPending("default", "user-1");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void markReadDoesNotThrow() {
        assertDoesNotThrow(() -> port.markRead(1L));
    }
}
