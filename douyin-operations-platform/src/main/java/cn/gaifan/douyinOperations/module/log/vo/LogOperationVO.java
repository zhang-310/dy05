package cn.gaifan.douyinOperations.module.log.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志 VO
 */
@Data
public class LogOperationVO {
    private Long id;
    private Long userId;
    private String username;
    private String operationType;
    private String description;
    private String ipAddress;
    private LocalDateTime createTime;
}
