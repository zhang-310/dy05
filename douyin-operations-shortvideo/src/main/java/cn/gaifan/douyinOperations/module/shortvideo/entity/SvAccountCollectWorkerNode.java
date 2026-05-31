package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * Remote account collection worker node heartbeat.
 */
@Getter
@Setter
@Entity
@Table(name = "sv_account_collect_worker_node")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvAccountCollectWorkerNode {

    @Id
    @Column(name = "worker_id", length = 128)
    private String workerId;

    @Column(name = "worker_region", length = 64)
    private String workerRegion;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "idle";

    @Column(name = "current_task_id")
    private Long currentTaskId;

    @Column(name = "lease_until")
    private Timestamp leaseUntil;

    @Column(name = "last_seen_at")
    private Timestamp lastSeenAt;

    @Column(name = "last_claim_at")
    private Timestamp lastClaimAt;

    @Column(name = "last_submit_at")
    private Timestamp lastSubmitAt;

    @Column(name = "last_fail_at")
    private Timestamp lastFailAt;

    @Column(name = "success_count", nullable = false)
    private Integer successCount = 0;

    @Column(name = "fail_count", nullable = false)
    private Integer failCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
        if (lastSeenAt == null) lastSeenAt = now;
        if (successCount == null) successCount = 0;
        if (failCount == null) failCount = 0;
        if (deleted == null) deleted = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
