package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeDeduplicationGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Knowledge Deduplication Group Repository
 */
public interface KnowledgeDeduplicationGroupRepository extends JpaRepository<KnowledgeDeduplicationGroup, Long>,
        JpaSpecificationExecutor<KnowledgeDeduplicationGroup> {

    /**
     * Find dedup groups by master script ID
     */
    List<KnowledgeDeduplicationGroup> findByMasterScriptIdAndDeletedOrderByCreatedAtDesc(Long masterScriptId, Integer deleted);

    /**
     * Find dedup groups by duplicate script ID
     */
    List<KnowledgeDeduplicationGroup> findByDuplicateScriptIdAndDeletedOrderByCreatedAtDesc(Long duplicateScriptId, Integer deleted);

    /**
     * Find by both master and duplicate IDs
     */
    Optional<KnowledgeDeduplicationGroup> findByMasterScriptIdAndDuplicateScriptIdAndDeleted(
            Long masterScriptId, Long duplicateScriptId, Integer deleted);

    /**
     * Find groups by merge status
     */
    Page<KnowledgeDeduplicationGroup> findByUserIdAndMergeStatusAndDeletedOrderByCreatedAtDesc(
            Long userId, String mergeStatus, Integer deleted, Pageable pageable);

    /**
     * Find active variants for a master script
     */
    @Query("SELECT k FROM KnowledgeDeduplicationGroup k WHERE k.masterScriptId = :masterScriptId AND k.variantType = 'VARIANT' AND k.isActive = 1 AND k.deleted = 0 ORDER BY k.similarityScore DESC")
    List<KnowledgeDeduplicationGroup> findActiveVariantsForMaster(@Param("masterScriptId") Long masterScriptId);

    /**
     * Find duplicate candidates above similarity threshold
     */
    @Query("SELECT k FROM KnowledgeDeduplicationGroup k WHERE k.userId = :userId AND k.similarityScore >= :threshold AND k.deleted = 0 ORDER BY k.similarityScore DESC")
    List<KnowledgeDeduplicationGroup> findDuplicateCandidates(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);

    /**
     * Find merged records
     */
    List<KnowledgeDeduplicationGroup> findByUserIdAndMergeStatusAndDeletedOrderByMergedAtDesc(
            Long userId, String mergeStatus, Integer deleted);

    /**
     * Count pending review items
     */
    @Query("SELECT COUNT(k) FROM KnowledgeDeduplicationGroup k WHERE k.userId = :userId AND k.mergeStatus = 'PENDING_REVIEW' AND k.deleted = 0")
    long countPendingReview(@Param("userId") Long userId);

    /**
     * Find all variants (including archived) for script
     */
    @Query("SELECT k FROM KnowledgeDeduplicationGroup k WHERE (k.masterScriptId = :scriptId OR k.duplicateScriptId = :scriptId) AND k.deleted = 0 ORDER BY k.similarityScore DESC")
    List<KnowledgeDeduplicationGroup> findAllVariantsForScript(@Param("scriptId") Long scriptId);
}
