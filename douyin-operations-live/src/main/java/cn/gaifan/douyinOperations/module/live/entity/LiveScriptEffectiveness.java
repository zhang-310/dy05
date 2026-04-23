package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播话术效果评分表
 * W-04: 效果评分系统
 * 记录每个话术版本的效果评分和关键指标
 */
@Data
@Entity
@Table(name = "live_script_effectiveness")
@SQLRestriction("deleted = 0")
public class LiveScriptEffectiveness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 话术 ID */
    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    /** 直播场次 ID */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 话术版本（版本号） */
    @Column(name = "version", length = 16)
    private String version;

    /** 转化率 (%) */
    @Column(name = "conversion_rate", precision = 5, scale = 2)
    private BigDecimal conversionRate = BigDecimal.ZERO;

    /** 点赞数 */
    @Column(name = "likes")
    private Long likes = 0L;

    /** 评论数 */
    @Column(name = "comments")
    private Integer comments = 0;

    /** 完播率 (%) */
    @Column(name = "completion_rate", precision = 5, scale = 2)
    private BigDecimal completionRate = BigDecimal.ZERO;

    /** 综合评分 (0-10) */
    @Column(name = "total_score", precision = 4, scale = 2)
    private BigDecimal totalScore = BigDecimal.ZERO;

    /** 评分权重：转化率(0.4) + 点赞(0.3) + 评论(0.2) + 完播(0.1) */
    @Column(name = "score_formula", length = 256)
    private String scoreFormula;

    /** 排名（1=最佳） */
    @Column(name = "ranking")
    private Integer ranking = 1;

    /** 排名变化：1=上升，0=持平，-1=下降 */
    @Column(name = "ranking_trend")
    private Integer rankingTrend = 0;

    /** 标签：hot(热门)/recommend(推荐)/new(新兴) */
    @Column(name = "tag", length = 32)
    private String tag;

    /** 样本量（使用次数） */
    @Column(name = "sample_size")
    private Integer sampleSize = 0;

    /** 更新时间（评分计算时间） */
    @Column(name = "calculated_at")
    private Timestamp calculatedAt;

    /** 删除标记 */
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    /** 创建时间 */
    @Column(name = "create_time")
    private Timestamp createTime;

    /** 更新时间 */
    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = new Timestamp(System.currentTimeMillis());
        }
        if (updateTime == null) {
            updateTime = new Timestamp(System.currentTimeMillis());
        }
        if (calculatedAt == null) {
            calculatedAt = new Timestamp(System.currentTimeMillis());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
        calculatedAt = new Timestamp(System.currentTimeMillis());
    }
}
