package cn.gaifan.douyinOperations.module.live.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单场支付与排品营收对账 — 单行（按商品库 productId）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveProductReconcileRowVO {

    private Long productId;
    private String productName;
    /** 支付侧：本场次该商品的 actual_amount 合计 */
    private BigDecimal paymentSum;
    /** 排品侧：live_product.revenue */
    private BigDecimal liveRevenue;
    /** paymentSum - liveRevenue */
    private BigDecimal lineDiff;
}
