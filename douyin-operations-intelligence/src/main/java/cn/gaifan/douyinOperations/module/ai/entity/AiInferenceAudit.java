package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * AI 推理审计（数据回流/效果分析用，不含完整 prompt 正文以避免敏感数据膨胀）
 */
@Data
@Entity
@Table(name = "ai_inference_audit")
@SQLRestriction("deleted = 0")
public class AiInferenceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "provider", length = 32)
    private String provider;

    /** chat | embed | video | tools */
    @Column(name = "capability", length = 32)
    private String capability;

    @Column(name = "model_ref", length = 256)
    private String modelRef;

    @Column(name = "input_chars")
    private Integer inputChars;

    @Column(name = "ok")
    private Integer ok;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "meta_json", columnDefinition = "TEXT")
    private String metaJson;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = new Timestamp(System.currentTimeMillis());
        }
    }
}
