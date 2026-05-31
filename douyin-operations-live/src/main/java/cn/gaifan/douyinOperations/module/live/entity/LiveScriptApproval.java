package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 话术审核记录
 */
@Getter
@Setter
@Entity
@Table(name = "live_script_approval")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveScriptApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 提交人 ID */
    @Column(name = "submitter_id", nullable = false)
    private Long submitterId;

    /** 审核人 ID */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    /** 操作: submit / approve / reject / revoke */
    @Column(name = "action", length = 16, nullable = false)
    private String action = "submit";

    /** 审核状态: 1=待审核, 2=已通过, 3=已拒绝 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "review_time")
    private Timestamp reviewTime;

    /** 当前审批层级（Phase 2.8） */
    @Column(name = "approval_level")
    private Integer approvalLevel = 1;

    /** 最大审批层级 */
    @Column(name = "max_level")
    private Integer maxLevel = 1;

    /** 当前审批人 ID */
    @Column(name = "current_approver_id")
    private Long currentApproverId;

    /** 自动审批规则：low_risk_auto / high_score_auto */
    @Column(name = "auto_approve_rule", length = 64)
    private String autoApproveRule;

    @Column(name = "owner_id")
    private Long ownerId;

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
