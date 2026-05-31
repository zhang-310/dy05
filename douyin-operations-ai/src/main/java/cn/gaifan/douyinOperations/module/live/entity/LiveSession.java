package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 直播场次表（intelligence 模块本地副本，映射到 live_session 表）
 * 用于 AttributionServiceImpl 读取直播数据，避免与 live 模块的循环依赖
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
    private String scriptStyle;

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
    private Integer viewers;

    @Column(name = "likes")
    private Long likes;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "recording_url", length = 512)
    private String recordingUrl;

    @Column(name = "recording_duration")
    private Long recordingDuration;

    @Column(name = "planned_end_time")
    private Timestamp plannedEndTime;

    @Column(name = "auto_sync_enabled", nullable = false)
    private Integer autoSyncEnabled;

    @Column(name = "session_type", length = 32)
    private String sessionType;

    @Column(name = "live_format", length = 32)
    private String liveFormat;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;
}
