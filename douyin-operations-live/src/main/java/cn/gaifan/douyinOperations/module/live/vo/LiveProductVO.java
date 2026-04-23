package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播产品关系 VO
 */
@Data
public class LiveProductVO {

    private Long id;
    private Long sessionId;
    private Long productId;
    private String productName;
    private Integer saleQuantity;
    private BigDecimal revenue;
    private Integer position;
    private Timestamp createTime;
}
