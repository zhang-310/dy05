package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播话术表（intelligence 模块本地副本，映射到 live_script 表）
 */
@Data
@Entity
@Table(name = "live_script")
@SQLRestriction("deleted = 0")
public class LiveScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
    private String scriptContent;

    @Column(name = "script_type", length = 32)
    private String scriptType;

    @Column(name = "style", length = 64)
    private String style;

    @Column(name = "ai_generated")
    private Integer aiGenerated;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "ai_call_log_id")
    private Long aiCallLogId;

    @Column(name = "generation_status", length = 16)
    private String generationStatus;

    @Column(name = "violation_checked")
    private Integer violationChecked;

    @Column(name = "violation_result", columnDefinition = "TEXT")
    private String violationResult;

    @Column(name = "viewer_delta")
    private Integer viewerDelta;

    @Column(name = "interaction_delta")
    private Integer interactionDelta;

    @Column(name = "conversion_delta")
    private Integer conversionDelta;

    @Column(name = "effectiveness_score", precision = 5, scale = 2)
    private BigDecimal effectivenessScore;

    @Column(name = "sequence_no")
    private Integer sequenceNo;

    @Column(name = "execution_time")
    private Long executionTime;

    @Column(name = "duration_limit_sec")
    private Integer durationLimitSec;

    @Column(name = "requirement", length = 128)
    private String requirement;

    @Column(name = "executed")
    private Integer executed;

    @Column(name = "referenced_script_id")
    private Long referencedScriptId;

    @Column(name = "referenced_script_snapshot", columnDefinition = "jsonb")
    private String referencedScriptSnapshot;

    @Column(name = "actual_execution_time")
    private Timestamp actualExecutionTime;

    @Column(name = "approval_status")
    private Integer approvalStatus;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "generation_prompt_hash", length = 64)
    private String generationPromptHash;

    @Column(name = "ab_experiment_id")
    private Long abExperimentId;

    @Column(name = "ab_variant_id")
    private Long abVariantId;

    @Column(name = "ai_suggestion", columnDefinition = "TEXT")
    private String aiSuggestion;

    @Column(name = "prompt_template_id")
    private Long promptTemplateId;

    @Column(name = "deleted", nullable = false)
    private Integer deleted;

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