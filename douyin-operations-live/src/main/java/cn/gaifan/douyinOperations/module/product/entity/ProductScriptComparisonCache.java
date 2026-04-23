package cn.gaifan.douyinOperations.module.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品话术对比缓存实体类
 * 缓存版本对比、风格对比、排行榜等计算结果，避免重复计算
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Entity
@Table(name = "product_script_comparison_cache", indexes = {
    @Index(name = "idx_comparison_cache_product_id", columnList = "product_id"),
    @Index(name = "idx_comparison_cache_type", columnList = "comparison_type"),
    @Index(name = "idx_comparison_cache_owner_id", columnList = "owner_id"),
    @Index(name = "idx_comparison_cache_cached_at", columnList = "cached_at DESC"),
    @Index(name = "idx_comparison_cache_composite", columnList = "product_id, comparison_type, cached_at DESC")
},
uniqueConstraints = {
    @UniqueConstraint(name = "uk_cache_composite", columnNames = {"product_id", "comparison_type", "owner_id"})
})
@SQLRestriction("deleted = 0")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductScriptComparisonCache implements Serializable {

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
     * 对比类型（version_compare/style_compare/ranking 等）
     */
    @Column(name = "comparison_type", length = 64, nullable = false)
    private String comparisonType;

    /**
     * 对比结果数据（JSONB 格式）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "comparison_data", nullable = false)
    private String comparisonData;

    /**
     * 缓存生成时间
     */
    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    /**
     * 缓存 TTL（分钟）
     */
    @Column(name = "ttl_minutes", nullable = false)
    private Integer ttlMinutes;

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
     * 检查缓存是否过期
     *
     * @return true 如果缓存已过期
     */
    public boolean isExpired() {
        if (this.cachedAt == null || this.ttlMinutes == null) {
            return true;
        }
        LocalDateTime expiresAt = this.cachedAt.plusMinutes(this.ttlMinutes);
        return LocalDateTime.now().isAfter(expiresAt);
    }

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
        if (this.ttlMinutes == null) {
            this.ttlMinutes = 60; // 默认 60 分钟
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
