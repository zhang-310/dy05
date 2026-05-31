package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeQualityScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

/**
 * Knowledge Quality Score Repository
 */
public interface KnowledgeQualityScoreRepository extends JpaRepository<KnowledgeQualityScore, Long>,
        JpaSpecificationExecutor<KnowledgeQualityScore> {

    /**
     * Find quality scores by script version ID
     */
    List<KnowledgeQualityScore> findByScriptVersionIdAndDeletedOrderByPeriodEndDesc(Long scriptVersionId, Integer deleted);

    /**
     * Find all quality scores for a user
     */
    List<KnowledgeQualityScore> findByUserIdAndDeletedOrderByPeriodEndDesc(Long userId, Integer deleted);

    /**
     * Find quality scores for a user in period range
     */
    List<KnowledgeQualityScore> findByUserIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualAndDeletedOrderByPeriodEndDesc(
            Long userId, Date periodStart, Date periodEnd, Integer deleted);

    /**
     * Find latest quality score for a script
     */
    Optional<KnowledgeQualityScore> findFirstByScriptVersionIdAndDeletedOrderByPeriodEndDesc(Long scriptVersionId, Integer deleted);

    /**
     * Find quality scores in period range
     */
    Page<KnowledgeQualityScore> findByUserIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualAndDeleted(
            Long userId, Date periodStart, Date periodEnd, Integer deleted, Pageable pageable);

    /**
     * Find scores below threshold in recent period
     */
    @Query("SELECT k FROM KnowledgeQualityScore k WHERE k.scriptVersionId = :scriptVersionId AND k.qualityScore < :threshold AND k.deleted = 0 ORDER BY k.periodEnd DESC LIMIT :limit")
    List<KnowledgeQualityScore> findRecentLowScores(@Param("scriptVersionId") Long scriptVersionId,
            @Param("threshold") BigDecimal threshold, @Param("limit") int limit);

    /**
     * Find script IDs with consecutive low scores
     */
    @Query("SELECT DISTINCT k.scriptVersionId FROM KnowledgeQualityScore k WHERE k.userId = :userId AND k.consecutiveLowScores >= :threshold AND k.deleted = 0")
    List<Long> findScriptsWithConsecutiveLowScores(@Param("userId") Long userId, @Param("threshold") Integer threshold);

    /**
     * Calculate average quality score for user's library
     */
    @Query("SELECT AVG(k.qualityScore) FROM KnowledgeQualityScore k WHERE k.userId = :userId AND k.deleted = 0")
    BigDecimal calculateAverageQualityScore(@Param("userId") Long userId);

    /**
     * Find score record by script and period
     */
    Optional<KnowledgeQualityScore> findByScriptVersionIdAndPeriodStartAndPeriodEndAndDeleted(
            Long scriptVersionId, Date periodStart, Date periodEnd, Integer deleted);

    /**
     * Count scores in period
     */
    @Query("SELECT COUNT(k) FROM KnowledgeQualityScore k WHERE k.userId = :userId AND k.periodStart >= :startDate AND k.periodEnd <= :endDate AND k.deleted = 0")
    long countScoresInPeriod(@Param("userId") Long userId, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /** 全局平均质量分（跨所有用户和文档） */
    @Query("SELECT AVG(k.qualityScore) FROM KnowledgeQualityScore k WHERE k.deleted = 0")
    BigDecimal calculateGlobalAverageQualityScore();

    /**
     * Find distinct script version IDs for a user
     */
    @Query("SELECT DISTINCT k.scriptVersionId FROM KnowledgeQualityScore k WHERE k.userId = :userId AND k.deleted = 0")
    List<Long> findDistinctScriptVersionIdsByUserId(@Param("userId") Long userId);

    /** 按周聚合平均分（进化仪表盘用），返回 Object[]{week_start_date, avg_score} */
    @Query("SELECT DATE_TRUNC('week', k.periodStart), AVG(k.qualityScore) FROM KnowledgeQualityScore k WHERE k.periodStart >= :since AND k.deleted = 0 GROUP BY DATE_TRUNC('week', k.periodStart) ORDER BY DATE_TRUNC('week', k.periodStart) ASC")
    List<Object[]> findWeeklyAvgScoresSince(@Param("since") Date since);
}
