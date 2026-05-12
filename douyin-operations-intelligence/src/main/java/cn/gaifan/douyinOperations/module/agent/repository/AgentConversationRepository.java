package cn.gaifan.douyinOperations.module.agent.repository;

import cn.gaifan.douyinOperations.module.agent.entity.AgentConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface AgentConversationRepository extends JpaRepository<AgentConversation, Long>, JpaSpecificationExecutor<AgentConversation> {
    Optional<AgentConversation> findByIdAndDeleted(Long id, Integer deleted);

    @Modifying
    @Query("UPDATE AgentConversation c SET c.messageCount = c.messageCount + 1, c.lastMessageTime = :time WHERE c.id = :id")
    void incrementMessageCount(@Param("id") Long id, @Param("time") Timestamp time);

    // P1-3: N+1 查询优化 - JOIN FETCH 智能体信息
    @Query("SELECT c FROM AgentConversation c " +
           "LEFT JOIN FETCH c.agent " +
           "WHERE c.userId = :userId AND c.deleted = 0 " +
           "ORDER BY c.lastMessageTime DESC")
    List<AgentConversation> findByUserIdWithAgent(@Param("userId") Long userId);

    @Query("SELECT c FROM AgentConversation c " +
           "LEFT JOIN FETCH c.agent " +
           "WHERE c.userId = :userId AND c.agentId = :agentId AND c.deleted = 0 " +
           "ORDER BY c.lastMessageTime DESC")
    List<AgentConversation> findByUserIdAndAgentIdWithAgent(@Param("userId") Long userId, @Param("agentId") Long agentId);
}
