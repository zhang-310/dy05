package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 检索查询日志（P2 监控埋点）
 */
@Data
@Entity
@Table(name = "ai_query_log")
public class AiQueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "query_text", nullable = false, length = 512)
    private String queryText;

    @Column(name = "top_k")
    private Integer topK = 10;

    @Column(name = "hit_count")
    private Integer hitCount = 0;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "cache_hit")
    private Integer cacheHit = 0;

    @Column(name = "source", length = 32)
    private String source = "user";

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
