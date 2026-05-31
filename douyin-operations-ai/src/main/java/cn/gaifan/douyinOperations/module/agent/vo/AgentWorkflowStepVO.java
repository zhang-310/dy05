package cn.gaifan.douyinOperations.module.agent.vo;

import lombok.Data;

/**
 * 工作流步骤返回值
 */
@Data
public class AgentWorkflowStepVO {
    private Long id;
    private Long workflowId;
    private Integer stepOrder;
    private Long agentId;
    private String agentName;
    private String stepName;
    private String inputTemplate;
    private String outputKey;
    private String skipCondition;
    /** JSON 数组字符串，如 ["step1","step2"] */
    private String dependsOn;
    private Integer executionMode;
    private Integer retryCount;
    private Integer timeoutSeconds;
}
