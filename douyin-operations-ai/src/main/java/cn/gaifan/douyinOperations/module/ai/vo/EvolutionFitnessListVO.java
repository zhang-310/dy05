package cn.gaifan.douyinOperations.module.ai.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 进化适应度记录分页查询（POST body，kbId 来自路径）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EvolutionFitnessListVO extends BasicQueryDto {

    /** 可选：进化任务号 */
    private String taskId;

    /** 可选：指标名，如 evolve_completed、index_succeeded */
    private String metricName;

    /** 可选：create_time 下限（毫秒时间戳） */
    private Long startTimeMs;

    /** 可选：create_time 上限（毫秒时间戳） */
    private Long endTimeMs;

    /** 可选：AB/实验 ID */
    private String experimentId;
}
