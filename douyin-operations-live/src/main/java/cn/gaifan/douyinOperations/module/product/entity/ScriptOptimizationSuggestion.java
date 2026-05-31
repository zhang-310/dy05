package cn.gaifan.douyinOperations.module.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 话术优化建议实体类
 * 存储基于分析结果的优化建议，包括建议类别、优先级、期望改进等
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Entity
@Table(name = "dy_optimization_suggestion", indexes = {
    @Index(name = "idx_optimization_suggestion_script_version_id", columnList = "script_version_id"),
    @Index(name = "idx_optimization_suggestion_analysis_result_id", columnList = "analysis_result_id"),
    @Index(name = "idx_optimization_suggestion_owner_id", columnList = "owner_id"),
    @Index(name = "idx_optimization_suggestion_category", columnList = "category"),
    @Index(name = "idx_optimization_suggestion_priority", columnList = "priority"),
    @Index(name = "idx_optimization_suggestion_adoption_status", columnList = "adoption_status"),
    @Index(name = "idx_optimization_suggestion_created_at_desc", columnList = "created_at DESC"),
    @Index(name = "idx_optimization_suggestion_deleted", columnList = "deleted")
})
@SQLRestriction("deleted = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptOptimizationSuggestion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的话术版本 ID
     */
    @Column(name = "script_version_id", nullable = false)
    private Long scriptVersionId;

    /**
     * 关联的分析结果 ID
     */
    @Column(name = "analysis_result_id", nullable = false)
    private Long analysisResultId;

    /**
     * 数据隔离：所有者 ID
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * 建议分类（CONTENT/PACING/STYLE/TOPIC）
     */
    @Column(name = "category", nullable = false, length = 64)
    private String category;

    /**
     * 优先级（LOW/MEDIUM/HIGH/CRITICAL）
     */
    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    /**
     * 建议内容描述
     */
    @Column(name = "suggestion_content", nullable = false, columnDefinition = "TEXT NOT NULL")
    private String suggestionContent;

    /**
     * 关联的弱点类型
     */
    @Column(name = "related_weak_point", length = 256)
    private String relatedWeakPoint;

    /**
     * 期望改进 JSON
     * JSON 结构：{"interactionRateIncrease": 4.2, "confidence": 0.75, "conversionRateIncrease": 2.1}
     */
    @Column(name = "expected_improvement", columnDefinition = "JSONB")
    private String expectedImprovement;

    /**
     * 采纳状态（PENDING/ACCEPTED/REJECTED/APPLIED）
     */
    @Column(name = "adoption_status", length = 20)
    private String adoptionStatus;

    /**
     * 采纳时间
     */
    @Column(name = "adopted_at")
    private LocalDateTime adoptedAt;

    /**
     * 采纳备注
     */
    @Column(name = "adoption_notes", columnDefinition = "TEXT")
    private String adoptionNotes;

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
        if (this.adoptionStatus == null) {
            this.adoptionStatus = "PENDING";
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
