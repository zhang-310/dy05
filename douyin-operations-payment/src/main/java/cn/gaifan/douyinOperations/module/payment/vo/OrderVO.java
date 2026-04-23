package cn.gaifan.douyinOperations.module.payment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderVO {

    private Long id;
    private String orderNo;             // 订单号
    private Long userId;                // 用户 ID
    private Long productId;             // 产品 ID
    private BigDecimal amount;          // 订单金额
    private BigDecimal actualAmount;    // 实际支付金额
    private Integer quantity;           // 商品数量
    private String status;              // 订单状态
    private String paymentMethod;       // 支付方式
    private String transactionId;       // 支付交易 ID
    private LocalDateTime paidAt;       // 支付时间
    private String trackingNumber;      // 快递单号
    private LocalDateTime shippedAt;    // 发货时间
    private LocalDateTime completedAt;  // 完成时间
    private String remark;              // 备注
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
