package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 短视频表
 * 与 sql/shortvideo/schema.sql 中 sv_video 一一对应
 */
@Data
@Entity
@Table(name = "sv_video")
@SQLRestriction("deleted = 0")
public class SvVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "douyin_video_id", length = 128)
    private String douyinVideoId;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;

    @Column(name = "video_url", length = 512)
    private String videoUrl;

    @Column(name = "duration")
    private Integer duration = 0;

    @Column(name = "tags", length = 512)
    private String tags;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "publish_time")
    private Timestamp publishTime;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;

    @Column(name = "share_count")
    private Integer shareCount = 0;

    @Column(name = "favorite_count")
    private Integer favoriteCount = 0;

    @Column(name = "is_viral")
    private Boolean isViral = false;

    @Column(name = "ai_call_log_id")
    private Long aiCallLogId;  // 关联 AI 调用日志 ID（效果归因）

    @Column(name = "ai_generated")
    private Boolean aiGenerated = false;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "sync_status", length = 16)
    private String syncStatus = "synced";

    @Column(name = "last_sync_time")
    private Timestamp lastSyncTime;

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
