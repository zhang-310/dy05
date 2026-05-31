package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播商品数据汇总表
 * 与 sql/live/migration-data-sync.sql 中 live_product_data 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "live_product_data")
@NoArgsConstructor
public class LiveProductData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "impressions", nullable = false)
    private Integer impressions = 0;

    @Column(name = "clicks", nullable = false)
    private Integer clicks = 0;

    @Column(name = "orders", nullable = false)
    private Integer orders = 0;

    @Column(name = "sale_quantity", nullable = false)
    private Integer saleQuantity = 0;

    @Column(name = "revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "refund_quantity", nullable = false)
    private Integer refundQuantity = 0;

    @Column(name = "conversion_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal conversionRate = BigDecimal.ZERO;

    @Column(name = "sync_time")
    private Timestamp syncTime;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
