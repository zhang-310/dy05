package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 知识库检索日志
 * 用于质量评分 usageCount、进化规则 90 天无命中判断
 */
@Getter
@Setter
@Entity
@Table(name = "ai_search_log")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AiSearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "query_rewritten", columnDefinition = "TEXT")
    private String queryRewritten;

    @Column(name = "kb_id")
    private Long kbId;

    /** 命中文档 ID 列表，逗号分隔 */
    @Column(name = "hit_doc_ids", columnDefinition = "TEXT")
    private String hitDocIds;

    @Column(name = "hit_count")
    private Integer hitCount = 0;

    @Column(name = "top1_score")
    private Double top1Score;

    @Column(name = "search_type", length = 32)
    private String searchType;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "user_feedback", length = 16)
    private String userFeedback;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
