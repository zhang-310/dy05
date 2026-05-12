package cn.gaifan.douyinOperations.module.system.vo;

import lombok.Data;

/**
 * P1-9: API 日志统计查询 VO
 */
@Data
public class ApiLogStatsVO {

    private String module;      // 模块名称
    private String startTime;   // 开始时间（格式：yyyy-MM-dd HH:mm:ss）
    private String endTime;     // 结束时间（格式：yyyy-MM-dd HH:mm:ss）
}
