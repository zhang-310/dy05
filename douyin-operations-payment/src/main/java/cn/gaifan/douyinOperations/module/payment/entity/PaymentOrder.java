/**
 * W-11 支付系统 - 订单实体
 */

package cn.gaifan.douyinOperations.module.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表
 */
@Entity
@Table(name = "payment_order", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_order_no", columnList = "order_no"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = 0")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 基本信息
    @Column(nullable = false, unique = true)
    private String orderNo;              // 订单号（唯一，用于幂等性）

    @Column(nullable = false)
    private Long userId;                 // 用户 ID

    @Column(nullable = false)
    private Long productId;              // 商品 ID

    // 金额信息
    @Column(nullable = false)
    private BigDecimal amount;           // 订单金额（元）

    @Column(nullable = false)
    private BigDecimal actualAmount;     // 实际支付金额（含折扣）

    @Column(nullable = false)
    private Integer quantity;            // 商品数量

    // 订单状态流程
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;          // 订单状态

    // 支付信息
    private String paymentMethod;        // 支付方式（抖音支付、支付宝、微信等）
    private String transactionId;        // 支付交易 ID（支付方返回）
    private LocalDateTime paidAt;        // 支付时间

    // 发货信息
    private LocalDateTime shippedAt;     // 发货时间
    private String trackingNumber;       // 快递单号

    // 完成信息
    private LocalDateTime completedAt;   // 完成时间

    // 备注
    private String remark;               // 备注

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    // 时间戳
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING_PAYMENT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
