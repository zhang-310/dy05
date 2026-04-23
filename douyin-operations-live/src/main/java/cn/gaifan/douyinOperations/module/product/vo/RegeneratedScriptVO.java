package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 重新生成的话术版本返回 VO
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegeneratedScriptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 生成版本 ID
     */
    private Long id;

    /**
     * 话术版本 ID
     */
    private Long scriptVersionId;

    /**
     * 建议 ID
     */
    private Long suggestionId;

    /**
     * 生成的话术风格
     */
    private String generationStyle;

    /**
     * 重新生成的话术内容
     */
    private String regeneratedContent;

    /**
     * AI 质量评分（0-10）
     */
    private BigDecimal aiQualityScore;

    /**
     * 估计指标
     */
    private EstimatedMetrics estimatedMetrics;

    /**
     * 是否已应用
     */
    private Boolean isApplied;

    /**
     * 应用时间
     */
    private LocalDateTime appliedAt;

    /**
     * 审批状态
     */
    private String approvalStatus;

    /**
     * 审批人 ID
     */
    private Long approvedBy;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 审批备注
     */
    private String approvalNotes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 估计指标嵌套 VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EstimatedMetrics implements Serializable {
        private BigDecimal interactionRate;         // 估计互动率
        private BigDecimal conversionRate;          // 估计转化率
        private Integer estimatedFanGrowth;         // 估计粉丝增长
    }
}
