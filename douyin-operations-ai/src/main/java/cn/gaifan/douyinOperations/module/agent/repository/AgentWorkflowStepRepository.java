package cn.gaifan.douyinOperations.module.agent.repository;

import cn.gaifan.douyinOperations.module.agent.entity.AgentWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgentWorkflowStepRepository extends JpaRepository<AgentWorkflowStep, Long> {

    List<AgentWorkflowStep> findByWorkflowIdAndDeletedOrderByStepOrderAsc(Long workflowId, Integer deleted);

    @Query("SELECT COUNT(s) FROM AgentWorkflowStep s WHERE s.workflowId = :wfId AND s.deleted = 0")
    long countByWorkflowId(@Param("wfId") Long workflowId);
}