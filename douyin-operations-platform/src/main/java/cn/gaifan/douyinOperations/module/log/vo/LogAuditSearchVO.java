package cn.gaifan.douyinOperations.module.log.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审计日志查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LogAuditSearchVO extends BasicQueryDto {
    private String keyword;
    private String auditType;
    private String username;
    private String action;
    private String entity;
    private Integer status;
    private String startTime;
    private String endTime;
    private Long userId;
}
