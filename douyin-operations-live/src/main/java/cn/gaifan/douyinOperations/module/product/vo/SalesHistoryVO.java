package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class SalesHistoryVO {
    private Long id;
    private Long productId;
    private Long saleQuantity;
    private BigDecimal saleAmount;
    private Timestamp saleTime;
    private String channelSource;
    private String sessionId;
    private Timestamp createTime;
}
