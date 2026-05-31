package cn.gaifan.douyinOperations.module.platform.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GovernancePortImplTest {

    private GovernancePortImpl newGov() {
        var gov = new GovernancePortImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(gov, "jdbcTemplate", null);
        return gov;
    }

    @Test
    void recordEventDoesNotThrow() {
        assertDoesNotThrow(() -> newGov().recordEvent("test", "res", "detail", true));
    }

    @Test
    void recordAiUsageDoesNotThrow() {
        assertDoesNotThrow(() -> newGov().recordAiUsage("test", "model", 100, 5));
    }

    @Test
    void checkQuotaGrantsWhenDBUnavailable() {
        var d = newGov().checkQuota("default", "test");
        assertTrue(d.granted());
    }
}
