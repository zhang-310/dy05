package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "ai_model")
@SQLRestriction("deleted = 0")
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 64)
    private String modelName;

    @Column(name = "model_provider", nullable = false, length = 32)
    private String modelProvider;

    @Column(name = "model_version", nullable = false, length = 128)
    private String modelVersion;

    @Column(name = "api_key", length = 512)
    private String apiKey;

    /** 可选；OpenAI 兼容网关根地址，留空则按 model_provider 使用系统配置 */
    @Column(name = "api_base_url", length = 512)
    private String apiBaseUrl;

    @Column(name = "max_tokens", nullable = false)
    private Integer maxTokens = 2048;

    @Column(name = "temperature", nullable = false, precision = 4, scale = 2)
    private BigDecimal temperature = new BigDecimal("0.70");

    /** 状态：0=不可用 1=可用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 是否默认模型：0=否 1=是 */
    @Column(name = "is_default", nullable = false)
    private Integer isDefault = 0;

    @Column(name = "cost_per_1k_tokens", nullable = false, precision = 10, scale = 6)
    private BigDecimal costPer1kTokens = BigDecimal.ZERO;

    @Column(name = "quota_limit")
    private Long quotaLimit = 0L;

    @Column(name = "quota_used", nullable = false)
    private Long quotaUsed = 0L;

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
