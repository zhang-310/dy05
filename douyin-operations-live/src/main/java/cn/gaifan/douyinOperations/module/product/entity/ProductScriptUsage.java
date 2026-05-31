package cn.gaifan.douyinOperations.module.product.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 商品话术使用记录表
 * 记录某个产品话术版本被应用到直播脚本及其效果
 */
@Getter
@Setter
@Entity
@Table(name = "dy_product_script_usage")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class ProductScriptUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_script_id", nullable = false)
    private Long productScriptId;

    @Column(name = "live_script_id")
    private Long liveScriptId;

    @Column(name = "session_id")
    private Long sessionId;

    /** 直播场次商品 ID（关联 live_product） */
    @Column(name = "live_product_id")
    private Long liveProductId;

    @Column(name = "applied_time", nullable = false)
    private Timestamp appliedTime;

    @Column(name = "effectiveness_score", precision = 5, scale = 2)
    private BigDecimal effectivenessScore;

    @Column(name = "conversion_rate", precision = 5, scale = 2)
    private BigDecimal conversionRate;

    @Column(name = "sales_amount", precision = 12, scale = 2)
    private BigDecimal salesAmount;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (appliedTime == null) appliedTime = new Timestamp(System.currentTimeMillis());
    }
}
