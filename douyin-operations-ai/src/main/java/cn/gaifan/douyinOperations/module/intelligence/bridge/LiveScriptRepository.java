package cn.gaifan.douyinOperations.module.intelligence.bridge;

import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository("intelligenceLiveScriptRepository")
public interface LiveScriptRepository extends JpaRepository<LiveScript, Long> {

    List<LiveScript> findBySessionIdAndDeleted(Long sessionId, Integer deleted);

    List<LiveScript> findBySessionIdAndDeletedOrderBySequenceNoAsc(Long sessionId, Integer deleted);

    Page<LiveScript> findBySessionIdAndDeleted(Long sessionId, Integer deleted, Pageable pageable);

    Page<LiveScript> findBySessionIdInAndDeleted(List<Long> sessionIds, Integer deleted, Pageable pageable);

    List<LiveScript> findBySessionIdAndExecutedAndDeleted(Long sessionId, Integer executed, Integer deleted);

    long deleteBySessionId(Long sessionId);

    long countBySessionIdAndDeleted(Long sessionId, Integer deleted);

    @Query("SELECT s.sessionId, COUNT(s) FROM LiveScript s WHERE s.sessionId IN :sessionIds AND s.deleted = 0 GROUP BY s.sessionId")
    List<Object[]> countGroupBySessionIdIn(@Param("sessionIds") List<Long> sessionIds);

    @Query("SELECT MAX(s.sequenceNo) FROM LiveScript s WHERE s.sessionId = :sessionId AND s.deleted = 0")
    Integer findMaxSequenceNoBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT s FROM LiveScript s WHERE s.deleted = 0 AND s.effectivenessScore IS NOT NULL AND s.effectivenessScore >= :threshold")
    List<LiveScript> findByEffectivenessScoreGreaterThanEqual(@Param("threshold") BigDecimal threshold);

    @Query("SELECT s FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE sess.userId = :userId AND s.deleted = 0 AND s.effectivenessScore IS NOT NULL AND s.effectivenessScore >= :threshold")
    List<LiveScript> findByUserIdAndEffectivenessScoreGreaterThanEqual(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);

    @Query("SELECT s FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE sess.userId = :userId AND s.deleted = 0 AND s.effectivenessScore IS NOT NULL AND s.effectivenessScore >= :threshold")
    List<LiveScript> findByUserIdAndEffectivenessScoreGte(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);

    @Query("SELECT s FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE s.id IN :ids AND sess.userId = :userId AND s.deleted = 0")
    List<LiveScript> findByIdInAndUserId(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    @Query("SELECT s.scriptType, COUNT(s) FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE sess.userId = :userId AND s.deleted = 0 AND s.scriptType IS NOT NULL GROUP BY s.scriptType")
    List<Object[]> countScriptTypesByOwnerId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT s.sessionId FROM LiveScript s WHERE s.deleted = 0 AND s.approvalStatus = 1")
    List<Long> findSessionIdsWithPendingApproval();

    @Query("SELECT s FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE sess.userId = :userId AND s.deleted = 0 AND (:excludeId IS NULL OR s.id <> :excludeId) ORDER BY s.createTime DESC")
    List<LiveScript> findRecentScriptsForDuplicateCheck(@Param("userId") Long userId, @Param("excludeId") Long excludeScriptId, Pageable pageable);

    @Query("SELECT s FROM LiveScript s JOIN LiveSession sess ON s.sessionId = sess.id WHERE sess.userId = :userId AND s.deleted = 0 AND s.effectivenessScore IS NOT NULL ORDER BY s.effectivenessScore DESC")
    List<LiveScript> findTopByUserIdOrderByEffectivenessScoreDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM LiveScript s WHERE s.sessionId = :sessionId AND s.productId = :productId AND s.deleted = 0")
    List<LiveScript> findBySessionIdAndProductId(@Param("sessionId") Long sessionId, @Param("productId") Long productId);
}
