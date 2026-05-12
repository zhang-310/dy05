package cn.gaifan.douyinOperations.module.workflow.repository;

import cn.gaifan.douyinOperations.module.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    Optional<WorkflowDefinition> findByWorkflowCodeAndDeleted(String workflowCode, Integer deleted);

    // P0-1: 带所有权校验的查询方法
    Optional<WorkflowDefinition> findByWorkflowCodeAndOwnerIdAndDeleted(String workflowCode, Long ownerId, Integer deleted);
}
