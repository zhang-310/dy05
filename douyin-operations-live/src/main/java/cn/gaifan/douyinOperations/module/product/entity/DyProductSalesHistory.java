package cn.gaifan.douyinOperations.module.product.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 商品销售历史表
 * 与 sql/product/schema.sql 中 dy_product_sales_history 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "dy_product_sales_history")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class DyProductSalesHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sale_quantity")
    private Long saleQuantity = 0L;

    @Column(name = "sale_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal saleAmount;

    @Column(name = "sale_time", nullable = false)
    private Timestamp saleTime;

    @Column(name = "channel_source", length = 64)
    private String channelSource;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (saleTime == null) saleTime = new Timestamp(System.currentTimeMillis());
    }
}
