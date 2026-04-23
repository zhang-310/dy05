package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 知识库文档表
 */
@Data
@Entity
@Table(name = "ai_kb_document")
@SQLRestriction("deleted = 0")
public class AiKbDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_type", length = 32)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount = 0;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "boost_factor", precision = 5, scale = 2)
    private java.math.BigDecimal boostFactor;

    /** 来源：manual=手动 evolved=进化 viral=爆款拆解 live_review=直播复盘 */
    @Column(name = "source_type", length = 32)
    private String sourceType;

    /** 时效性：0=未检测 1=有效 2=过期待更新 */
    @Column(name = "expiry_status")
    private Integer expiryStatus;

    @Column(name = "last_expiry_check")
    private Timestamp lastExpiryCheck;

    @Column(name = "sync_retry_count")
    private Integer syncRetryCount = 0;

    @Column(name = "last_sync_retry_at")
    private Timestamp lastSyncRetryAt;

    /** 内容指纹 MD5，用于文档级完全一致去重 */
    @Column(name = "content_fingerprint", length = 64)
    private String contentFingerprint;

    /** 64 位 SimHash，用于近似重复检测 */
    @Column(name = "simhash")
    private Long simhash;

    /** 检索次数（效果统计） */
    @Column(name = "retrieval_count")
    private Long retrievalCount = 0L;

    /** 引用次数（效果统计） */
    @Column(name = "citation_count")
    private Long citationCount = 0L;

    /** 最近被检索时间 */
    @Column(name = "last_retrieval_at")
    private java.sql.Timestamp lastRetrievalAt;

    /** 质量分层：0=未评估 1=低质 2=中质（TIER_NEUTRAL） 3=高质（TIER_HEALTHY） */
    @Column(name = "quality_tier")
    private Integer qualityTier = 0;

    /** 启发式质量评分 0-100 */
    @Column(name = "quality_heuristic_score")
    private Integer qualityHeuristicScore;

    /** 最近质量评估时间 */
    @Column(name = "last_quality_eval_at")
    private java.sql.Timestamp lastQualityEvalAt;

    /** 扩展元数据（JSON 字符串）；与库表 TEXT 一致（见 sql/ai/kb-document-metadata-text-migration.sql） */
    @Column(name = "metadata", columnDefinition = "text") 
    private String metadata;

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
