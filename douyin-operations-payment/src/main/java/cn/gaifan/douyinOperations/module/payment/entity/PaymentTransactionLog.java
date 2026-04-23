/**
 * W-11 支付系统 - 交易日志实体
 */

package cn.gaifan.douyinOperations.module.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易日志表
 */
@Entity
@Table(name = "payment_transaction_log", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_type", columnList = "type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;                // 订单 ID

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;        // 交易类型

    @Column(nullable = false)
    private BigDecimal amount;           // 金额

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;    // 交易状态

    private String externalTransactionId;// 外部交易 ID（支付方返回）

    private String remarks;              // 备注

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
