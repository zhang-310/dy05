package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiKnowledgeBaseRepository extends JpaRepository<AiKnowledgeBase, Long>, JpaSpecificationExecutor<AiKnowledgeBase> {

    Optional<AiKnowledgeBase> findByIdAndDeleted(Long id, Integer deleted);

    List<AiKnowledgeBase> findByUserIdAndDeletedOrderByCreateTimeDesc(Long userId, Integer deleted);

    Optional<AiKnowledgeBase> findByUserIdAndKbNameAndDeleted(Long userId, String kbName, Integer deleted);

    @Modifying
    @Query("UPDATE AiKnowledgeBase k SET k.totalDocuments = k.totalDocuments + :delta, k.totalTokens = k.totalTokens + :tokens WHERE k.id = :id")
    void incrementStats(@Param("id") Long id, @Param("delta") int delta, @Param("tokens") long tokens);

    @Modifying
    @Query("UPDATE AiKnowledgeBase k SET k.status = :status WHERE k.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 查询所有不重复的 userId（跨租户进化/沉淀用） */
    @Query("SELECT DISTINCT k.userId FROM AiKnowledgeBase k WHERE k.deleted = 0")
    List<Long> findDistinctUserIds();
}
