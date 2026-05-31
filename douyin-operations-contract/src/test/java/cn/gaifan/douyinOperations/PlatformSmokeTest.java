package cn.gaifan.douyinOperations;

import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.product.EntitlementDecision;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.contract.role.OperationRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成冒烟测试 — 验证基座核心链路完整性
 */
class PlatformSmokeTest {

    @Test
    void identityContextCreation() {
        var anon = IdentityContext.anonymous();
        assertFalse(anon.authenticated());
        assertEquals("default", anon.tenantId());

        var auth = IdentityContext.authenticated("org-1", 100L, "admin", null, "WEB", "t1", "r1");
        assertTrue(auth.authenticated());
        assertEquals("admin", auth.roleCode());
    }

    @Test
    void productCodesAvailable() {
        assertNotNull(ProductCode.DOUYIN_OPS);
        assertEquals(6, OperationRole.values().length); // 6 角色
    }

    @Test
    void entitlementDecisionFlow() {
        var granted = EntitlementDecision.granted("douyin-ops", "douyin-ops.account-mgmt");
        assertTrue(granted.granted());

        var denied = EntitlementDecision.denied("douyin-ops", "douyin-ops.account-mgmt", "配额不足");
        assertFalse(denied.granted());
        assertEquals("配额不足", denied.reason());
    }

    @Test
    void operationRolesComplete() {
        assertEquals(6, OperationRole.class.getEnumConstants().length);
        assertEquals("主播", OperationRole.ANCHOR.displayName());
        assertEquals("中控人员", OperationRole.DIRECTOR.displayName());
    }
}
