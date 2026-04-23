package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品话术效果分析 VO
 * 用于返回效果分析汇总信息
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptEffectivenessAnalysisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总体趋势（上升/下降/稳定）
     */
    private String overallTrend;

    /**
     * 评分变化（相比前期）
     */
    private BigDecimal scoreChange;

    /**
     * 评分变化百分比
     */
    private BigDecimal scoreChangePercent;

    /**
     * 趋势开始日期
     */
    private LocalDateTime trendStartDate;

    /**
     * 趋势结束日期
     */
    private LocalDateTime trendEndDate;

    /**
     * 当前评分
     */
    private BigDecimal currentScore;

    /**
     * 当前评分等级
     */
    private String currentScoreLevel;

    /**
     * 推荐行动
     */
    private String recommendation;

    /**
     * 分析更新时间
     */
    private LocalDateTime analysisTime;
}
