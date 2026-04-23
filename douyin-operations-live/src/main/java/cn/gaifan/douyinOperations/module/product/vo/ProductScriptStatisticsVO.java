package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 产品话术统计信息 VO
 */
@Data
public class ProductScriptStatisticsVO {

    /** 话术 ID */
    private Long productScriptId;

    /** 使用总次数 */
    private Long totalUsageCount;

    /** 平均效果评分 */
    private BigDecimal averageEffectivenessScore;

    /** 平均转化率 */
    private BigDecimal averageConversionRate;

    /** 总销售额 */
    private BigDecimal totalSalesAmount;

    /** 最近使用时间 */
    private java.sql.Timestamp lastUsedTime;
}
