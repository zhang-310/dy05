package cn.gaifan.douyinOperations.module.workflow.repository;

import cn.gaifan.douyinOperations.module.workflow.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

    List<WorkflowStep> findByDefinitionIdAndDeletedOrderBySequenceNoAsc(Long definitionId, Integer deleted);
}
