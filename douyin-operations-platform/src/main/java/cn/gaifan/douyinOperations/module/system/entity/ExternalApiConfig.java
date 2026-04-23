package cn.gaifan.douyinOperations.module.system.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 外部 API 配置表
 * 与 V007 migration 中 external_api_config 一一对应
 */
@Data
@Entity
@Table(name = "external_api_config")
@SQLRestriction("deleted = 0")
public class ExternalApiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 供应商编码，如 tianapi / deepseek / qwen */
    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    /** 供应商名称 */
    @Column(name = "provider_name", nullable = false, length = 128)
    private String providerName;

    /** 分类：ai / data / media / sms 等 */
    @Column(name = "category", length = 32)
    private String category;

    /** 基础 URL */
    @Column(name = "base_url", length = 512)
    private String baseUrl;

    /** 加密后的 API Key（不可明文返回） */
    @Column(name = "api_key_encrypted", length = 512)
    private String apiKeyEncrypted;

    /** 加密后的 API Secret（不可明文返回） */
    @Column(name = "api_secret_encrypted", length = 512)
    private String apiSecretEncrypted;

    /** 是否启用 */
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    /** 优先级，数值越小越优先 */
    @Column(name = "priority")
    private Integer priority = 0;

    /** 每分钟请求限制 */
    @Column(name = "rate_limit_per_min")
    private Integer rateLimitPerMin;

    /** 日配额 */
    @Column(name = "daily_quota")
    private Integer dailyQuota;

    /** 月配额 */
    @Column(name = "monthly_quota")
    private Integer monthlyQuota;

    /** 最后健康检查时间 */
    @Column(name = "last_health_check")
    private Timestamp lastHealthCheck;

    /** 健康状态：healthy / degraded / down */
    @Column(name = "health_status", length = 32)
    private String healthStatus;

    /** 平均延迟（毫秒） */
    @Column(name = "avg_latency_ms")
    private Integer avgLatencyMs;

    /** 成功率百分比（0-100） */
    @Column(name = "success_rate_pct")
    private Float successRatePct;

    /** 扩展配置 JSON */
    @Column(name = "extra_config", columnDefinition = "jsonb")
    private String extraConfig;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
