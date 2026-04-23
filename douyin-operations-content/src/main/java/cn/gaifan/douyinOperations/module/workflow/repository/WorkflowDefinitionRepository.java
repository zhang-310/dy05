package cn.gaifan.douyinOperations.module.workflow.repository;

import cn.gaifan.douyinOperations.module.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    Optional<WorkflowDefinition> findByWorkflowCodeAndDeleted(String workflowCode, Integer deleted);
}
