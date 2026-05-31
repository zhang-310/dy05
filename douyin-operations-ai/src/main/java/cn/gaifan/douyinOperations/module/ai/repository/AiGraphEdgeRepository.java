package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiGraphEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 知识图谱边 Repository（Phase 3.1）
 */
public interface AiGraphEdgeRepository extends JpaRepository<AiGraphEdge, Long> {

    List<AiGraphEdge> findBySourceNodeIdAndDeleted(Long sourceNodeId, Integer deleted);

    List<AiGraphEdge> findByTargetNodeIdAndDeleted(Long targetNodeId, Integer deleted);
}
