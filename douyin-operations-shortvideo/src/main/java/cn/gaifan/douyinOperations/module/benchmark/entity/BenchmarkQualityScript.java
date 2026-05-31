package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 质量脚本知识库实体
 */
@Getter
@Setter
@Entity
@Table(name = "benchmark_quality_script")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class BenchmarkQualityScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
    private String scriptContent;

    @Column(name = "script_type", length = 50)
    private String scriptType;

    @Column(name = "industry", length = 50)
    private String industry;

    @Column(name = "scene_type", length = 50)
    private String sceneType;

    @Column(name = "quality_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "engagement_rate", precision = 5, scale = 2)
    private BigDecimal engagementRate;

    @Column(name = "viral_score", precision = 5, scale = 2)
    private BigDecimal viralScore;

    @Column(name = "completion_rate", precision = 5, scale = 2)
    private BigDecimal completionRate;

    @Column(name = "ai_rating", precision = 5, scale = 2)
    private BigDecimal aiRating;

    @Column(name = "likes_count")
    private Integer likesCount = 0;

    @Column(name = "comments_count")
    private Integer commentsCount = 0;

    @Column(name = "shares_count")
    private Integer sharesCount = 0;

    @Column(name = "collections_count")
    private Integer collectionsCount = 0;

    @Column(name = "views_count")
    private Integer viewsCount = 0;

    @Column(name = "video_duration")
    private Integer videoDuration;

    @Column(name = "key_features", columnDefinition = "JSONB")
    private String keyFeatures;

    @Column(name = "creative_elements", columnDefinition = "JSONB")
    private String creativeElements;

    @Column(name = "hook_strategy", columnDefinition = "TEXT")
    private String hookStrategy;

    @Column(name = "content_structure", columnDefinition = "TEXT")
    private String contentStructure;

    @Column(name = "embedding_vector", columnDefinition = "TEXT")
    private String embeddingVector;

    @Column(name = "reference_count")
    private Integer referenceCount = 0;

    @Column(name = "last_referenced_at")
    private LocalDateTime lastReferencedAt;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "deleted")
    private Integer deleted = 0;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
