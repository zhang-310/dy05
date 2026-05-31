package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Map;

/**
 * 进化 ROI 监控服务：入库率、自动降频
 */
public interface EvolveRoiService {

    /** 入库率阈值：低于此值连续 N 轮则降频 */
    double LOW_STORAGE_THRESHOLD = 0.30;
    /** 恢复阈值：高于此值连续 N 轮则恢复 */
    double RECOVERY_THRESHOLD = 0.50;
    /** 降频触发轮数 */
    int LOW_ROUNDS_TO_THROTTLE = 5;
    /** 恢复轮数 */
    int RECOVERY_ROUNDS_TO_RESTORE = 3;

    /**
     * 获取进化 ROI 指标（最近 N 轮）
     */
    Map<String, Object> getRoiMetrics(int lastRounds);

    /**
     * 是否应执行本轮进化（降频时返回 false）
     */
    boolean shouldRun();

    /**
     * 记录本轮进化结果，用于降频/恢复判断
     */
    void recordRun(int generatedCount, int indexedCount);

    /**
     * 知识引用率：30 天内被 ai_call_log 引用的进化知识 chunk 数 / 进化知识总 chunk 数
     *
     * @return 0~1，无进化知识时返回 -1
     */
    double getKnowledgeRefRate();
}
