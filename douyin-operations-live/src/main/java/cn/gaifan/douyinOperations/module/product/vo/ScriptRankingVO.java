package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品话术排行榜 VO
 * 用于返回排行榜信息
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptRankingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 版本 ID
     */
    private Long versionId;

    /**
     * 版本号
     */
    private Integer versionNumber;

    /**
     * 话术风格
     */
    private String style;

    /**
     * 效果评分（0-100）
     */
    private BigDecimal score;

    /**
     * 评分等级（A/B/C/D/F）
     */
    private String scoreLevel;

    /**
     * 使用次数
     */
    private Integer usageCount;

    /**
     * 转化率（百分比）
     */
    private BigDecimal conversionRate;

    /**
     * 点赞数
     */
    private Integer likesCount;

    /**
     * 评论数
     */
    private Integer commentsCount;

    /**
     * 是否推荐
     */
    private Boolean isRecommended;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;

    /**
     * 排行榜类型（总体/周排行/月排行）
     */
    private String rankingType;
}
