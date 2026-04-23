package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

/**
 * 短视频 Dashboard 服务：统计、趋势、项目列表（含进度）
 */
public interface ShortVideoDashboardService {

    /**
     * 数据概览：总视频数、总播放量、总成本、ROI
     */
    Map<String, Object> getStats(Long ownerId);

    /**
     * 播放量趋势（最近 7 天）
     */
    List<Map<String, Object>> getTrend(Long ownerId, int days);

    /**
     * 我的项目列表（含进度、阶段）
     */
    List<Map<String, Object>> getProjectsWithProgress(Long ownerId, String status, int page, int rows);

    /**
     * 成本分解（单项目或汇总）
     * @param projectId 为 null 时返回用户全部项目汇总
     */
    Map<String, Object> getCostBreakdown(Long ownerId, Long projectId);
}
