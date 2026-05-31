package cn.gaifan.douyinOperations.module.ai.tool;

/**
 * 工具执行上下文（用户隔离、可选 Agent/会话）
 */
public record LlmToolContext(Long userId, Long agentId, Long conversationId, String traceHint) {

    public static LlmToolContext forUser(long userId) {
        return new LlmToolContext(userId, null, null, null);
    }
}
