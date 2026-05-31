package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.auth.EntitlementProvider;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class EntitlementServiceImplTest {

    private final EntitlementProvider service = new EntitlementServiceImpl(
            Mockito.mock(PlatformEntitlementService.class));

    @Test
    void nullUserDenied() {
        var d = service.check("default", null, "douyin-ops", "douyin-ops.account-mgmt");
        assertFalse(d.granted());
        assertEquals("未登录", d.reason());
    }

    @Test
    void demoFeatureGranted() {
        var d = service.check("default", 1L, "douyin-ops", "douyin-ops.account-mgmt");
        assertTrue(d.granted());
    }

    @Test
    void demoChatGranted() {
        var d = service.check("default", 1L, "douyin-ops", "ai.chat");
        assertTrue(d.granted());
    }

    @Test
    void unknownFeatureGrantedInDemo() {
        var d = service.check("default", 1L, "douyin-ops", "unknown.feature");
        assertTrue(d.granted());
    }
}
