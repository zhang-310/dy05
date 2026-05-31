package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiSearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

/**
 * 知识库检索日志 Repository
 */
public interface AiSearchLogRepository extends JpaRepository<AiSearchLog, Long> {

    /**
     * 统计某文档在指定时间范围内被检索命中的次数
     * hit_doc_ids 逗号分隔，pattern 为 '%,docId,%' 用于精确匹配
     */
    @Query(value = "SELECT COUNT(*) FROM ai_search_log l WHERE l.owner_id = :ownerId AND l.deleted = 0 " +
            "AND l.create_time >= :since AND l.hit_doc_ids IS NOT NULL " +
            "AND (',' || l.hit_doc_ids || ',') LIKE :pattern",
            nativeQuery = true)
    long countHitsForDocSince(@Param("ownerId") Long ownerId, @Param("pattern") String pattern,
                              @Param("since") Timestamp since);

    @Query("SELECT COUNT(l) FROM AiSearchLog l WHERE l.deleted = 0 AND l.createTime >= :since AND (l.hitCount IS NULL OR l.hitCount = 0)")
    long countZeroHitSince(@Param("since") Timestamp since);

    @Query("SELECT COUNT(l) FROM AiSearchLog l WHERE l.deleted = 0 AND l.createTime >= :since")
    long countSince(@Param("since") Timestamp since);

    @Query(value = "SELECT l.query_text FROM ai_search_log l WHERE l.owner_id = :ownerId AND l.deleted = 0 " +
            "AND (:prefix IS NULL OR :prefix = '' OR LOWER(l.query_text) LIKE LOWER(CONCAT('%', :prefix, '%'))) " +
            "GROUP BY l.query_text ORDER BY COUNT(*) DESC LIMIT :lim",
            nativeQuery = true)
    List<String> findTopQueriesByOwner(@Param("ownerId") Long ownerId, @Param("prefix") String prefix, @Param("lim") int lim);

    /** F-5 仪表盘：全租户近窗口热词（管理端） */
    @Query(value = "SELECT l.query_text FROM ai_search_log l WHERE l.deleted = 0 AND l.create_time >= :since " +
            "AND l.query_text IS NOT NULL AND TRIM(l.query_text) <> '' " +
            "GROUP BY l.query_text ORDER BY COUNT(*) DESC LIMIT :lim",
            nativeQuery = true)
    List<String> findTopQueriesSince(@Param("since") Timestamp since, @Param("lim") int lim);
}
