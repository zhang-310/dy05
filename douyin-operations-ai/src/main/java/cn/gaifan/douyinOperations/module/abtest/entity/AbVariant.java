package cn.gaifan.douyinOperations.module.abtest.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * A/B 测试变体表
 * 与 sql/abtest/schema.sql 中 ab_variant 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "ab_variant")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AbVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_id", nullable = false)
    private Long experimentId;

    @Column(name = "variant_name", nullable = false, length = 64)
    private String variantName;

    /** 变体类型：A / B */
    @Column(name = "variant_type", nullable = false, length = 2)
    private String variantType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "entity_type", length = 32)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    /** 话术风格编码（script_style 实验时：如 professional / friendly） */
    @Column(name = "style_code", length = 64)
    private String styleCode;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "conversion_count", nullable = false)
    private Long conversionCount = 0L;

    @Column(name = "conversion_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal conversionRate = BigDecimal.ZERO;

    /** 是否获胜：0=否 1=是 */
    @Column(name = "is_winner", nullable = false)
    private Integer isWinner = 0;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
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
