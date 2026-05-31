package cn.gaifan.douyinOperations.contract.role;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoleDashboardProviderTest {

    @Test
    void dailyBriefingRecord() {
        var db = new RoleDashboardProvider.DailyBriefing(
                "2026-05-30", 3, 150000L, 5, "爆款视频标题", "紧急: 库存不足");
        assertEquals(3, db.todaySessions());
        assertEquals(150000L, db.yesterdayGmv());
        assertNotNull(db.urgentNotice());
    }

    @Test
    void zeroValuesAllowed() {
        var db = new RoleDashboardProvider.DailyBriefing("", 0, 0L, 0, "", "");
        assertEquals(0, db.todaySessions());
        assertEquals(0L, db.yesterdayGmv());
    }
}
