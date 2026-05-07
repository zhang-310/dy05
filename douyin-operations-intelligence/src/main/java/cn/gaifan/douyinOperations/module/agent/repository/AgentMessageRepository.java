package cn.gaifan.douyinOperations.module.agent.repository;

import cn.gaifan.douyinOperations.module.agent.entity.AgentMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {
    List<AgentMessage> findByConversationIdOrderByCreateTimeAsc(Long conversationId);

    List<AgentMessage> findByConversationIdOrderByIdAsc(Long conversationId);
}
