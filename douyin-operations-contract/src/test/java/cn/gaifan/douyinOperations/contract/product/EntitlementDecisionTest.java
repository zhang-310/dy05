package cn.gaifan.douyinOperations.contract.product;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * EntitlementDecision 单元测试
 */
class EntitlementDecisionTest {

    @Test
    void granted() {
        EntitlementDecision d = EntitlementDecision.granted("douyin-ops", "douyin-ops.account-mgmt");
        assertTrue(d.granted());
        assertEquals("douyin-ops", d.productCode());
        assertNull(d.reason());
    }

    @Test
    void denied() {
        EntitlementDecision d = EntitlementDecision.denied("douyin-ops", "douyin-ops.account-mgmt", "配额不足");
        assertFalse(d.granted());
        assertEquals("配额不足", d.reason());
        assertEquals(0, d.quotaRemaining());
    }
}
