package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * Prompt 使用日志实体
 */
@Getter
@Setter
@Entity
@Table(name = "benchmark_prompt_usage_log")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class BenchmarkPromptUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "video_id")
    private Long videoId;

    @Column(name = "analysis_id")
    private Long analysisId;

    @Column(name = "prompt_content", nullable = false, columnDefinition = "TEXT")
    private String promptContent;

    @Column(name = "response_content", columnDefinition = "TEXT")
    private String responseContent;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "user_rating")
    private Integer userRating;

    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private String userFeedback;

    @Column(name = "ab_test_group", length = 20)
    private String abTestGroup;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "deleted")
    private Integer deleted = 0;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
