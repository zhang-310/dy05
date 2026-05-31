package cn.gaifan.douyinOperations.module.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 重新生成的话术版本实体类
 * 存储基于优化建议重新生成的话术版本，支持多种风格
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Entity
@Table(name = "dy_script_regenerated_version", indexes = {
    @Index(name = "idx_script_regenerated_version_script_version_id", columnList = "script_version_id"),
    @Index(name = "idx_script_regenerated_version_suggestion_id", columnList = "suggestion_id"),
    @Index(name = "idx_script_regenerated_version_owner_id", columnList = "owner_id"),
    @Index(name = "idx_script_regenerated_version_generation_style", columnList = "generation_style"),
    @Index(name = "idx_script_regenerated_version_is_applied", columnList = "is_applied"),
    @Index(name = "idx_script_regenerated_version_approval_status", columnList = "approval_status"),
    @Index(name = "idx_script_regenerated_version_ai_quality_score_desc", columnList = "ai_quality_score DESC"),
    @Index(name = "idx_script_regenerated_version_created_at_desc", columnList = "created_at DESC"),
    @Index(name = "idx_script_regenerated_version_deleted", columnList = "deleted")
})
@SQLRestriction("deleted = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptRegeneratedVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的原话术版本 ID
     */
    @Column(name = "script_version_id", nullable = false)
    private Long scriptVersionId;

    /**
     * 关联的优化建议 ID
     */
    @Column(name = "suggestion_id", nullable = false)
    private Long suggestionId;

    /**
     * 数据隔离：所有者 ID
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * 生成的话术风格（FRIENDLY/HUMOROUS/PREMIUM/INSPIRATIONAL）
     */
    @Column(name = "generation_style", nullable = false, length = 64)
    private String generationStyle;

    /**
     * 重新生成的话术内容
     */
    @Column(name = "regenerated_content", nullable = false, columnDefinition = "TEXT NOT NULL")
    private String regeneratedContent;

    /**
     * AI 生成质量评分（0-10）
     */
    @Column(name = "ai_quality_score", precision = 5, scale = 2)
    private BigDecimal aiQualityScore;

    /**
     * 估计指标 JSON
     * JSON 结构：{"interactionRate": 22.7, "conversionRate": 15.1, "estimatedFanGrowth": 350}
     */
    @Column(name = "estimated_metrics", columnDefinition = "JSONB")
    private String estimatedMetrics;

    /**
     * 生成时使用的提示词
     */
    @Column(name = "generation_prompt", columnDefinition = "TEXT")
    private String generationPrompt;

    /**
     * 是否已应用到话术版本
     */
    @Column(name = "is_applied")
    private Boolean isApplied;

    /**
     * 应用时间
     */
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    /**
     * 审批状态（PENDING/APPROVED/REJECTED）
     */
    @Column(name = "approval_status", length = 20)
    private String approvalStatus;

    /**
     * 审批人 ID
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    /**
     * 审批时间
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * 审批备注
     */
    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 逻辑删除标记（0=正常 1=已删除）
     */
    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    /**
     * 自动维护创建时间
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.deleted == null) {
            this.deleted = 0;
        }
        if (this.isApplied == null) {
            this.isApplied = false;
        }
        if (this.approvalStatus == null) {
            this.approvalStatus = "PENDING";
        }
        if (this.aiQualityScore == null) {
            this.aiQualityScore = BigDecimal.ZERO;
        }
    }

    /**
     * 自动维护更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
