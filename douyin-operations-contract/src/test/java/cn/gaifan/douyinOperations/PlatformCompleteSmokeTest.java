package cn.gaifan.douyinOperations;

import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.identity.IdentityHeaders;
import cn.gaifan.douyinOperations.contract.payment.SessionGmvSummary;
import cn.gaifan.douyinOperations.contract.product.EntitlementDecision;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.role.OperationRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 平台基座完整冒烟测试 — 一次性验证所有 Contract 组件
 */
class PlatformCompleteSmokeTest {

    @Test
    void identityFlow() {
        var anon = IdentityContext.anonymous();
        assertFalse(anon.authenticated());

        var auth = IdentityContext.authenticated("t1", 1L, "admin", 1L, "WEB", "t", "r");
        assertTrue(auth.authenticated());
        assertEquals("t1", auth.tenantId());
    }

    @Test
    void productFlow() {
        assertNotNull(ProductCode.DOUYIN_OPS);
        assertTrue(ProductCode.DOUYIN_OPS.contains("-"));
    }

    @Test
    void featureFlow() {
        assertNotNull(FeatureCode.AI_CHAT);
        assertTrue(FeatureCode.AI_CHAT.startsWith("ai."));
    }

    @Test
    void entitlementFlow() {
        var granted = EntitlementDecision.granted("p", "f");
        assertTrue(granted.granted());
        assertEquals(-1, granted.quotaRemaining());

        var denied = EntitlementDecision.denied("p", "f", "no quota");
        assertFalse(denied.granted());
        assertEquals("no quota", denied.reason());
    }

    @Test
    void gmvFlow() {
        var gmv = new SessionGmvSummary(1L, null, 10000L, 1000L, 9000L, 5, 1, null, "test");
        assertEquals(9000L, gmv.netGmv());
        assertEquals(0.9, gmv.netGmvInWan(), 0.01);
    }

    @Test
    void roleFlow() {
        assertEquals(6, OperationRole.values().length);
        assertEquals("主播", OperationRole.ANCHOR.displayName());
        assertEquals("中控人员", OperationRole.DIRECTOR.displayName());
    }

    @Test
    void headersFlow() {
        assertNotNull(IdentityHeaders.X_TENANT_ID);
        assertNotNull(IdentityHeaders.X_TRACE_ID);
        assertNotNull(IdentityHeaders.X_SERVICE_AUTH);
    }
}
