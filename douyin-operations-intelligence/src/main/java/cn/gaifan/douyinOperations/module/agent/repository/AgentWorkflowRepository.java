package cn.gaifan.douyinOperations.module.agent.repository;

import cn.gaifan.douyinOperations.module.agent.entity.AgentWorkflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AgentWorkflowRepository extends JpaRepository<AgentWorkflow, Long>, JpaSpecificationExecutor<AgentWorkflow> {

    Optional<AgentWorkflow> findByIdAndDeleted(Long id, Integer deleted);

    Page<AgentWorkflow> findByUserIdAndDeletedOrderByCreateTimeDesc(Long userId, Integer deleted, Pageable pageable);

    long countByUserIdAndDeleted(Long userId, Integer deleted);
}