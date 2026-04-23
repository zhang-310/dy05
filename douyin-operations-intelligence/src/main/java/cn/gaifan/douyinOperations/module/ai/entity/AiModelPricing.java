package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * AI 模型价格矩阵 — 区分 input/output token 价格，支持多货币
 */
@Data
@Entity
@Table(name = "ai_model_pricing")
@SQLRestriction("deleted = 0")
public class AiModelPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 100, unique = true)
    private String modelName;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "input_price_per_1k", nullable = false, precision = 10, scale = 6)
    private BigDecimal inputPricePer1k = BigDecimal.ZERO;

    @Column(name = "output_price_per_1k", nullable = false, precision = 10, scale = 6)
    private BigDecimal outputPricePer1k = BigDecimal.ZERO;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "CNY";

    @Column(name = "effective_from")
    private Timestamp effectiveFrom;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
        if (effectiveFrom == null) effectiveFrom = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
