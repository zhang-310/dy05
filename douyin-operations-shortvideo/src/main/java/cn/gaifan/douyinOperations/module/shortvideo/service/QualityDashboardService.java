package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

/**
 * 质量仪表板服务 (Phase 6.3)
 * 基于 sv_generation_log 聚合统计
 */
public interface QualityDashboardService {

    /** 概览：本周发布数、平均评分、成功率 */
    Map<String, Object> getOverview(Long ownerId);

    /** 近 N 天质量评分趋势 (按日) */
    List<Map<String, Object>> getQualityTrend(Long ownerId, int days);

    /** 模型效果排名 (ai_provider → 平均分) */
    List<Map<String, Object>> getModelRanking(Long ownerId, int days);

    /** 运镜效果排名 (camera_type → 平均分) */
    List<Map<String, Object>> getCameraRanking(Long ownerId, int days);

    /** 本周 AI 反思/改进建议 (占位) */
    List<String> getAiReflections(Long ownerId);
}
