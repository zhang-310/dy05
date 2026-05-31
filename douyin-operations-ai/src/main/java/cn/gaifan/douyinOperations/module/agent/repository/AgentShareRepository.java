package cn.gaifan.douyinOperations.module.agent.repository;

import cn.gaifan.douyinOperations.module.agent.entity.AgentShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AgentShareRepository extends JpaRepository<AgentShare, Long>, JpaSpecificationExecutor<AgentShare> {

    Optional<AgentShare> findByShareCodeAndDeleted(String shareCode, Integer deleted);

    @Modifying
    @Query("UPDATE AgentShare s SET s.viewCount = s.viewCount + 1 WHERE s.shareCode = :shareCode")
    void incrementViewCount(@Param("shareCode") String shareCode);

    Optional<AgentShare> findByConversationIdAndDeleted(Long conversationId, Integer deleted);
}
