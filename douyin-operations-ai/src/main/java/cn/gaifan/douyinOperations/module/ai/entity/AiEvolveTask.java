package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 进化任务表
 */
@Getter
@Setter
@Entity
@Table(name = "ai_evolve_task")
@NoArgsConstructor
public class AiEvolveTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id")
    private Long kbId;

    @Column(name = "task_no", nullable = false, unique = true, length = 64)
    private String taskNo;

    @Column(name = "topic_ids", length = 512)
    private String topicIds;

    @Column(name = "topic_texts", columnDefinition = "TEXT")
    private String topicTexts;

    @Column(name = "evolve_angle", length = 32)
    private String evolveAngle;

    @Column(name = "gather_mode", length = 16)
    private String gatherMode = "hybrid";

    @Column(name = "context_length")
    private Integer contextLength;

    @Column(name = "model_used", length = 64)
    private String modelUsed;

    @Column(name = "fallback_tier")
    private Integer fallbackTier = 1;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "score_total")
    private Integer scoreTotal;

    @Column(name = "score_detail", columnDefinition = "TEXT")
    private String scoreDetail;

    @Column(name = "expanded_count")
    private Integer expandedCount = 0;

    @Column(name = "deepened_count")
    private Integer deepenedCount = 0;

    @Column(name = "had_quality_hint", nullable = false)
    private Integer hadQualityHint = 0;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "pending";

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    /** 任务依赖（逗号分隔的 task_no 列表，E-4 DAG） */
    @Column(name = "depends_on_task_nos", length = 1024)
    private String dependsOnTaskNos;

    /** 阻塞原因（status=blocked 时记录） */
    @Column(name = "blocked_reason", length = 512)
    private String blockedReason;

    /** 可选：AB/实验 ID（Flyway V147），适应度 experiment_id 同源 */
    @Column(name = "ab_experiment_id", length = 64)
    private String abExperimentId;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
