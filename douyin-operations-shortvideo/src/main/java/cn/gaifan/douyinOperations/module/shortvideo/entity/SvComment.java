package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 视频评论表
 * 与 sql/shortvideo/schema.sql 中 sv_comment 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "sv_comment")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "douyin_comment_id", length = 128)
    private String douyinCommentId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_name", length = 128)
    private String authorName;

    @Column(name = "author_avatar", length = 512)
    private String authorAvatar;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "reply_count")
    private Integer replyCount = 0;

    @Column(name = "sentiment", length = 16)
    private String sentiment;

    @Column(name = "sentiment_score", precision = 3, scale = 2)
    private BigDecimal sentimentScore;

    @Column(name = "comment_time")
    private Timestamp commentTime;

    /** 视频来源标识（用于区分不同来源的评论） */
    @Column(name = "video_source", length = 64)
    private String videoSource;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
