package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.auth.EntitlementProvider;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class EntitlementServiceImplCompleteTest {

    private final EntitlementProvider service = new EntitlementServiceImpl(
            Mockito.mock(PlatformEntitlementService.class));

    @Test
    void allDemoFeaturesGranted() {
        String[] features = {"douyin-ops.account-mgmt", "douyin-ops.video-analysis",
                "video-insight.breakdown", "ai.chat", "ai.generation", "knowledge-base.rag"};
        for (String f : features) {
            var d = service.check("default", 1L, "douyin-ops", f);
            assertTrue(d.granted(), "Feature " + f + " should be granted");
        }
    }

    @Test
    void nullUserAlwaysDenied() {
        var d = service.check("default", null, "douyin-ops", "any-feature");
        assertFalse(d.granted());
        assertEquals("未登录", d.reason());
    }

    @Test
    void differentTenantsCanCheck() {
        var d1 = service.check("tenant-1", 100L, "douyin-ops", "ai.chat");
        var d2 = service.check("tenant-2", 200L, "douyin-ops", "ai.chat");
        assertTrue(d1.granted());
        assertTrue(d2.granted());
    }
}
