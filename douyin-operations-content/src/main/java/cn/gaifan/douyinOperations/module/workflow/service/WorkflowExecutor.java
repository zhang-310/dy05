package cn.gaifan.douyinOperations.module.workflow.service;

import java.util.Map;

/**
 * 工作流执行器：按步骤执行 generate→iterate→violation_check→save
 */
public interface WorkflowExecutor {

    /**
     * 执行工作流
     * @param workflowCode 工作流编码，如 live_script_full
     * @param params 参数，如 sessionId, userId, personaId 等
     * @return 执行结果（含各步骤输出、最终 scriptIds 等）
     */
    WorkflowExecuteResult execute(String workflowCode, Map<String, Object> params);

    record WorkflowExecuteResult(boolean success, String message, Map<String, Object> outputs, String failedStep) {}
}
