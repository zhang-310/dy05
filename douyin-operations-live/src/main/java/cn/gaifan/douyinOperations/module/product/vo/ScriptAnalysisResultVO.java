package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 话术分析结果返回 VO
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptAnalysisResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分析结果 ID
     */
    private Long id;

    /**
     * 话术版本 ID
     */
    private Long scriptVersionId;

    /**
     * 综合评分（0-100）
     */
    private BigDecimal overallScore;

    /**
     * 效果指标
     */
    private EffectivenessMetrics effectivenessMetrics;

    /**
     * 弱点列表
     */
    private List<WeakPoint> weakPoints;

    /**
     * 风格识别结果
     */
    private StyleProfile styleProfile;

    /**
     * 分析类型
     */
    private String analysisType;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 效果指标嵌套 VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EffectivenessMetrics implements Serializable {
        private BigDecimal interactionRate;      // 互动率 %
        private BigDecimal conversionRate;       // 转化率 %
        private Integer fanGrowth;               // 粉丝增长数
        private BigDecimal commentSentiment;     // 评论正面率 0-1
    }

    /**
     * 弱点嵌套 VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeakPoint implements Serializable {
        private String type;                     // 弱点类型
        private String timeRange;                // 时间范围
        private String severity;                 // 严重程度（LOW/MEDIUM/HIGH）
        private String description;              // 描述
    }

    /**
     * 风格识别嵌套 VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StyleProfile implements Serializable {
        private String dominantStyle;            // 主导风格
        private Map<String, BigDecimal> styleScores;  // 各风格得分
    }
}
