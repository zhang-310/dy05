package cn.gaifan.douyinOperations.module.product.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 商品库表
 * 与 sql/product/schema.sql 中 dy_product 一一对应
 */
@Data
@Entity
@Table(name = "dy_product")
@SQLRestriction("deleted = 0")
public class DyProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_name", nullable = false, length = 256)
    private String productName;

    @Column(name = "product_category", length = 128)
    private String productCategory;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 256)
    private String imageUrl;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "inventory")
    private Long inventory = 0L;

    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "barcode", length = 128)
    private String barcode;

    @Column(name = "manufacturer", length = 256)
    private String manufacturer;

    @Column(name = "tags", length = 512)
    private String tags;

    /** 利润百分比：如 0.45=45%，正数=利润；与 lossPerUnit 二选一 */
    @Column(name = "profit_margin_pct", precision = 5, scale = 4)
    private BigDecimal profitMarginPct;

    /** 每单亏损金额（元），正数表示亏多少；与 profitMarginPct 二选一 */
    @Column(name = "loss_per_unit", precision = 10, scale = 2)
    private BigDecimal lossPerUnit;

    /** 控单策略：控3单、不控单1分钟下、控单憋单 等，拍品千次成交/密度成交关键策略 */
    @Column(name = "control_strategy", length = 128)
    private String controlStrategy;

    /** 商品链接（抖音/淘宝等），用于 AI 提取信息 */
    @Column(name = "product_link", length = 512)
    private String productLink;

    /** AI 从链接提炼的卖点（多行文本） */
    @Column(name = "ai_selling_points", columnDefinition = "TEXT")
    private String aiSellingPoints;

    /** 状态：0=下架 1=上架 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 推荐：0=普通 1=推荐 */
    @Column(name = "featured", nullable = false)
    private Integer featured = 0;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    /** 乐观锁版本号，防止并发库存更新超卖 */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

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
