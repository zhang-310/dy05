package cn.gaifan.douyinOperations.module.log.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志 VO
 */
@Data
public class LogAuditVO {
    private Long id;
    private Long userId;
    private String username;
    private String auditType;
    private String action;
    private String module;
    private String entity;
    private Long entityId;
    private String resourceType;
    private String resourceId;
    private String targetType;
    private Long targetId;
    private String oldValue;
    private String newValue;
    private String beforeValue;
    private String afterValue;
    private String ip;
    private String userAgent;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;
}
