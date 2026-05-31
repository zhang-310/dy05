package cn.gaifan.douyinOperations.contract.product;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EntitlementDecisionCompleteTest {

    @Test
    void quotaRemainingIsMinusOneWhenGranted() {
        var d = EntitlementDecision.granted("p", "f");
        assertEquals(-1, d.quotaRemaining());
    }

    @Test
    void quotaRemainingIsZeroWhenDenied() {
        var d = EntitlementDecision.denied("p", "f", "reason");
        assertEquals(0, d.quotaRemaining());
    }

    @Test
    void grantedIsNotNull() {
        var d = EntitlementDecision.granted("p", "f");
        assertNotNull(d.productCode());
        assertNotNull(d.featureCode());
        assertNull(d.reason());
    }

    @Test
    void reasonIsSetOnDenied() {
        var d = EntitlementDecision.denied("x", "y", "over quota");
        assertNotNull(d.reason());
        assertFalse(d.granted());
    }
}
