package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播话术表
 * 与 sql/live/schema.sql 中 live_script 一一对应
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
    private String scriptType = "custom";

    @Column(name = "style", length = 64)
    private String style;

    @Column(name = "ai_generated")
    private Integer aiGenerated = 0;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "ai_call_log_id")
    private Long aiCallLogId;

    @Column(name = "generation_status", length = 16)
    private String generationStatus = "success";

    @Column(name = "violation_checked")
    private Integer violationChecked = 0;

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

    /** 时长上限（秒），0=不限制 */
    @Column(name = "duration_limit_sec")
    private Integer durationLimitSec;

    /** 需求/意图：开场白/产品介绍/促单/转场/收尾/互动引导 */
    @Column(name = "requirement", length = 128)
    private String requirement;

    @Column(name = "executed")
    private Integer executed = 0;

    /** 引用的产品话术 ID（dy_product_script.id） */
    @Column(name = "referenced_script_id")
    private Long referencedScriptId;

    /** 引用时的话术快照 JSON：id/version/content/style/scriptType */
    @Column(name = "referenced_script_snapshot", columnDefinition = "jsonb")
    private String referencedScriptSnapshot;

    @Column(name = "actual_execution_time")
    private Timestamp actualExecutionTime;

    /** 审核状态：0=未提审 1=待审核 2=已通过 3=已拒绝 */
    @Column(name = "approval_status")
    private Integer approvalStatus = 0;

    /** 话术所属用户 ID */
    @Column(name = "user_id")
    private Long userId;

    /** 生成 Prompt 的哈希（幂等检测用） */
    @Column(name = "generation_prompt_hash", length = 64)
    private String generationPromptHash;

    /** 关联 A/B 实验 ID */
    @Column(name = "ab_experiment_id")
    private Long abExperimentId;

    /** 关联 A/B 变体 ID */
    @Column(name = "ab_variant_id")
    private Long abVariantId;

    /** AI 建议内容（可选，供前端展示） */
    @Column(name = "ai_suggestion", columnDefinition = "TEXT")
    private String aiSuggestion;

    /** 关联 Prompt 模板 ID */
    @Column(name = "prompt_template_id")
    private Long promptTemplateId;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

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
