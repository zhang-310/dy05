package cn.gaifan.douyinOperations.module.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品特征缓存实体
 *
 * @author Claude Code
 * @since 2026-04-04
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_feature_cache")
public class ProductFeatureCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "feature_vector", nullable = false, columnDefinition = "TEXT")
    private String featureVector;  // JSON数组

    @Column(name = "category_encoded", columnDefinition = "TEXT")
    private String categoryEncoded;  // JSON对象

    @Column(name = "price_normalized", precision = 10, scale = 6)
    private BigDecimal priceNormalized;

    @Column(name = "text_features", columnDefinition = "TEXT")
    private String textFeatures;  // JSON对象

    @Column(name = "historical_avg_score", precision = 5, scale = 2)
    private BigDecimal historicalAvgScore;

    @Column(name = "historical_usage_count")
    private Integer historicalUsageCount;

    @Column(name = "feature_version", length = 20)
    private String featureVersion;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted")
    private Integer deleted;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (deleted == null) deleted = 0;
        if (featureVersion == null) featureVersion = "1.0";
        if (historicalUsageCount == null) historicalUsageCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
