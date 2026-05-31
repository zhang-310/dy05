package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeEvolutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

/**
 * Knowledge Evolution Log Repository
 */
public interface KnowledgeEvolutionLogRepository extends JpaRepository<KnowledgeEvolutionLog, Long>,
        JpaSpecificationExecutor<KnowledgeEvolutionLog> {

    /**
     * Find logs by user ID
     */
    Page<KnowledgeEvolutionLog> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);

    /**
     * Find logs by script version ID
     */
    List<KnowledgeEvolutionLog> findByScriptVersionIdAndDeletedOrderByCreatedAtDesc(Long scriptVersionId, Integer deleted);

    /**
     * Find logs by action type
     */
    List<KnowledgeEvolutionLog> findByActionAndDeletedOrderByCreatedAtDesc(String action, Integer deleted);

    /**
     * Find logs by rule type
     */
    Page<KnowledgeEvolutionLog> findByRuleTypeAndDeletedOrderByCreatedAtDesc(String ruleType, Integer deleted, Pageable pageable);

    /**
     * Find logs by action and rule type
     */
    List<KnowledgeEvolutionLog> findByActionAndRuleTypeAndUserIdAndDeleted(String action, String ruleType, Long userId, Integer deleted);

    /**
     * Find failed executions
     */
    List<KnowledgeEvolutionLog> findByStatusAndDeletedOrderByCreatedAtDesc(String status, Integer deleted);

    /**
     * Count actions in time period
     */
    @Query("SELECT COUNT(k) FROM KnowledgeEvolutionLog k WHERE k.userId = :userId AND k.action = :action AND k.createdAt >= :startTime AND k.deleted = 0")
    long countActionsByUserAndTime(@Param("userId") Long userId, @Param("action") String action, @Param("startTime") Timestamp startTime);

    /**
     * Count by action and rule type
     */
    @Query("SELECT COUNT(k) FROM KnowledgeEvolutionLog k WHERE k.userId = :userId AND k.action = :action AND k.ruleType = :ruleType AND k.deleted = 0")
    long countByActionAndRuleType(@Param("userId") Long userId, @Param("action") String action, @Param("ruleType") String ruleType);

    /**
     * Find logs by user and time range
     */
    List<KnowledgeEvolutionLog> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedOrderByCreatedAtDesc(
            Long userId, Timestamp startTime, Timestamp endTime, Integer deleted);

    /**
     * Count logs for a script in time range
     */
    long countByUserIdAndScriptVersionIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeleted(
            Long userId, Long scriptVersionId, Timestamp startTime, Timestamp endTime, Integer deleted);

    /**
     * Count logs by user, action and time range
     */
    long countByUserIdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeleted(
            Long userId, String action, Timestamp startTime, Timestamp endTime, Integer deleted);
}
