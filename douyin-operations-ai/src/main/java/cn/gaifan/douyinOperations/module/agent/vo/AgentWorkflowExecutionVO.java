package cn.gaifan.douyinOperations.module.agent.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行记录返回值
 */
@Data
public class AgentWorkflowExecutionVO {
    private Long id;
    private Long workflowId;
    private String workflowName;
    private Long userId;
    private Integer status;
    private String statusLabel;
    private Integer currentStepOrder;
    private Integer totalSteps;
    private Map<String, String> contextData;
    private String startTime;
    private String endTime;
    private Long durationSeconds;
    private String errorMessage;
    private List<AgentWorkflowStepVO> steps;
}
