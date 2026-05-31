package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 直播话术 Repository
 */
public interface LiveScriptRepository extends JpaRepository<LiveScript, Long> {

    /**
     * 根据直播场次 ID 查询话术
     */
    List<LiveScript> findBySessionIdAndDeleted(Long sessionId, Integer deleted);

    /**
     * 话术效果排行：按 sequence_no 升序（effectiveness_score 后续 migration 补齐后可扩展）
     */
    List<LiveScript> findBySessionIdAndDeletedOrderBySequenceNoAsc(Long sessionId, Integer deleted);

    /**
     * 根据直播场次 ID 分页查询
     */
    Page<LiveScript> findBySessionIdAndDeleted(Long sessionId, Integer deleted, Pageable pageable);

    /**
     * 多条件分页查询。keyword 只匹配真实落库字段 scriptContent/requirement，
     * 避免前端传入未落库的 scriptTitle 后产生假筛选。
     */
    @Query("""
            SELECT s FROM LiveScript s
            WHERE s.deleted = 0
              AND (:sessionId IS NULL OR s.sessionId = :sessionId)
              AND (:hasSessionIds = false OR s.sessionId IN :sessionIds)
              AND (:executed IS NULL OR s.executed = :executed)
              AND (:scriptType IS NULL OR s.scriptType = :scriptType)
              AND (
                    :keyword IS NULL
                    OR LOWER(COALESCE(s.scriptContent, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(s.requirement, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            """)
    Page<LiveScript> searchByFilters(@Param("sessionId") Long sessionId,
                                     @Param("sessionIds") List<Long> sessionIds,
                                     @Param("hasSessionIds") boolean hasSessionIds,
                                     @Param("scriptType") String scriptType,
                                     @Param("keyword") String keyword,
                                     @Param("executed") Integer executed,
                                     Pageable pageable);

    /**
     * 根据多个场次 ID 分页查询（DataScope 数据范围过滤）
     */
    Page<LiveScript> findBySessionIdInAndDeleted(List<Long> sessionIds, Integer deleted, Pageable pageable);

    /**
     * 根据执行状态查询
     */
    List<LiveScript> findBySessionIdAndExecutedAndDeleted(Long sessionId, Integer executed, Integer deleted);

    /**
     * 删除直播场次关联的所有话术
     */
    long deleteBySessionId(Long sessionId);

    /**
     * 统计直播场次的话术总数
     */
    long countBySessionIdAndDeleted(Long sessionId, Integer deleted);

    /**
     * 批量统计各场次话术数量（返回 sessionId -> count）
     */
    @Query("SELECT s.sessionId, COUNT(s) FROM LiveScript s WHERE s.sessionId IN :sessionIds AND s.deleted = 0 GROUP BY s.sessionId")
    List<Object[]> countGroupBySessionIdIn(@Param("sessionIds") List<Long> sessionIds);

    /**
     * 获取场次下最大的排序号
     */
    @Query("SELECT MAX(s.sequenceNo) FROM LiveScript s WHERE s.sessionId = :sessionId AND s.deleted = 0")
    Integer findMaxSequenceNoBySessionId(@Param("sessionId") Long sessionId);

    /** 效果评分 >= 阈值的话术（用于高效话术入库） */
    @Query("SELECT s FROM LiveScript s WHERE s.deleted = 0 AND s.effectivenessScore IS NOT NULL AND s.effectivenessScore >= :threshold")
    List<LiveScript> findByEffectivenessScoreGreaterThanEqual(@Param("threshold") BigDecimal threshold);

    /** 按用户 ID + 效果评分阈值查询（通过 session 关联） */
    @Query("SELECT s FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE sess.userId = :userId AND s.deleted = 0 AND s.effectivenessScore IS NOT NULL AND s.effectivenessScore >= :threshold")
    List<LiveScript> findByUserIdAndEffectivenessScoreGreaterThanEqual(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);

    /** 按效果评分 >= 阈值查询（兼容别名） */
    @Query("SELECT s FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE sess.userId = :userId AND s.deleted = 0 AND s.effectivenessScore IS NOT NULL AND s.effectivenessScore >= :threshold")
    List<LiveScript> findByUserIdAndEffectivenessScoreGte(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);

    /** 按 ID 列表和 userId 查询（通过 session 归属过滤） */
    @Query("SELECT s FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE s.id IN :ids AND sess.userId = :userId AND s.deleted = 0")
    List<LiveScript> findByIdInAndUserId(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /** 按 userId 统计话术类型分布（认知画像用） */
    @Query("SELECT s.scriptType, COUNT(s) FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE sess.userId = :userId AND s.deleted = 0 AND s.scriptType IS NOT NULL GROUP BY s.scriptType")
    List<Object[]> countScriptTypesByOwnerId(@Param("userId") Long userId);

    /** 查询有待审核话术的场次 ID 列表 */
    @Query("SELECT DISTINCT s.sessionId FROM LiveScript s WHERE s.deleted = 0 AND s.approvalStatus = 1")
    List<Long> findSessionIdsWithPendingApproval();

    /** 最近 N 条话术（用户维度重复检测，按创建时间倒序） */
    @Query("SELECT s FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE sess.userId = :userId AND s.deleted = 0 AND (:excludeId IS NULL OR s.id <> :excludeId) ORDER BY s.createTime DESC")
    List<LiveScript> findRecentScriptsForDuplicateCheck(@Param("userId") Long userId, @Param("excludeId") Long excludeScriptId, Pageable pageable);

    /** 按用户 ID 取效果最高的话术 */
    @Query("SELECT s FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE sess.userId = :userId AND s.deleted = 0 AND s.effectivenessScore IS NOT NULL ORDER BY s.effectivenessScore DESC")
    List<LiveScript> findTopByUserIdOrderByEffectivenessScoreDesc(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    /** 按场次 ID 和商品 ID 查询话术 */
    @Query("SELECT s FROM LiveScript s WHERE s.sessionId = :sessionId AND s.productId = :productId AND s.deleted = 0")
    List<LiveScript> findBySessionIdAndProductId(@Param("sessionId") Long sessionId, @Param("productId") Long productId);
}
