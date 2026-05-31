package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiAgentWorkflowContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiAgentWorkflowContextRepository extends JpaRepository<AiAgentWorkflowContext, Long> {

    List<AiAgentWorkflowContext> findByWorkflowIdAndDeleted(String workflowId, int deleted);

    List<AiAgentWorkflowContext> findBySessionIdAndDeleted(Long sessionId, int deleted);
}
