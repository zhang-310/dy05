package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class LiveProductDataVO {
    private Long id;
    private Long sessionId;
    private Long productId;
    private String productName;  // 冗余自 live_product，便于前端展示
    private Integer impressions;
    private Integer clicks;
    private Integer orders;
    private Integer saleQuantity;
    private BigDecimal revenue;
    private Integer refundQuantity;
    private BigDecimal conversionRate;
    private Timestamp syncTime;
    private Timestamp createTime;
    private Timestamp updateTime;
}
