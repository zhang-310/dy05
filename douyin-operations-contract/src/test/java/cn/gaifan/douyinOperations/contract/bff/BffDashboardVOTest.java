package cn.gaifan.douyinOperations.contract.bff;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class BffDashboardVOTest {

    @Test
    void dashboardRecord() {
        var d = new BffDashboardVO(
                Map.of("userId", 1), Map.of("products", 6),
                Map.of("calls", 100), java.util.Collections.emptyList());
        assertNotNull(d.userProfile());
        assertNotNull(d.platformOverview());
        assertNotNull(d.aiUsageSummary());
        assertNotNull(d.recentActivity());
    }
}
