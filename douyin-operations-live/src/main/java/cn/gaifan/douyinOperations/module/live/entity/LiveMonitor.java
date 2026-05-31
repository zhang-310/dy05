package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播监控数据表
 * 与 sql/live/schema.sql 中 live_monitor 一一对应
 * 逻辑删除：deleted=0 有效，由 sql/migrations/upgrade-analysis-2026.sql 增加 deleted 列
 */
@Getter
@Setter
@Entity
@Table(name = "live_monitor")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "timestamp", nullable = false)
    private Timestamp timestamp;

    @Column(name = "viewers")
    private Integer viewers = 0;

    @Column(name = "likes")
    private Long likes = 0L;

    @Column(name = "comments")
    private Integer comments = 0;

    @Column(name = "shares")
    private Integer shares = 0;

    @Column(name = "product_impressions")
    private Integer productImpressions = 0;

    @Column(name = "total_viewers")
    private Integer totalViewers = 0;

    @Column(name = "new_followers")
    private Integer newFollowers = 0;

    @Column(name = "online_count")
    private Integer onlineCount = 0;

    @Column(name = "gmv", precision = 12, scale = 2)
    private BigDecimal gmv = BigDecimal.ZERO;

    @Column(name = "orders")
    private Integer orders = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        // 时间戳由 migration 或业务维护，此处仅占位
    }
}
