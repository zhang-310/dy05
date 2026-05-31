package cn.gaifan.douyinOperations.contract.bff;

/**
 * BFF 聚合响应 — 概览仪表盘
 */
public record BffDashboardVO(
        Object userProfile,
        Object platformOverview,
        Object aiUsageSummary,
        Object recentActivity
) {}
