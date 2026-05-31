package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 话术行内评论表
 * 与 sql/live/migration-script-comment.sql 中 live_script_comment 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "live_script_comment")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveScriptComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "session_id")
    private Long sessionId;

    /** 评论作者用户 ID（兼容旧字段 user_id） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 评论作者用户 ID（对应 SQL DDL 中 author_id 列） */
    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "user_name", length = 64)
    private String userName;

    /** 评论作者名称（对应 SQL DDL 中 author_name 列） */
    @Column(name = "author_name", length = 100)
    private String authorName;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "resolved", nullable = false)
    private Integer resolved = 0;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "parent_id")
    private Long parentId;

    /** 数据所有者 ID（数据隔离） */
    @Column(name = "owner_id")
    private Long ownerId;

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
