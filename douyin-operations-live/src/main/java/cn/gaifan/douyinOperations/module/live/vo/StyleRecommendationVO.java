package cn.gaifan.douyinOperations.module.live.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 智能风格推荐：基于历史效果（effectiveness_score）聚合按风格排序
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StyleRecommendationVO {
    private String styleCode;
    private BigDecimal avgEffectiveness;
    private Long usageCount;
    private Integer sessionCount;
}
