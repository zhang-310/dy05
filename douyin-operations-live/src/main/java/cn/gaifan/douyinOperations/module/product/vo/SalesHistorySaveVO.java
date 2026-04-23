package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class SalesHistorySaveVO {
    private Long id;
    @NotNull(message = "商品 ID 不能为空")
    private Long productId;
    private Long saleQuantity;
    @NotNull(message = "销售金额不能为空")
    private BigDecimal saleAmount;
    /** 销售时间（ISO 8601 或 yyyy-MM-dd HH:mm:ss），前端传入字符串 */
    private String saleTime;
    private String channelSource;
    private String sessionId;
}
