package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 脚本使用效果实体
 */
@Getter
@Setter
@Entity
@Table(name = "benchmark_script_usage_effect")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class BenchmarkScriptUsageEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "quality_script_id", nullable = false)
    private Long qualityScriptId;

    @Column(name = "usage_video_id", nullable = false)
    private Long usageVideoId;

    @Column(name = "usage_analysis_id", nullable = false)
    private Long usageAnalysisId;

    @Column(name = "usage_context", columnDefinition = "TEXT")
    private String usageContext;

    @Column(name = "effect_rating")
    private Integer effectRating;

    @Column(name = "effect_feedback", columnDefinition = "TEXT")
    private String effectFeedback;

    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "deleted")
    private Integer deleted = 0;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
