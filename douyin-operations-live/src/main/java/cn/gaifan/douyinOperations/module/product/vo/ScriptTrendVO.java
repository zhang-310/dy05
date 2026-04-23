package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品话术效果趋势 VO
 * 用于返回历史评分趋势数据
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 趋势数据点
     */
    private java.util.List<TrendPoint> trendPoints;

    /**
     * 平均评分
     */
    private BigDecimal averageScore;

    /**
     * 最高评分
     */
    private BigDecimal maxScore;

    /**
     * 最低评分
     */
    private BigDecimal minScore;

    /**
     * 趋势方向（上升/下降/稳定）
     */
    private String trendDirection;

    /**
     * 版本 ID
     */
    private Long versionId;

    /**
     * 统计周期（天数）
     */
    private Integer days;

    /**
     * 趋势数据点内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendPoint implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 日期
         */
        private LocalDateTime date;

        /**
         * 评分
         */
        private BigDecimal score;

        /**
         * 评分等级
         */
        private String scoreLevel;

        /**
         * 使用次数
         */
        private Integer usageCount;

        /**
         * 转化率
         */
        private BigDecimal conversionRate;

        /**
         * 点赞数
         */
        private Integer likesCount;
    }
}
