package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 直播话术审批日志表
 */
@Data
@Entity
@Table(name = "live_approval_log")
@SQLRestriction("deleted = 0")
public class LiveApprovalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "script_id")
    private Long scriptId;

    /** 动作：submit / approve / reject */
    @Column(name = "action", length = 16, nullable = false)
    private String action;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "deleted", nullable = false)
    private int deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
