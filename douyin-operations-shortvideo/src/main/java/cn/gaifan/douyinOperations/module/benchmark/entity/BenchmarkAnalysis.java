package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 深度分析结果实体
 */
@Getter
@Setter
@Entity
@Table(name = "benchmark_analysis")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class BenchmarkAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "benchmark_video_id", nullable = false)
    private Long benchmarkVideoId;

    @Column(name = "transcript_text", columnDefinition = "TEXT")
    private String transcriptText;

    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Column(name = "api_description", columnDefinition = "TEXT")
    private String apiDescription;

    @Column(name = "merged_content", columnDefinition = "TEXT")
    private String mergedContent;

    @Column(name = "scene_count")
    private Integer sceneCount = 0;

    @Column(name = "key_frames_json", columnDefinition = "TEXT")
    private String keyFramesJson;

    @Column(name = "scene_description", columnDefinition = "TEXT")
    private String sceneDescription;

    @Column(name = "creative_type", length = 64)
    private String creativeType;

    @Column(name = "hook_strategy", columnDefinition = "TEXT")
    private String hookStrategy;

    @Column(name = "content_structure", columnDefinition = "TEXT")
    private String contentStructure;

    @Column(name = "emotional_curve", length = 256)
    private String emotionalCurve;

    @Column(name = "pacing_analysis", columnDefinition = "TEXT")
    private String pacingAnalysis;

    @Column(name = "viral_factors", columnDefinition = "TEXT")
    private String viralFactors;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "replicable_elements", columnDefinition = "TEXT")
    private String replicableElements;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "script_breakdown", columnDefinition = "TEXT")
    private String scriptBreakdown;

    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions;

    @Column(name = "target_audience", length = 256)
    private String targetAudience;

    @Column(name = "comparison_report", columnDefinition = "TEXT")
    private String comparisonReport;

    @Column(name = "differentiation_points", columnDefinition = "TEXT")
    private String differentiationPoints;

    @Column(name = "ai_model_used", length = 64)
    private String aiModelUsed;

    @Column(name = "tokens_used")
    private Long tokensUsed = 0L;

    @Column(name = "analysis_duration_ms")
    private Integer analysisDurationMs = 0;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

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
