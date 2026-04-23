package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * 进化灰色地带审核任务：qualityScore ∈ [40, 50) 的内容提交人工审核
 */
@Getter
@Setter
@Entity
@Table(name = "ai_evolution_review_task")
@SQLRestriction("deleted = 0")
public class AiEvolutionReviewTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的进化任务 ID */
    @Column(name = "evolve_task_id")
    private Long evolveTaskId;

    /** 内容预览（前 500 字符） */
    @Column(name = "content_preview", length = 500)
    private String contentPreview;

    /** 质量评分 */
    @Column(name = "quality_score")
    private Integer qualityScore;

    /** 审核人 ID */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    /** 审核状态: PENDING / APPROVED / REJECTED / REVISED */
    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus = "PENDING";

    /** 审核备注 */
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    /** 修订后内容（REVISED 时使用） */
    @Column(name = "revised_content", columnDefinition = "TEXT")
    private String revisedContent;

    /** 审核时间 */
    @Column(name = "reviewed_at")
    private Timestamp reviewedAt;

    /** 是否自动过期（48h 未审核） */
    @Column(name = "auto_expired")
    private Boolean autoExpired = false;

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
