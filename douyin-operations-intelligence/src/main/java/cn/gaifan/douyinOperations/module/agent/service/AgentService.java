package cn.gaifan.douyinOperations.module.agent.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSearchVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentVO;

import java.util.List;
import java.util.Map;

public interface AgentService {

    PageResultVO<AgentVO> searchAgents(Long userId, AgentSearchVO searchVO);

    AgentVO getAgentById(Long id);

    long saveAgent(Long userId, AgentSaveVO saveVO);

    void deleteAgent(Long id);

    void updateAgentStatus(Long id, Integer status);

    long createConversation(Long agentId, Long userId, String topic);

    List<Map<String, Object>> listConversations(Long userId, Long agentId);

    void deleteConversation(Long id);

    long sendMessage(Long conversationId, Integer senderType, String content, Integer tokens);

    List<Map<String, Object>> listMessages(Long conversationId);

    /**
     * 与智能体对话（消息触达 webhook 用）
     * @param agentId 智能体 ID
     * @param userId 用户 ID
     * @param message 用户消息
     * @param conversationId 会话 ID（可为 null，null 时自动创建）
     * @param context 额外上下文（可为 null）
     * @param statusEmitter 可选的状态事件发射器，用于 SSE 流式工具调用反馈
     * @return 智能体回复内容
     */
    String chatWithAgent(Long agentId, Long userId, String message, Long conversationId, Object context,
                         java.util.function.BiConsumer<String, String> statusEmitter);

    /** 评价消息（up=好评 down=差评） */
    void rateMessage(Long messageId, String rating);

    /** 导出会话为 Markdown，返回下载 URL */
    String exportConversation(Long conversationId);
}
