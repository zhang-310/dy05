package cn.gaifan.douyinOperations.module.log.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 用户操作日志表，与 sql/log/schema.sql 中 sys_operation_log 对应
 */
@Data
@Entity
@Table(name = "sys_operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "module", length = 64)
    private String module;

    @Column(name = "action", length = 32)
    private String action;

    @Column(name = "request_uri", length = 256)
    private String requestUri;

    @Column(name = "request_method", length = 16)
    private String requestMethod;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @Column(name = "duration_ms")
    private Integer durationMs;

    /** 结果 0失败 1成功 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "error_msg", length = 512)
    private String errorMsg;

    /** 请求体摘要（调试用，限制 2000 字符），由 sql/migrations/upgrade-analysis-2026.sql 增加 */
    @Column(name = "request_body", length = 2000)
    private String requestBody;

    /** 响应体摘要（调试用，限制 2000 字符），由 sql/migrations/upgrade-analysis-2026.sql 增加 */
    @Column(name = "response_body", length = 2000)
    private String responseBody;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
