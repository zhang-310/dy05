package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 直播场次表
 * 与 sql/live/schema.sql 中 live_session 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "live_session")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 ID（数据隔离，语义等价于 owner_id） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "persona_id")
    private Long personaId;

    @Column(name = "live_title", nullable = false, length = 256)
    private String liveTitle;

    @Column(name = "session_cover", length = 512)
    private String sessionCover;

    @Column(name = "script_style", length = 64)
    private String scriptStyle = "professional";

    @Column(name = "readiness_check", length = 512)
    private String readinessCheck;

    @Column(name = "live_description", columnDefinition = "TEXT")
    private String liveDescription;

    @Column(name = "scheduled_time")
    private Timestamp scheduledTime;

    @Column(name = "scheduled_end_time")
    private Timestamp scheduledEndTime;

    @Column(name = "start_time")
    private Timestamp startTime;

    @Column(name = "end_time")
    private Timestamp endTime;

    @Column(name = "live_url", length = 512)
    private String liveUrl;

    @Column(name = "viewers")
    private Integer viewers = 0;

    @Column(name = "likes")
    private Long likes = 0L;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "recording_url", length = 512)
    private String recordingUrl;

    @Column(name = "recording_duration")
    private Long recordingDuration;

    /** 计划结束时间（超时自动结束用） */
    @Column(name = "planned_end_time")
    private Timestamp plannedEndTime;

    /** 是否开启自动同步：0=否 1=是 */
    @Column(name = "auto_sync_enabled", nullable = false)
    private Integer autoSyncEnabled = 0;

    /** 场次类型（普通直播/品牌专场/大促等） */
    @Column(name = "session_type", length = 32)
    private String sessionType;

    /** 直播形式（单人/多人/连麦等） */
    @Column(name = "live_format", length = 32)
    private String liveFormat;

    /** 所属机构 ID */
    @Column(name = "org_id")
    private Long orgId;

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
