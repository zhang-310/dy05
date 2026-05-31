package cn.gaifan.douyinOperations.module.ai.service;

/**
 * Agent 输出校验与自修复服务
 */
public interface AgentOutputValidator {

    /**
     * 校验 Agent 输出并在失败时尝试自修复
     *
     * @param agentRole Agent 角色
     * @param output    原始输出
     * @param prompt    原始输入 prompt
     * @param model     使用的 AI 模型
     * @param sessionId 场次 ID（用于事实校验）
     * @return 校验通过或修复后的输出；修复失败返回降级输出
     */
    String validateAndRepair(String agentRole, String output, String prompt,
                             cn.gaifan.douyinOperations.module.ai.entity.AiModel model, Long sessionId);
}
