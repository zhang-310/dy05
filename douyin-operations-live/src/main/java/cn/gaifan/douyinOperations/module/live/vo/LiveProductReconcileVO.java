package cn.gaifan.douyinOperations.module.live.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 单场支付 vs live_product 营收对账结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveProductReconcileVO {

    private Long sessionId;
    /** 支付订单侧合计（PAID/SHIPPED/COMPLETED） */
    private BigDecimal paymentTotal;
    /** live_product.revenue 合计 */
    private BigDecimal liveProductRevenueTotal;
    /** paymentTotal - liveProductRevenueTotal */
    private BigDecimal diff;

    @Builder.Default
    private List<LiveProductReconcileRowVO> rows = new ArrayList<>();
}
