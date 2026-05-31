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
 * 商品话术版本实体类
 * 存储产品的各个话术版本及其效果评分
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Entity
@Table(name = "product_script_version", indexes = {
    @Index(name = "idx_product_script_version_product_id", columnList = "product_id"),
    @Index(name = "idx_product_script_version_owner_id", columnList = "owner_id"),
    @Index(name = "idx_product_script_version_effectiveness_score", columnList = "effectiveness_score desc"),
    @Index(name = "idx_product_script_version_usage_count", columnList = "usage_count desc"),
    @Index(name = "idx_product_script_version_is_active", columnList = "is_active"),
    @Index(name = "idx_product_script_version_is_recommended", columnList = "is_recommended"),
    @Index(name = "idx_product_script_version_created_at", columnList = "created_at desc")
})
@SQLRestriction("deleted = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductScriptVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联产品 ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 关联话术 ID（如果来自话术库）
     */
    @Column(name = "script_id")
    private Long scriptId;

    /**
     * 版本号（自动递增）
     */
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    /**
     * 话术内容
     */
    @Column(name = "content", columnDefinition = "TEXT NOT NULL")
    private String content;

    /**
     * 话术风格（激情/温柔/专业/幽默等）
     */
    @Column(name = "style", length = 64)
    private String style;

    /**
     * 效果评分（0-100）
     */
    @Column(name = "effectiveness_score", precision = 5, scale = 2)
    private BigDecimal effectivenessScore;

    /**
     * 使用次数
     */
    @Column(name = "usage_count")
    private Integer usageCount;

    /**
     * 转化率（百分比）
     */
    @Column(name = "conversion_rate", precision = 5, scale = 2)
    private BigDecimal conversionRate;

    /**
     * 点赞数
     */
    @Column(name = "likes_count")
    private Integer likesCount;

    /**
     * 评论数
     */
    @Column(name = "comments_count")
    private Integer commentsCount;

    /**
     * 是否启用
     */
    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * 是否推荐
     */
    @Column(name = "is_recommended")
    private Boolean isRecommended;

    /**
     * 是否归档
     */
    @Column(name = "archived")
    private Boolean archived;

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
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
        if (this.likesCount == null) {
            this.likesCount = 0;
        }
        if (this.commentsCount == null) {
            this.commentsCount = 0;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isRecommended == null) {
            this.isRecommended = false;
        }
        if (this.archived == null) {
            this.archived = false;
        }
        if (this.effectivenessScore == null) {
            this.effectivenessScore = BigDecimal.ZERO;
        }
        if (this.conversionRate == null) {
            this.conversionRate = BigDecimal.ZERO;
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
