package cn.gaifan.douyinOperations.module.copy.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 文案审核表
 * 与 sql/copy/schema.sql 中 copy_approval 一一对应
 */
@Data
@Entity
@Table(name = "copy_approval")
@SQLRestriction("deleted = 0")
public class CopyApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "copy_id", nullable = false)
    private Long copyId;

    /** P0-2: 数据隔离 - 审批记录所有者 ID */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 审核员 ID（可为空，待审核时为空） */
    @Column(name = "user_id")
    private Long userId;

    /** 审核状态：0=拒绝 1=通过 2=待审核 */
    @Column(name = "approval_status", nullable = false)
    private Integer approvalStatus = 2;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "approval_time")
    private Timestamp approvalTime;

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
        // P0-2: 自动设置 ownerId（从 userId 复制）
        if (ownerId == null && userId != null) ownerId = userId;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
