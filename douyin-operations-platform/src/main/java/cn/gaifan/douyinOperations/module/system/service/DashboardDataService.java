package cn.gaifan.douyinOperations.module.system.service;

import cn.gaifan.douyinOperations.module.system.vo.DashboardDataVO;

/**
 * 仪表板数据服务
 */
public interface DashboardDataService {

    /**
     * 获取系统整体统计
     */
    DashboardDataVO getSystemOverview();

    /**
     * 获取实时告警数据
     */
    DashboardDataVO getRealtimeAlerts();

    /**
     * 获取性能指标趋势
     */
    DashboardDataVO getPerformanceTrends();

    /**
     * 获取日志聚合统计
     */
    DashboardDataVO getLogStatistics();

    /**
     * 获取链路追踪摘要
     */
    DashboardDataVO getTracesSummary();

    /**
     * 获取健康检查状态
     */
    DashboardDataVO getHealthStatus();
}
