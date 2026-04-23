package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 风格推荐结果 VO
 *
 * @author Claude Code
 * @since 2026-04-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StyleRecommendationVO {

    /**
     * 推荐的风格代码
     */
    private String styleCode;

    /**
     * 风格名称
     */
    private String styleName;

    /**
     * 推荐置信度（0-1）
     */
    private BigDecimal confidence;

    /**
     * 推荐原因
     */
    private String reason;

    /**
     * 推荐来源
     */
    private String source;  // 'rule_based', 'ml_model', 'hybrid'

    /**
     * 预期评分（基于历史数据预测）
     */
    private BigDecimal expectedScore;

    /**
     * 相似商品列表（协同过滤使用）
     */
    private List<SimilarProduct> similarProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimilarProduct {
        private Long productId;
        private String productName;
        private BigDecimal similarity;
        private String bestStyle;
        private BigDecimal bestStyleScore;
    }
}
