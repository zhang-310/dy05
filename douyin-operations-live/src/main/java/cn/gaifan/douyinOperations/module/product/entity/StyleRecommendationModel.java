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
 * 风格推荐模型实体
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
@Table(name = "style_recommendation_model")
public class StyleRecommendationModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "model_type", nullable = false, length = 50)
    private String modelType;  // 'collaborative_filtering', 'rule_based', 'hybrid'

    @Column(name = "model_version", nullable = false, length = 20)
    private String modelVersion;

    @Column(name = "model_data", columnDefinition = "TEXT")
    private String modelData;  // JSON格式存储模型参数

    @Column(name = "training_samples")
    private Integer trainingSamples;

    @Column(name = "accuracy", precision = 5, scale = 4)
    private BigDecimal accuracy;

    @Column(name = "precision_score", precision = 5, scale = 4)
    private BigDecimal precisionScore;

    @Column(name = "recall_score", precision = 5, scale = 4)
    private BigDecimal recallScore;

    @Column(name = "f1_score", precision = 5, scale = 4)
    private BigDecimal f1Score;

    @Column(name = "top3_hit_rate", precision = 5, scale = 4)
    private BigDecimal top3HitRate;

    @Column(name = "avg_score_improvement", precision = 5, scale = 2)
    private BigDecimal avgScoreImprovement;

    @Column(name = "trained_at", nullable = false)
    private LocalDateTime trainedAt;

    @Column(name = "is_active")
    private Boolean isActive;

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
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
