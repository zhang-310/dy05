package cn.gaifan.douyinOperations.module.script.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 搜索建议表
 * 存储热点搜索词、历史搜索和推荐词
 */
@Data
@Entity
@Table(name = "sc_search_suggestion")
@SQLRestriction("deleted = 0")
public class SearchSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL 表示全局建议 */
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "suggestion_text", nullable = false, length = 256)
    private String suggestionText;

    /** HISTORY / HOT_TOPIC / RECOMMENDED / SYSTEM */
    @Column(name = "suggestion_type", nullable = false, length = 50)
    private String suggestionType;

    /** 被搜索的次数 */
    @Column(name = "search_count", nullable = false)
    private Integer searchCount = 0;

    /** 热度评分 [0-1] */
    @Column(name = "trending_score", precision = 4, scale = 3)
    private BigDecimal trendingScore = BigDecimal.ZERO;

    /** 匹配的脚本数 */
    @Column(name = "result_count")
    private Integer resultCount = 0;

    /** 最后被搜索的时间 */
    @Column(name = "last_searched_at")
    private Timestamp lastSearchedAt;

    @Column(name = "last_updated")
    private Timestamp lastUpdated;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Timestamp(System.currentTimeMillis());
        if (lastUpdated == null) lastUpdated = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        lastUpdated = new Timestamp(System.currentTimeMillis());
    }
}
