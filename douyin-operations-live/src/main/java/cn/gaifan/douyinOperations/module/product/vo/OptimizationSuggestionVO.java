package cn.gaifan.douyinOperations.module.product.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优化建议返回和创建 VO
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationSuggestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 建议 ID
     */
    private Long id;

    /**
     * 话术版本 ID
     */
    private Long scriptVersionId;

    /**
     * 分析结果 ID
     */
    private Long analysisResultId;

    /**
     * 建议分类（CONTENT/PACING/STYLE/TOPIC）
     */
    @NotBlank(message = "建议分类不能为空")
    private String category;

    /**
     * 优先级（LOW/MEDIUM/HIGH/CRITICAL）
     */
    @NotBlank(message = "优先级不能为空")
    private String priority;

    /**
     * 建议内容
     */
    @NotBlank(message = "建议内容不能为空")
    private String suggestionContent;

    /**
     * 关联的弱点类型
     */
    private String relatedWeakPoint;

    /**
     * 期望改进
     */
    private ExpectedImprovement expectedImprovement;

    /**
     * 采纳状态
     */
    private String adoptionStatus;

    /**
     * 采纳时间
     */
    private LocalDateTime adoptedAt;

    /**
     * 采纳备注
     */
    private String adoptionNotes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 期望改进嵌套 VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpectedImprovement implements Serializable {
        @JsonProperty("interactionRateIncrease")
        private BigDecimal interactionRateIncrease;    // 互动率提升
        @JsonProperty("conversionRateIncrease")
        private BigDecimal conversionRateIncrease;      // 转化率提升
        @JsonProperty("confidence")
        private BigDecimal confidence;                   // 置信度（0-1）
    }
}
