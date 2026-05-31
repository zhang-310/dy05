package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播场次数据汇总表
 * 与 sql/live/migration-data-sync.sql 中 live_session_data 一一对应
 * 逻辑删除：deleted=0 有效，由 sql/migrations/upgrade-analysis-2026.sql 增加 deleted 列
 */
@Getter
@Setter
@Entity
@Table(name = "live_session_data")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveSessionData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private Long sessionId;

    @Column(name = "total_viewers", nullable = false)
    private Integer totalViewers = 0;

    @Column(name = "peak_viewers", nullable = false)
    private Integer peakViewers = 0;

    @Column(name = "total_likes", nullable = false)
    private Long totalLikes = 0L;

    @Column(name = "total_comments", nullable = false)
    private Integer totalComments = 0;

    @Column(name = "total_shares", nullable = false)
    private Integer totalShares = 0;

    @Column(name = "total_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_orders", nullable = false)
    private Integer totalOrders = 0;

    @Column(name = "avg_stay_time", nullable = false)
    private Integer avgStayTime = 0;

    @Column(name = "new_followers", nullable = false)
    private Integer newFollowers = 0;

    @Column(name = "sync_time")
    private Timestamp syncTime;

    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column(name = "ai_review_id")
    private Long aiReviewId;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

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
