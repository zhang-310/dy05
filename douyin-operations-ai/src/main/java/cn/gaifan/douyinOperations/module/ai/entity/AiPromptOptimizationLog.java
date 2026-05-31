package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Prompt 自优化日志：记录每次 meta-prompt 分析与优化结果
 */
@Getter
@Setter
@Entity
@Table(name = "ai_prompt_optimization_log")
@SQLRestriction("deleted = 0")
public class AiPromptOptimizationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务类型（live_script / evolve / copy_gen 等） */
    @Column(name = "task_type", nullable = false, length = 50)
    private String taskType;

    /** 原始 prompt 片段的 SHA-256 哈希 */
    @Column(name = "original_prompt_hash", length = 64)
    private String originalPromptHash;

    /** 优化后 prompt 片段的 SHA-256 哈希 */
    @Column(name = "optimized_prompt_hash", length = 64)
    private String optimizedPromptHash;

    /** 优化改善百分比（-100 ~ +100） */
    @Column(name = "improvement_pct", precision = 5, scale = 2)
    private BigDecimal improvementPct;

    /** 触发原因（low_effectiveness / manual / scheduled） */
    @Column(name = "trigger_reason", length = 100)
    private String triggerReason;

    /** 优化详情 JSON：{effective_fragments:[], ineffective_fragments:[], suggestions:[]} */
    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (this.createTime == null) this.createTime = now;
        if (this.updateTime == null) this.updateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}
