package cn.gaifan.douyinOperations.module.attribution.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "attribution")
@SQLRestriction("deleted = 0")
public class Attribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 归因类型: script_sales / product_gmv / overall */
    @Column(name = "attribution_type", length = 32, nullable = false)
    private String attributionType;

    /** 关联的话术ID（script_sales类型） */
    @Column(name = "script_id")
    private Long scriptId;

    /** 关联的商品ID（product_gmv类型） */
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "script_content", columnDefinition = "TEXT")
    private String scriptContent;

    @Column(name = "product_name", length = 256)
    private String productName;

    /** 贡献的销售额 */
    @Column(name = "contributed_gmv", precision = 12, scale = 2)
    private BigDecimal contributedGmv = BigDecimal.ZERO;

    /** 贡献的销量 */
    @Column(name = "contributed_sales")
    private Integer contributedSales = 0;

    /** 转化率 */
    @Column(name = "conversion_rate", precision = 5, scale = 4)
    private BigDecimal conversionRate = BigDecimal.ZERO;

    /** 贡献占比（0-1） */
    @Column(name = "contribution_ratio", precision = 5, scale = 4)
    private BigDecimal contributionRatio = BigDecimal.ZERO;

    /** 效果评分（0-100） */
    @Column(name = "effect_score")
    private Integer effectScore = 0;

    /** AI 分析说明 */
    @Column(name = "analysis", columnDefinition = "TEXT")
    private String analysis;

    @Column(name = "model_used", length = 64)
    private String modelUsed;

    @Column(name = "tokens_used")
    private Long tokensUsed = 0L;

    /** 0=计算中 1=完成 2=失败 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

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
