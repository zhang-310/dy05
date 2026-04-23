package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 已结束场次时间窗口内，按商品汇总的 GMV 行（与 live_product.revenue 口径一致）
 */
@Data
public class ProductGmvRowVO {

    private Long productId;
    private String productName;
    private Long sessionCount;
    private BigDecimal totalGmv;
}
