package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 账号短视频采集任务，对应表 sv_account_collect_task
 */
@Getter
@Setter
@Entity
@Table(name = "sv_account_collect_task")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvAccountCollectTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "account_id")
    private Long accountId;

    /** 关联账号主表 ID */
    @Column(name = "sv_account_id")
    private Long svAccountId;

    @Column(name = "account_url", length = 512)
    private String accountUrl;

    @Column(name = "account_name", length = 128)
    private String accountName;

    @Column(name = "sec_uid", length = 256)
    private String secUid;

    /** account_url / video_url / douyin_id */
    @Column(name = "input_type", length = 32)
    private String inputType;

    @Column(name = "original_input", length = 1024)
    private String originalInput;

    /** pending / collecting / collected / analyzing / indexing / completed / failed */
    @Column(name = "status", nullable = false, length = 32)
    private String status = "pending";

    @Column(name = "total_videos", nullable = false)
    private Integer totalVideos = 0;

    @Column(name = "collected_videos", nullable = false)
    private Integer collectedVideos = 0;

    @Column(name = "analyzed_videos", nullable = false)
    private Integer analyzedVideos = 0;

    @Column(name = "indexed_videos", nullable = false)
    private Integer indexedVideos = 0;

    @Column(name = "target_kb_id")
    private Long targetKbId;

    /** 本次列表采集最多写入多少条视频 */
    @Column(name = "max_count")
    private Integer maxCount;

    /** 领取该任务的采集节点 ID */
    @Column(name = "worker_id", length = 128)
    private String workerId;

    /** 领取该任务的采集节点区域 */
    @Column(name = "worker_region", length = 64)
    private String workerRegion;

    /** 当前采集租约过期时间，过期后其他节点可接力 */
    @Column(name = "lease_until")
    private Timestamp leaseUntil;

    @Column(name = "claimed_at")
    private Timestamp claimedAt;

    @Column(name = "started_at")
    private Timestamp startedAt;

    @Column(name = "finished_at")
    private Timestamp finishedAt;

    @Column(name = "last_heartbeat_at")
    private Timestamp lastHeartbeatAt;

    @Column(name = "next_run_at")
    private Timestamp nextRunAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount = 3;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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
        if (nextRunAt == null) nextRunAt = createTime;
        if (retryCount == null) retryCount = 0;
        if (maxRetryCount == null) maxRetryCount = 3;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
