package cn.gaifan.douyinOperations.module.payment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付交易 VO
 */
@Data
public class PaymentTransactionVO {
    private Long id;
    private String transactionNo;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private LocalDateTime createTime;
}
