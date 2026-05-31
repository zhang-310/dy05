package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播产品关系表
 * 与 sql/live/schema.sql 中 live_product 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "live_product")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 256)
    private String productName;

    @Column(name = "sale_quantity")
    private Integer saleQuantity = 0;

    @Column(name = "revenue", precision = 12, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "position")
    private Integer position;

    @Column(name = "script_source", length = 32)
    private String scriptSource = "session";

    @Column(name = "product_script_id")
    private Long productScriptId;

    /** 产品类型：profit=利润品,loss=亏品,flat=平价品,hot=爆品,control=控单产品，可多选逗号分隔 */
    @Column(name = "product_type", length = 128)
    private String productType;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

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
