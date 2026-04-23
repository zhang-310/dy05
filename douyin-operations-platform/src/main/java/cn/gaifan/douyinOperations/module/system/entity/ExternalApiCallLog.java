package cn.gaifan.douyinOperations.module.system.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 外部 API 调用日志表
 * 与 V007 migration 中 external_api_call_log 一一对应
 * 注意：无 deleted 字段，日志通过定时任务物理清理
 */
@Data
@Entity
@Table(name = "external_api_call_log")
public class ExternalApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 供应商编码 */
    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    /** 请求端点 */
    @Column(name = "endpoint", length = 512)
    private String endpoint;

    /** HTTP 方法 */
    @Column(name = "method", length = 16)
    private String method;

    /** 请求摘要 */
    @Column(name = "request_summary", length = 512)
    private String requestSummary;

    /** HTTP 响应状态码 */
    @Column(name = "response_status")
    private Integer responseStatus;

    /** 延迟（毫秒） */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /** 错误信息 */
    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    /** 调用方模块 */
    @Column(name = "caller_module", length = 64)
    private String callerModule;

    /** 调用方用户 ID */
    @Column(name = "caller_user_id")
    private Long callerUserId;

    /** 预估费用 */
    @Column(name = "estimated_cost", precision = 12, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
