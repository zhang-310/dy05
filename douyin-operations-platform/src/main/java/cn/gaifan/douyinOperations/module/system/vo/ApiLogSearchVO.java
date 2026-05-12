package cn.gaifan.douyinOperations.module.system.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * P1-9: API 日志查询 VO（继承 BasicQueryDto，自动校验分页参数）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiLogSearchVO extends BasicQueryDto {

    private String module;      // 模块名称
    private String apiName;     // API 名称
    private Integer status;     // 状态（1=成功 0=失败）
    private String startTime;   // 开始时间（格式：yyyy-MM-dd HH:mm:ss）
    private String endTime;     // 结束时间（格式：yyyy-MM-dd HH:mm:ss）
}
