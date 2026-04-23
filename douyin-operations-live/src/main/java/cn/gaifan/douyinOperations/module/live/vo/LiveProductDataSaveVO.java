package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class LiveProductDataSaveVO {
    @NotNull(message = "场次 ID 不能为空")
    private Long sessionId;
    @NotNull(message = "商品 ID 不能为空")
    private Long productId;
    private Integer impressions;
    private Integer clicks;
    private Integer orders;
    private Integer saleQuantity;
    private BigDecimal revenue;
    private Integer refundQuantity;
    private BigDecimal conversionRate;
}
