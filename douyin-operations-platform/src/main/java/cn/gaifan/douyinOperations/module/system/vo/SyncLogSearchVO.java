package cn.gaifan.douyinOperations.module.system.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * P1-9: 同步日志查询 VO（继承 BasicQueryDto，自动校验分页参数）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SyncLogSearchVO extends BasicQueryDto {

    private String syncType;    // 同步类型
    private String status;      // 状态
    private Long userId;        // 用户 ID
    private String startTime;   // 开始时间（格式：yyyy-MM-dd HH:mm:ss）
    private String endTime;     // 结束时间（格式：yyyy-MM-dd HH:mm:ss）
}
