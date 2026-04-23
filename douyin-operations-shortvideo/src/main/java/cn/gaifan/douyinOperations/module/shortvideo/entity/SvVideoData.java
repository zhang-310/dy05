package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 视频数据每日快照表
 * 与 sql/shortvideo/schema.sql 中 sv_video_data 一一对应
 * 注意：无 deleted 字段
 */
@Data
@Entity
@Table(name = "sv_video_data")
public class SvVideoData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "snapshot_date", nullable = false)
    private Date snapshotDate;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    private Long commentCount = 0L;

    @Column(name = "share_count")
    private Long shareCount = 0L;

    @Column(name = "favorite_count")
    private Integer favoriteCount = 0;

    @Column(name = "new_followers")
    private Integer newFollowers = 0;

    @Column(name = "view_delta")
    private Long viewDelta = 0L;

    @Column(name = "like_delta")
    private Integer likeDelta = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (snapshotDate == null) snapshotDate = new Date(System.currentTimeMillis());
    }
}
