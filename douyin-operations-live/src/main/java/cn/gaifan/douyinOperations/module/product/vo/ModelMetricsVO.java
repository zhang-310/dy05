package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型性能指标 VO
 *
 * @author Claude Code
 * @since 2026-04-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelMetricsVO {

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 模型类型
     */
    private String modelType;

    /**
     * 模型版本
     */
    private String modelVersion;

    /**
     * 训练样本数
     */
    private Integer trainingSamples;

    /**
     * 准确率
     */
    private BigDecimal accuracy;

    /**
     * 精确率
     */
    private BigDecimal precision;

    /**
     * 召回率
     */
    private BigDecimal recall;

    /**
     * F1分数
     */
    private BigDecimal f1Score;

    /**
     * Top-3命中率
     */
    private BigDecimal top3HitRate;

    /**
     * 平均评分提升
     */
    private BigDecimal avgScoreImprovement;

    /**
     * 训练时间
     */
    private LocalDateTime trainedAt;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 最近30天反馈数量
     */
    private Long recentFeedbackCount;

    /**
     * 推荐总次数
     */
    private Long totalRecommendations;

    /**
     * 模型状态
     */
    private String status;  // 'active', 'training', 'outdated'
}
