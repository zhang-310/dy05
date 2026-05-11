package cn.gaifan.douyinOperations.module.workflow.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowSearchVO extends BasicQueryDto {
    private String keyword;
    private String status;
}
