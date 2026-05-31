package cn.gaifan.douyinOperations.module.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品话术效果评分历史实体类
 * 记录每个话术版本的效果评分计算历史和快照数据
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Entity
@Table(name = "product_script_effectiveness_record", indexes = {
    @Index(name = "idx_effectiveness_record_product_id", columnList = "product_id"),
    @Index(name = "idx_effectiveness_record_script_version_id", columnList = "script_version_id"),
    @Index(name = "idx_effectiveness_record_calculated_at", columnList = "calculated_at DESC"),
    @Index(name = "idx_effectiveness_record_score_value", columnList = "score_value DESC"),
    @Index(name = "idx_effectiveness_record_owner_id", columnList = "owner_id"),
    @Index(name = "idx_effectiveness_record_composite", columnList = "product_id, script_version_id, calculated_at DESC")
})
@SQLRestriction("deleted = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductScriptEffectivenessRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属产品 ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 话术版本 ID（关联 product_script_version.id）
     */
    @Column(name = "script_version_id", nullable = false)
    private Long scriptVersionId;

    /**
     * 评分计算时间
     */
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    /**
     * 评分值（0-100）
     */
    @Column(name = "score_value", precision = 5, scale = 2, nullable = false)
    private BigDecimal scoreValue;

    /**
     * 评分等级（A/B/C/D/F）
     */
    @Column(name = "score_level", length = 10)
    private String scoreLevel;

    /**
     * 快照：使用次数
     */
    @Column(name = "usage_count_snapshot", nullable = false)
    private Integer usageCountSnapshot;

    /**
     * 快照：转化率（百分比）
     */
    @Column(name = "conversion_rate_snapshot", precision = 5, scale = 2)
    private BigDecimal conversionRateSnapshot;

    /**
     * 快照：点赞数
     */
    @Column(name = "likes_snapshot", nullable = false)
    private Integer likesSnapshot;

    /**
     * 快照：评论数
     */
    @Column(name = "comments_snapshot", nullable = false)
    private Integer commentsSnapshot;

    /**
     * 数据隔离：所有者 ID
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记（0=未删除, 1=已删除）
     */
    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    /**
     * 自动维护创建时间
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.deleted == null) {
            this.deleted = 0;
        }
        if (this.usageCountSnapshot == null) {
            this.usageCountSnapshot = 0;
        }
        if (this.likesSnapshot == null) {
            this.likesSnapshot = 0;
        }
        if (this.commentsSnapshot == null) {
            this.commentsSnapshot = 0;
        }
        if (this.scoreValue == null) {
            this.scoreValue = BigDecimal.ZERO;
        }
        if (this.conversionRateSnapshot == null) {
            this.conversionRateSnapshot = BigDecimal.ZERO;
        }
    }

    /**
     * 自动维护更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
