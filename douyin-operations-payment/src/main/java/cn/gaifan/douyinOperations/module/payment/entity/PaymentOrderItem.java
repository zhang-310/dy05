/**
 * W-11 支付系统 - 订单明细实体
 */

package cn.gaifan.douyinOperations.module.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细表
 */
@Entity
@Table(name = "payment_order_item", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_product_id", columnList = "product_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;                // 订单 ID

    @Column(nullable = false)
    private Long productId;              // 商品 ID

    @Column(nullable = false)
    private String productName;          // 商品名称

    @Column(nullable = false)
    private Integer quantity;            // 购买数量

    @Column(nullable = false)
    private BigDecimal unitPrice;        // 单价

    @Column(nullable = false)
    private BigDecimal totalPrice;       // 小计

    @Column(nullable = false)
    private BigDecimal discount;         // 折扣金额

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
