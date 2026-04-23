package cn.gaifan.douyinOperations.module.live.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A/B 测试闭环反馈分析服务
 */
public interface LiveAbTestAnalysisService {

    /**
     * 记录 A/B 测试结果
     *
     * @param sessionId        场次 ID
     * @param experimentKey    实验标识
     * @param variant          变体标识
     * @param style            话术风格
     * @param effectivenessScore 效果评分
     * @param conversionRate   转化率
     * @param interactionRate  互动率
     * @param sampleSize       样本量
     * @param confidence       置信度
     * @param ownerId          所属用户 ID
     */
    void recordResult(Long sessionId, String experimentKey, String variant, String style,
                      BigDecimal effectivenessScore, BigDecimal conversionRate,
                      BigDecimal interactionRate, Integer sampleSize,
                      BigDecimal confidence, Long ownerId);

    /**
     * 获取推荐话术风格
     *
     * @param ownerId 用户 ID（作为账号维度查询）
     * @return { style, confidence, sampleSize }
     */
    Map<String, Object> getRecommendedStyle(Long ownerId);

    /**
     * 获取实验摘要
     *
     * @param experimentKey 实验标识
     * @return 实验下各变体的结果列表
     */
    List<Map<String, Object>> getExperimentSummary(String experimentKey);

    /**
     * 计算两个变体之间的统计显著性（Z-test for proportions）
     *
     * @param experimentKey 实验标识
     * @return 包含 significant, pValue, zScore, winner, confidenceLevel 等
     */
    Map<String, Object> calculateSignificance(String experimentKey);
}
