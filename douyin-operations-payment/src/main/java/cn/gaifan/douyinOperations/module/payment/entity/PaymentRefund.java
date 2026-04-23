/**
 * W-11 支付系统 - 退款实体
 */

package cn.gaifan.douyinOperations.module.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款表
 */
@Entity
@Table(name = "payment_refund", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;                // 订单 ID

    @Column(nullable = false)
    private BigDecimal amount;           // 退款金额

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RefundStatus status;         // 退款状态

    private String reason;               // 退款原因

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;    // 批准时间
    private LocalDateTime completedAt;   // 完成时间
}
