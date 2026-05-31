package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 对标视频实体
 */
@Getter
@Setter
@Entity
@Table(name = "benchmark_video")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class BenchmarkVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "benchmark_account_id", nullable = false)
    private Long benchmarkAccountId;

    @Column(name = "video_id", nullable = false, length = 128)
    private String videoId;

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

    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    @Column(name = "is_qualified")
    private Boolean isQualified = false;

    @Column(name = "analysis_status", nullable = false, length = 16)
    private String analysisStatus = "pending";

    @Column(name = "local_video_path", length = 512)
    private String localVideoPath;

    @Column(name = "bos_video_url", length = 512)
    private String bosVideoUrl;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
