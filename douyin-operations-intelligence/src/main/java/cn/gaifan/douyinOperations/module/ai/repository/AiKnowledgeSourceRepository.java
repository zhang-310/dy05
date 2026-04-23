package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiKnowledgeSourceRepository extends JpaRepository<AiKnowledgeSource, Long>, JpaSpecificationExecutor<AiKnowledgeSource> {

    List<AiKnowledgeSource> findByStatusAndDeletedOrderByCreateTimeDesc(Integer status, Integer deleted);

    boolean existsBySourcePathAndDeleted(String sourcePath, Integer deleted);
}
