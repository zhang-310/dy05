package cn.gaifan.douyinOperations.module.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风格推荐反馈实体
 *
 * @author Claude Code
 * @since 2026-04-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "style_recommendation_feedback")
public class StyleRecommendationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "recommended_styles", columnDefinition = "TEXT")
    private String recommendedStyles;  // JSON数组

    @Column(name = "selected_styles", columnDefinition = "TEXT")
    private String selectedStyles;  // JSON数组

    @Column(name = "recommendation_source", length = 20)
    private String recommendationSource;  // 'rule_based', 'ml_model', 'hybrid'

    @Column(name = "model_version", length = 20)
    private String modelVersion;

    @Column(name = "effectiveness_scores", columnDefinition = "TEXT")
    private String effectivenessScores;  // JSON对象

    @Column(name = "best_style", length = 50)
    private String bestStyle;

    @Column(name = "best_score", precision = 5, scale = 2)
    private BigDecimal bestScore;

    @Column(name = "is_top3_hit")
    private Boolean isTop3Hit;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "feedback_type", length = 20)
    private String feedbackType;  // 'implicit', 'explicit'

    @Column(name = "user_rating")
    private Integer userRating;

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
        if (feedbackType == null) feedbackType = "implicit";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
