package cn.gaifan.douyinOperations.module.agent.repository;

import cn.gaifan.douyinOperations.module.agent.entity.AgentWorkflowExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AgentWorkflowExecutionRepository extends JpaRepository<AgentWorkflowExecution, Long>, JpaSpecificationExecutor<AgentWorkflowExecution> {

    Page<AgentWorkflowExecution> findByUserIdAndDeletedOrderByCreateTimeDesc(Long userId, Integer deleted, Pageable pageable);

    Page<AgentWorkflowExecution> findByWorkflowIdAndDeletedOrderByCreateTimeDesc(Long workflowId, Integer deleted, Pageable pageable);

    Optional<AgentWorkflowExecution> findByIdAndDeleted(Long id, Integer deleted);
}
