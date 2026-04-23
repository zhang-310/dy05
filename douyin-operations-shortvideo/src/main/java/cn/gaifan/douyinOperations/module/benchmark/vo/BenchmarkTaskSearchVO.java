package cn.gaifan.douyinOperations.module.benchmark.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务查询VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BenchmarkTaskSearchVO extends BasicQueryDto {

    /**
     * 账号ID
     */
    private Long benchmarkAccountId;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 任务状态
     */
    private String taskStatus;
}
