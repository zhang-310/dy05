package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiOfficialKnowledgeCollectItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiOfficialKnowledgeCollectItemRepository extends
        JpaRepository<AiOfficialKnowledgeCollectItem, Long>,
        JpaSpecificationExecutor<AiOfficialKnowledgeCollectItem> {

    Optional<AiOfficialKnowledgeCollectItem> findBySourceTypeAndSourceUrlHashAndDeleted(
            String sourceType, String sourceUrlHash, Integer deleted);

    @Query("SELECT i.collectStatus, COUNT(i) FROM AiOfficialKnowledgeCollectItem i " +
            "WHERE i.deleted = 0 GROUP BY i.collectStatus")
    List<Object[]> countByCollectStatus();

    @Query("SELECT i.indexStatus, COUNT(i) FROM AiOfficialKnowledgeCollectItem i " +
            "WHERE i.deleted = 0 GROUP BY i.indexStatus")
    List<Object[]> countByIndexStatus();

    @Query("SELECT i.ocrStatus, COUNT(i) FROM AiOfficialKnowledgeCollectItem i " +
            "WHERE i.deleted = 0 GROUP BY i.ocrStatus")
    List<Object[]> countByOcrStatus();

    @Query("SELECT i.asrStatus, COUNT(i) FROM AiOfficialKnowledgeCollectItem i " +
            "WHERE i.deleted = 0 GROUP BY i.asrStatus")
    List<Object[]> countByAsrStatus();

    @Query("SELECT i.targetKbName, COUNT(i) FROM AiOfficialKnowledgeCollectItem i " +
            "WHERE i.deleted = 0 GROUP BY i.targetKbName")
    List<Object[]> countByTargetKbName();

    @Query("SELECT COUNT(i) FROM AiOfficialKnowledgeCollectItem i WHERE i.deleted = 0")
    long countActive();

    @Query("SELECT COUNT(i) FROM AiOfficialKnowledgeCollectItem i " +
            "WHERE i.deleted = 0 AND (i.collectStatus = 'FAILED' OR i.indexStatus = 'FAILED' " +
            "OR i.ocrStatus = 'FAILED' OR i.asrStatus = 'FAILED')")
    long countFailedAny();

    @Query("SELECT COUNT(i) FROM AiOfficialKnowledgeCollectItem i " +
            "WHERE i.deleted = 0 AND (i.ocrStatus = :status OR i.asrStatus = :status)")
    long countMediaStatus(@Param("status") String status);

    Optional<AiOfficialKnowledgeCollectItem> findFirstByDeletedOrderByUpdateTimeDesc(Integer deleted);
}
