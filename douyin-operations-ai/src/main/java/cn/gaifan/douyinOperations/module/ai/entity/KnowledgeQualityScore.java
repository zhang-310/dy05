package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Knowledge Quality Score Entity
 * Records quality metrics for each script version in each evaluation period
 */
@Getter
@Setter
@Entity
@Table(name = "ai_knowledge_quality_score")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class KnowledgeQualityScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "script_version_id", nullable = false)
    private Long scriptVersionId;

    @Column(name = "period_start", nullable = false)
    private Date periodStart;

    @Column(name = "period_end", nullable = false)
    private Date periodEnd;

    /**
     * Quality score 0-100
     */
    @Column(name = "quality_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal qualityScore;

    /**
     * Effectiveness score
     */
    @Column(name = "effectiveness_score", precision = 5, scale = 2)
    private BigDecimal effectivenessScore;

    /**
     * Usage count in this period
     */
    @Column(name = "usage_count")
    private Integer usageCount = 0;

    /**
     * Adoption rate 0-100
     */
    @Column(name = "adoption_rate", precision = 5, scale = 2)
    private BigDecimal adoptionRate;

    /**
     * Engagement rate
     */
    @Column(name = "engagement_rate", precision = 5, scale = 2)
    private BigDecimal engagementRate;

    /**
     * Conversion rate
     */
    @Column(name = "conversion_rate", precision = 5, scale = 2)
    private BigDecimal conversionRate;

    /**
     * Average sentiment score
     */
    @Column(name = "avg_sentiment_score", precision = 5, scale = 2)
    private BigDecimal avgSentimentScore;

    /**
     * Number of consecutive periods with low score
     */
    @Column(name = "consecutive_low_scores")
    private Integer consecutiveLowScores = 0;

    /**
     * Trend: UP, DOWN, STABLE
     */
    @Column(name = "trend", length = 20)
    private String trend;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Timestamp(System.currentTimeMillis());
        if (updatedAt == null) updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
