package cn.gaifan.douyinOperations.module.system.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * API 调用日志表，与 sql/system/schema.sql 中 sys_api_call_log 一一对应
 * 注意：无 deleted 字段，日志通过定时任务物理清理
 */
@Data
@Entity
@Table(name = "sys_api_call_log")
public class SysApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module", nullable = false, length = 64)
    private String module;

    @Column(name = "api_name", nullable = false, length = 256)
    private String apiName;

    @Column(name = "request_url", length = 512)
    private String requestUrl;

    @Column(name = "request_method", length = 16)
    private String requestMethod;

    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
