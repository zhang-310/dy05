package cn.gaifan.douyinOperations.module.payment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付退款 VO
 */
@Data
public class PaymentRefundVO {
    private Long id;
    private String refundNo;
    private String transactionNo;
    private BigDecimal amount;
    private String status;
    private String reason;
    private LocalDateTime createTime;
}
