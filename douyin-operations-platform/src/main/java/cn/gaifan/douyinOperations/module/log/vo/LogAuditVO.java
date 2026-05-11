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
    private String resourceType;
    private String resourceId;
    private LocalDateTime createTime;
}
