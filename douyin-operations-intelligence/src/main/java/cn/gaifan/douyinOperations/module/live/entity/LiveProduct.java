package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播产品关系表（intelligence 模块本地副本，映射到 live_product 表）
 */
@Data
@Entity
@Table(name = "live_product")
@SQLRestriction("deleted = 0")
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
    private Integer saleQuantity;

    @Column(name = "revenue", precision = 12, scale = 2)
    private BigDecimal revenue;

    @Column(name = "position")
    private Integer position;

    @Column(name = "script_source", length = 32)
    private String scriptSource;

    @Column(name = "product_script_id")
    private Long productScriptId;

    @Column(name = "product_type", length = 128)
    private String productType;

    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;
}
