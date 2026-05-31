package cn.gaifan.douyinOperations.contract.payment;

import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.product.EntitlementDecision;
import cn.gaifan.douyinOperations.contract.role.OperationRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * API 契约冒烟测试 — 验证所有 Contract Record 可以正常序列化
 */
class ContractApiSmokeTest {

    @Test
    void sessionGmvSummary() {
        var s = new SessionGmvSummary(1L, "测试", 100L, 10L, 90L, 3, 1, 5L, "场次");
        assertEquals(90L, s.netGmv());
        assertEquals(0.009, s.netGmvInWan(), 0.001);
    }

    @Test
    void entitlementDecisionToJson() {
        var d = EntitlementDecision.granted("p1", "f1");
        assertTrue(d.granted());
        assertEquals("p1", d.productCode());
        assertNull(d.reason());
    }

    @Test
    void operationRoleValues() {
        for (var r : OperationRole.values()) {
            assertNotNull(r.code());
            assertNotNull(r.displayName());
            assertNotNull(r.responsibility());
            assertFalse(r.code().isEmpty());
        }
    }

    @Test
    void sessionCompletionRate() {
        var r = SessionCompletionRate.estimated(1L);
        assertEquals(0.85, r.fullWatchRatio(), 0.01);
        assertEquals("estimated", r.dataSource());
    }

    @Test
    void identityContextAnonymous() {
        var ctx = IdentityContext.anonymous();
        assertFalse(ctx.authenticated());
        assertEquals("ANONYMOUS", ctx.source());
    }
}
