package cn.gaifan.douyinOperations.module.log.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogSearchVO extends BasicQueryDto {

    private String module;
    private String action;
    private String username;
    /** 状态 0失败 1成功，不传查全部 */
    private Integer status;
    /** 开始时间（时间戳或 yyyy-MM-dd HH:mm:ss） */
    private String startTime;
    /** 结束时间 */
    private String endTime;
}
