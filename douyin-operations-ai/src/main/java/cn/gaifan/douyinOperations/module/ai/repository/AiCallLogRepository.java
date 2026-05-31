package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long>, JpaSpecificationExecutor<AiCallLog> {

    Page<AiCallLog> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Page<AiCallLog> findByCallTypeOrderByCreateTimeDesc(String callType, Pageable pageable);

    Page<AiCallLog> findByUserIdAndCallTypeOrderByCreateTimeDesc(Long userId, String callType, Pageable pageable);

    /** 待归因：已关联视频/场次但未计算效果 */
    @Query("SELECT l FROM AiCallLog l WHERE (l.linkedVideoId IS NOT NULL OR l.linkedSessionId IS NOT NULL) AND l.contentEffect IS NULL")
    List<AiCallLog> findPendingAttribution();

    /** 按时间范围统计调用量（按日） */
    @Query(value = "SELECT DATE(create_time) AS d, COUNT(*) AS c FROM ai_call_log WHERE create_time >= :start AND create_time < :end AND status = 1 GROUP BY DATE(create_time) ORDER BY d", nativeQuery = true)
    List<Object[]> countByDateRange(@Param("start") Timestamp start, @Param("end") Timestamp end);

    /** 按时间范围统计调用量（按日，可选 callType 筛选） */
    @Query(value = "SELECT DATE(create_time) AS d, COUNT(*) AS c FROM ai_call_log WHERE create_time >= :start AND create_time < :end AND status = 1 AND (:callType IS NULL OR call_type = :callType) GROUP BY DATE(create_time) ORDER BY d", nativeQuery = true)
    List<Object[]> countByDateRangeAndCallType(@Param("start") Timestamp start, @Param("end") Timestamp end, @Param("callType") String callType);

    /** 按日 + call_type 聚合调用量（管理端额度/功能用量看板） */
    @Query(value = "SELECT DATE(create_time) AS d, call_type, COUNT(*) AS c FROM ai_call_log WHERE create_time >= :start AND create_time < :end AND status = 1 GROUP BY DATE(create_time), call_type ORDER BY d, call_type", nativeQuery = true)
    List<Object[]> aggregateDailyByCallType(@Param("start") Timestamp start, @Param("end") Timestamp end);

    /** 按时间范围统计调用量（按小时，最近 N 小时） */
    @Query(value = "SELECT date_trunc('hour', create_time) AS h, COUNT(*) AS c FROM ai_call_log WHERE create_time >= :start AND create_time < :end AND status = 1 AND (:callType IS NULL OR call_type = :callType) GROUP BY date_trunc('hour', create_time) ORDER BY h", nativeQuery = true)
    List<Object[]> countByHourRange(@Param("start") Timestamp start, @Param("end") Timestamp end, @Param("callType") String callType);

    /** 按 call_type 统计调用量 */
    @Query("SELECT l.callType, COUNT(l) FROM AiCallLog l WHERE l.createTime >= :start AND l.createTime < :end AND l.status = 1 GROUP BY l.callType")
    List<Object[]> countByCallType(@Param("start") Timestamp start, @Param("end") Timestamp end);

    /** 有 referenced_chunk_ids 的调用记录（用于知识引用率） */
    @Query("SELECT l FROM AiCallLog l WHERE l.referencedChunkIds IS NOT NULL AND l.referencedChunkIds != '' AND l.createTime >= :since")
    List<AiCallLog> findWithReferencedChunksSince(@Param("since") Timestamp since);

    @Query("SELECT COUNT(l) FROM AiCallLog l WHERE l.referencedChunkIds IS NOT NULL AND l.referencedChunkIds != '' AND l.createTime >= :since")
    long countWithReferencedChunksSince(@Param("since") Timestamp since);

    @Query("SELECT COUNT(l) FROM AiCallLog l WHERE l.createTime >= :since")
    long countCallsSince(@Param("since") Timestamp since);

    @Query("SELECT MAX(l.createTime) FROM AiCallLog l WHERE l.referencedChunkIds IS NOT NULL AND l.referencedChunkIds != ''")
    Timestamp findLastReferencedAt();

    // === Dashboard 统计方法 ===

    /** 统计今日调用总数 */
    long countByCreateTimeAfterAndStatus(Timestamp createTime, Integer status);

    /** 统计某用户今日调用总数 */
    long countByUserIdAndCreateTimeAfterAndStatus(Long userId, Timestamp createTime, Integer status);

    /** 统计今日成功调用数 */
    @Query("SELECT COUNT(l) FROM AiCallLog l WHERE l.createTime >= :startTime AND l.status = 1")
    long countSuccessCallsSince(@Param("startTime") Timestamp startTime);

    /** 统计某用户今日成功调用数 */
    @Query("SELECT COUNT(l) FROM AiCallLog l WHERE l.userId = :userId AND l.createTime >= :startTime AND l.status = 1")
    long countSuccessCallsByUserIdSince(@Param("userId") Long userId, @Param("startTime") Timestamp startTime);

    /** 统计今日失败调用数 */
    @Query("SELECT COUNT(l) FROM AiCallLog l WHERE l.createTime >= :startTime AND l.status != 1")
    long countFailedCallsSince(@Param("startTime") Timestamp startTime);

    /** 统计某用户今日失败调用数 */
    @Query("SELECT COUNT(l) FROM AiCallLog l WHERE l.userId = :userId AND l.createTime >= :startTime AND l.status != 1")
    long countFailedCallsByUserIdSince(@Param("userId") Long userId, @Param("startTime") Timestamp startTime);

    /** 统计总调用数（不限时间） */
    long countByStatus(Integer status);

    /** 统计某用户总调用数 */
    long countByUserIdAndStatus(Long userId, Integer status);

    /** 统计指定时间之后的总 token 消耗（优先 total_tokens，回退 prompt_tokens + completion_tokens） */
    @Query("SELECT COALESCE(SUM(COALESCE(l.totalTokens, COALESCE(l.promptTokens, 0) + COALESCE(l.completionTokens, 0))), 0) FROM AiCallLog l WHERE l.createTime >= :startTime AND l.status = 1")
    long sumTotalTokensSince(@Param("startTime") Timestamp startTime);

    /** 按 call_type 汇总 Token 与调用次数（成功记录，时间窗内） */
    @Query("SELECT l.callType, COALESCE(SUM(COALESCE(l.totalTokens, COALESCE(l.promptTokens, 0) + COALESCE(l.completionTokens, 0))), 0), COUNT(l) "
            + "FROM AiCallLog l WHERE l.createTime >= :start AND l.createTime < :end AND l.status = 1 GROUP BY l.callType")
    List<Object[]> aggregateTokensByCallType(@Param("start") Timestamp start, @Param("end") Timestamp end);

    /**
     * 从真实调用日志聚合模型基准数据。modelCode 可能是 model:{id}，也可能是模型版本字符串。
     */
    @Query("SELECT l.callType, l.templateCode, l.modelCode, " +
            "AVG(COALESCE(l.durationMs, 0)), " +
            "SUM(CASE WHEN l.status = 1 THEN 1 ELSE 0 END) * 1.0 / COUNT(l), " +
            "COALESCE(AVG(COALESCE(l.totalTokens, COALESCE(l.promptTokens, 0) + COALESCE(l.completionTokens, 0))), 0), " +
            "COUNT(l) " +
            "FROM AiCallLog l WHERE (:taskCode IS NULL OR l.callType = :taskCode OR l.templateCode = :taskCode) " +
            "AND l.modelCode IS NOT NULL AND l.modelCode <> '' " +
            "GROUP BY l.callType, l.templateCode, l.modelCode " +
            "ORDER BY l.callType ASC, l.modelCode ASC")
    List<Object[]> aggregateModelBenchmarkFromCallLog(@Param("taskCode") String taskCode);
}
