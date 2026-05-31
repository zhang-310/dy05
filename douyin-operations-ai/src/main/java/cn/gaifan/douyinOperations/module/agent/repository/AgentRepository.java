package cn.gaifan.douyinOperations.module.agent.repository;

import cn.gaifan.douyinOperations.module.agent.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long>, JpaSpecificationExecutor<Agent> {
    Optional<Agent> findByIdAndDeleted(Long id, Integer deleted);

    @Modifying
    @Query("UPDATE Agent a SET a.status = :status WHERE a.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Modifying
    @Query("UPDATE Agent a SET a.conversationCount = a.conversationCount + 1 WHERE a.id = :id")
    void incrementConversationCount(@Param("id") Long id);
}
