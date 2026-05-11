package cn.gaifan.douyinOperations.module.workflow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流 VO
 */
@Data
public class WorkflowVO {
    private Long id;
    private String name;
    private String description;
    private String status;
    private Integer stepCount;
    private LocalDateTime createTime;
}
