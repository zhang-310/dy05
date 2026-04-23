package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 商品话术对比 VO
 * 用于返回版本对比、风格对比等信息
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptComparisonVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 对比的版本列表
     */
    private List<ComparisonItem> versions;

    /**
     * 对比维度数据
     */
    private Map<String, Object> comparisons;

    /**
     * 对比类型（version_compare/style_compare 等）
     */
    private String comparisonType;

    /**
     * 对比的产品 ID
     */
    private Long productId;

    /**
     * 对比建议
     */
    private String recommendation;

    /**
     * 对比时间戳
     */
    private Long timestamp;

    /**
     * 对比项目内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComparisonItem implements Serializable {

        private static final long serialVersionUID = 1L;

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
         * 效果评分
         */
        private Double score;

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
        private Double conversionRate;

        /**
         * 点赞数
         */
        private Integer likesCount;

        /**
         * 评论数
         */
        private Integer commentsCount;
    }
}
