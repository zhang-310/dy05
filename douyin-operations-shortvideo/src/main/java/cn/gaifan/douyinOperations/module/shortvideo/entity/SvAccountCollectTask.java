package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 账号短视频采集任务，对应表 sv_account_collect_task
 */
@Data
@Entity
@Table(name = "sv_account_collect_task")
@SQLRestriction("deleted = 0")
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
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
