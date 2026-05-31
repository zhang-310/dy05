package cn.gaifan.douyinOperations.module.agent.repository;

import cn.gaifan.douyinOperations.module.agent.entity.AgentMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {
    List<AgentMessage> findByConversationIdOrderByCreateTimeAsc(Long conversationId);

    List<AgentMessage> findByConversationIdOrderByIdAsc(Long conversationId);

    List<AgentMessage> findTop20ByConversationIdAndDeletedOrderByIdDesc(Long conversationId, Integer deleted);

    // P0-4: 分页查询对话历史（防止 N+1 查询和内存溢出）
    Page<AgentMessage> findByConversationIdAndDeleted(Long conversationId, Integer deleted, Pageable pageable);
}
