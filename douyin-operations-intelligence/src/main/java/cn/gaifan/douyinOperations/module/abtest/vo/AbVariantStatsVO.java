package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A/B 测试变体统计 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbVariantStatsVO {
    private Long variantId;
    private String variantName;
    private String variantType;
    private Long viewCount;
    private Long conversionCount;
    private Double conversionRate;
    private Double avgValue;
}
