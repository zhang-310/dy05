package cn.gaifan.douyinOperations.module.script.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 用户自定义违规词表
 * 与 sql/script/migration-template-uservw.sql 中 sc_user_violation_word 一一对应
 */
@Data
@Entity
@Table(name = "sc_user_violation_word")
@SQLRestriction("deleted = 0")
public class UserViolationWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word", nullable = false, length = 256)
    private String word;

    /** 适用范围：all=全场景 live_only=仅直播 video_only=仅短视频 */
    @Column(name = "scope", length = 16)
    private String scope = "all";

    /** 严重程度：1=低 2=中 3=高 */
    @Column(name = "level", nullable = false)
    private Integer level = 2;

    @Column(name = "reason", length = 128)
    private String reason;

    @Column(name = "replacement", length = 256)
    private String replacement;

    /** 状态：0=禁用 1=有效 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

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
