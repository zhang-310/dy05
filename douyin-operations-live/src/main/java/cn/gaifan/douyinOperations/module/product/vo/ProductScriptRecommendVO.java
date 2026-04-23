package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品话术推荐返回值对象
 * 包含推荐的话术版本和推荐分数
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductScriptRecommendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 推荐的话术版本列表
     */
    private List<RecommendItem> versions;

    /**
     * 推荐分数列表（与 versions 对应）
     */
    private List<BigDecimal> scores;

    /**
     * 推荐版本项目
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendItem implements Serializable {

        /**
         * 话术版本 ID
         */
        private Long id;

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
        private BigDecimal effectivenessScore;

        /**
         * 使用次数
         */
        private Integer usageCount;

        /**
         * 转化率
         */
        private BigDecimal conversionRate;

        /**
         * 推荐分数
         */
        private BigDecimal recommendScore;
    }
}
