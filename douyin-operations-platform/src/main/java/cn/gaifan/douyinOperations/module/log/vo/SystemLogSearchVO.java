package cn.gaifan.douyinOperations.module.log.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统日志分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemLogSearchVO extends BasicQueryDto {

    private String module;
    private String eventType;
    /** 状态 0失败 1成功，不传查全部 */
    private Integer status;
    private String startTime;
    private String endTime;
}
