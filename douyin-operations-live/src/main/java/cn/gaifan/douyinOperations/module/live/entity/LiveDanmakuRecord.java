package cn.gaifan.douyinOperations.module.live.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "live_danmaku_record")
@SQLRestriction("deleted = 0")
public class LiveDanmakuRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 20)
    private String sentiment;

    @Column(name = "author_nickname", length = 100)
    private String authorNickname;

    @Column(name = "danmaku_time")
    private Timestamp danmakuTime;

    @Column(name = "douyin_comment_id", length = 100)
    private String douyinCommentId;

    @Column(name = "user_id")
    private Long userId;

    @Column
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}
