package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

/**
 * AI 调用日志表
 * 表：ai_call_log
 */
@Data
@Entity
@Table(name = "ai_call_log")
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "call_type", nullable = false, length = 64)
    private String callType;

    @Column(name = "template_code", length = 128)
    private String templateCode;

    @Column(name = "model_code", length = 64)
    private String modelCode;

    @Column(name = "input_summary", length = 512)
    private String inputSummary;

    @Column(name = "output_length")
    private Integer outputLength;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "is_fallback", nullable = false)
    private Integer isFallback = 0;

    /** 效果归因：引用的知识 chunk ID（JSON 数组） */
    @Column(name = "referenced_chunk_ids", columnDefinition = "TEXT")
    private String referencedChunkIds;

    /** 效果归因：关联的短视频 ID */
    @Column(name = "linked_video_id")
    private Long linkedVideoId;

    /** 效果归因：关联的直播场次 ID */
    @Column(name = "linked_session_id")
    private Long linkedSessionId;

    /** 效果归因：内容效果 high_perform/normal/low_perform */
    @Column(name = "content_effect", length = 32)
    private String contentEffect;

    /** 效果归因：效果评分（播放量/均值） */
    @Column(name = "effect_score", precision = 8, scale = 2)
    private java.math.BigDecimal effectScore;

    /** 全链路性能：各阶段耗时 JSON，如 {"cache_lookup_ms":2,"embedding_ms":100,"vector_search_ms":80,...} */
    @Column(name = "stage_timings", columnDefinition = "TEXT")
    private String stageTimings;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
