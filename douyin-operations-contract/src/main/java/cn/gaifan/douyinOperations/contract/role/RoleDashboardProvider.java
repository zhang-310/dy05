package cn.gaifan.douyinOperations.contract.role;

/**
 * 角色专属 Dashboard 数据 SPI
 *
 * 每个角色看到不同的首页数据。
 */
public interface RoleDashboardProvider {

    /** 返回指定角色的首页聚合数据 */
    Object buildDashboard(String roleCode, Long userId);

    /** 每日晨报（7:30 推送企微） */
    DailyBriefing buildDailyBriefing();

    record DailyBriefing(
            String date,
            int todaySessions,     // 今日场次数
            long yesterdayGmv,     // 昨日 GMV（分）
            int pendingApprovals,  // 待审核数
            String topVideoTitle,  // 最佳短视频
            String urgentNotice    // 紧急通知
    ) {}
}
