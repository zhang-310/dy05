package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账号详情返回 VO（包含更多统计信息）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SvAccountDetailVO extends SvAccountVO {

    /** 采集任务数量 */
    private Integer taskCount;

    /** 待分析视频数 */
    private Integer pendingAnalysisCount;

    /** 已分析视频数 */
    private Integer analyzedCount;

    /** 最近一次采集任务 ID */
    private Long latestTaskId;
}
