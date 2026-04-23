package cn.gaifan.douyinOperations.module.payment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundVO {

    private Long id;
    private Long orderId;           // 订单 ID
    private BigDecimal amount;      // 退款金额
    private String status;          // 退款状态
    private String reason;          // 退款原因
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
}
