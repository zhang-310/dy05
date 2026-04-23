package cn.gaifan.douyinOperations.module.script.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 搜索分析表
 * 按日期聚合搜索数据，支持分析和可视化
 */
@Data
@Entity
@Table(name = "sc_search_analytics")
@SQLRestriction("deleted = 0")
public class SearchAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL 表示全局统计 */
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "analytics_date", nullable = false)
    private Date analyticsDate;

    @Column(name = "search_query", length = 512)
    private String searchQuery;

    /** HYBRID / SEMANTIC / LEXICAL */
    @Column(name = "search_type", length = 50)
    private String searchType;

    /** 搜索次数 */
    @Column(name = "search_count", nullable = false)
    private Integer searchCount = 0;

    /** 平均执行耗时（毫秒） */
    @Column(name = "avg_execution_time_ms", precision = 10, scale = 2)
    private BigDecimal avgExecutionTimeMs = BigDecimal.ZERO;

    /** 点击率 [0-100] */
    @Column(name = "click_through_rate", precision = 5, scale = 2)
    private BigDecimal clickThroughRate = BigDecimal.ZERO;

    /** 满意度评分 [0-1] */
    @Column(name = "satisfaction_score", precision = 4, scale = 2)
    private BigDecimal satisfactionScore = BigDecimal.ZERO;

    /** 排名第一的结果 ID */
    @Column(name = "top_result_id")
    private Long topResultId;

    /** 第一名的点击次数 */
    @Column(name = "top_result_click_count")
    private Integer topResultClickCount = 0;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Timestamp(System.currentTimeMillis());
        if (updatedAt == null) updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
