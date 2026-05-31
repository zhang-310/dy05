package cn.gaifan.douyinOperations.module.douyin.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 抖音视频表
 * 与 sql/douyin/schema.sql 中 douyin_video 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "douyin_video")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class DouyinVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "video_id", nullable = false, length = 128)
    private String videoId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "share_count", nullable = false)
    private Long shareCount = 0L;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount = 0L;

    @Column(name = "download_count", nullable = false)
    private Long downloadCount = 0L;

    @Column(name = "video_type", length = 32)
    private String videoType;

    @Column(name = "publish_time")
    private Timestamp publishTime;

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
