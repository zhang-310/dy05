package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptEffectiveness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 直播话术效果评分 Repository
 * W-04: 效果评分系统
 */
public interface LiveScriptEffectivenessRepository extends JpaRepository<LiveScriptEffectiveness, Long> {

    /**
     * 根据话术 ID 查询最新效果评分
     */
    Optional<LiveScriptEffectiveness> findByScriptIdAndDeletedOrderByCalculatedAtDesc(Long scriptId, Integer deleted);

    /**
     * 根据直播场次 ID 查询所有话术的效果评分（按评分降序）
     */
    List<LiveScriptEffectiveness> findBySessionIdAndDeletedOrderByTotalScoreDesc(Long sessionId, Integer deleted);

    /**
     * 根据直播场次 ID 分页查询效果评分（按排名升序）
     */
    Page<LiveScriptEffectiveness> findBySessionIdAndDeletedOrderByRankingAsc(Long sessionId, Integer deleted, Pageable pageable);

    /**
     * 根据评分范围查询（用于找出优秀话术）
     */
    @Query("SELECT e FROM LiveScriptEffectiveness e WHERE e.deleted = 0 AND e.totalScore >= :minScore AND e.sessionId = :sessionId ORDER BY e.totalScore DESC")
    List<LiveScriptEffectiveness> findBySessionIdAndScoreRange(@Param("sessionId") Long sessionId, @Param("minScore") java.math.BigDecimal minScore);

    /**
     * 根据标签查询（hot/recommend/new）
     */
    List<LiveScriptEffectiveness> findBySessionIdAndTagAndDeletedOrderByTotalScoreDesc(Long sessionId, String tag, Integer deleted);

    /**
     * 根据场次 ID 查询最高评分的话术
     */
    @Query("SELECT e FROM LiveScriptEffectiveness e WHERE e.deleted = 0 AND e.sessionId = :sessionId ORDER BY e.totalScore DESC LIMIT 1")
    Optional<LiveScriptEffectiveness> findTopBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 删除直播场次的所有评分数据
     */
    long deleteBySessionId(Long sessionId);

    /**
     * 统计场次下的评分记录数
     */
    long countBySessionIdAndDeleted(Long sessionId, Integer deleted);

    /**
     * 按话术类型聚合平均转化率（因果因子自适应用）
     * 返回 Object[]{scriptType, avgConversionPct}
     */
    @Query("SELECT e.scoreFormula, AVG(e.conversionRate) FROM LiveScriptEffectiveness e WHERE e.deleted = 0 AND e.scoreFormula IS NOT NULL GROUP BY e.scoreFormula")
    List<Object[]> findAvgConversionByScriptType();

    /**
     * 按用户 ID 查询平均转化率（脑部诊断用，通过场次关联）
     */
    @Query("SELECT AVG(e.conversionRate) FROM LiveScriptEffectiveness e " +
           "JOIN LiveSession s ON e.sessionId = s.id " +
           "WHERE s.userId = :userId AND e.deleted = 0 AND s.deleted = 0")
    Double findAvgConversionByUserId(@Param("userId") Long userId);
}
