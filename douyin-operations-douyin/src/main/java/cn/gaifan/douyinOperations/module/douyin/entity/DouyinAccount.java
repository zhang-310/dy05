package cn.gaifan.douyinOperations.module.douyin.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 抖音账号表
 * 与 sql/douyin/schema.sql 中 douyin_account 一一对应
 */
@Data
@Entity
@Table(name = "douyin_account")
@SQLRestriction("deleted = 0")
public class DouyinAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_name", nullable = false, length = 128)
    private String accountName;

    @Column(name = "account_id", nullable = false, length = 128)
    private String accountId;

    @Column(name = "follow_count", nullable = false)
    private Long followCount = 0L;

    @Column(name = "fan_count", nullable = false)
    private Long fanCount = 0L;

    @Column(name = "video_count", nullable = false)
    private Long videoCount = 0L;

    @Column(name = "total_likes", nullable = false)
    private Long totalLikes = 0L;

    @Column(name = "description", length = 512)
    private String description;

    /** 状态：0=正常 1=禁用 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "bind_time")
    private Timestamp bindTime;

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
        if (bindTime == null) bindTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
