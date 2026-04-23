package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 产品话术效果摘要（按 dy_product_script.id 聚合）
 * 用于话术管理抽屉内展示：使用次数、效果分、引用场次等
 */
@Data
public class ProductScriptEffectSummaryVO {

    /** 使用次数（来自 dy_product_script_usage） */
    private long usageCount;

    /** 平均效果评分（1–5 或业务定义） */
    private BigDecimal avgEffectivenessScore;

    /** 平均转化率（%） */
    private BigDecimal avgConversionRate;

    /** 总销售额 */
    private BigDecimal totalSalesAmount;

    /** 被排品引用条数（live_product.product_script_id = 该话术 id 的条数） */
    private long referenceCount;
}
