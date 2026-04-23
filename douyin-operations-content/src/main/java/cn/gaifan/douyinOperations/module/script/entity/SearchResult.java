package cn.gaifan.douyinOperations.module.script.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 搜索结果记录表
 * 记录每次搜索的详细信息，用于分析和优化搜索功能
 */
@Data
@Entity
@Table(name = "sc_search_result")
@SQLRestriction("deleted = 0")
public class SearchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "query_text", columnDefinition = "TEXT", nullable = false)
    private String queryText;

    /** HYBRID / SEMANTIC / LEXICAL */
    @Column(name = "search_type", nullable = false, length = 50)
    private String searchType;

    /** 搜索返回结果总数 */
    @Column(name = "total_results")
    private Integer totalResults = 0;

    /** 排名第一的结果 ID */
    @Column(name = "top_result_id")
    private Long topResultId;

    /** 搜索执行耗时（毫秒） */
    @Column(name = "execution_time_ms")
    private Integer executionTimeMs = 0;

    /** 向量搜索权重 */
    @Column(name = "vector_weight", precision = 3, scale = 2)
    private BigDecimal vectorWeight = new BigDecimal("0.50");

    /** BM25 权重 */
    @Column(name = "lexical_weight", precision = 3, scale = 2)
    private BigDecimal lexicalWeight = new BigDecimal("0.50");

    /** 用户点击的结果 ID */
    @Column(name = "clicked_result_id")
    private Long clickedResultId;

    /** 点击时间 */
    @Column(name = "clicked_at")
    private Timestamp clickedAt;

    /** NULL=未反馈, TRUE=满意, FALSE=不满意 */
    @Column(name = "is_satisfied")
    private Boolean isSatisfied;

    /** 用户反馈时间 */
    @Column(name = "feedback_at")
    private Timestamp feedbackAt;

    /** User-Agent */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** 用户 IP */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Timestamp(System.currentTimeMillis());
    }
}
