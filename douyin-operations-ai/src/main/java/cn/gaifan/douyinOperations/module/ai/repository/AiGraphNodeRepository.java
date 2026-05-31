package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiGraphNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 知识图谱节点 Repository（Phase 3.1）
 */
public interface AiGraphNodeRepository extends JpaRepository<AiGraphNode, Long> {

    List<AiGraphNode> findByEntityNameContainingIgnoreCaseAndDeleted(String entityName, Integer deleted);

    Optional<AiGraphNode> findByOwnerIdAndEntityTypeAndEntityNameAndDeleted(Long ownerId, String entityType, String entityName, Integer deleted);
}
