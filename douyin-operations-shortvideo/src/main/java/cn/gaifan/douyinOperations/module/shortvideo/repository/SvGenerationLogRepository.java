package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

/**
 * 图生视频生成历史 Repository (Phase 6)
 */
public interface SvGenerationLogRepository extends JpaRepository<SvGenerationLog, Long> {

    List<SvGenerationLog> findByCreateTimeBetweenOrderByCreateTimeAsc(Timestamp start, Timestamp end);

    /** 按 owner 过滤：仅统计该用户项目下的生成日志 */
    @Query("SELECT g FROM SvGenerationLog g WHERE g.projectId IN (SELECT p.id FROM SvProject p WHERE p.ownerId = ?1) AND g.createTime BETWEEN ?2 AND ?3 ORDER BY g.createTime ASC")
    List<SvGenerationLog> findByOwnerIdAndCreateTimeBetweenOrderByCreateTimeAsc(Long ownerId, Timestamp start, Timestamp end);

    List<SvGenerationLog> findByOwnerIdAndContentTypeInOrderByCreateTimeDesc(
            Long ownerId, Collection<String> contentTypes, Pageable pageable);

    @Query("SELECT g.aiProvider, AVG(g.qualityScore) FROM SvGenerationLog g WHERE g.createTime BETWEEN ?1 AND ?2 AND g.success = true AND g.qualityScore IS NOT NULL GROUP BY g.aiProvider")
    List<Object[]> avgQualityByProvider(Timestamp start, Timestamp end);

    /** 按 owner 过滤的 Provider 排名 */
    @Query("SELECT g.aiProvider, AVG(g.qualityScore) FROM SvGenerationLog g WHERE g.projectId IN (SELECT p.id FROM SvProject p WHERE p.ownerId = ?1) AND g.createTime BETWEEN ?2 AND ?3 AND g.success = true AND g.qualityScore IS NOT NULL GROUP BY g.aiProvider")
    List<Object[]> avgQualityByProvider(Long ownerId, Timestamp start, Timestamp end);

    @Query("SELECT g.cameraType, AVG(g.qualityScore) FROM SvGenerationLog g WHERE g.createTime BETWEEN ?1 AND ?2 AND g.success = true AND g.qualityScore IS NOT NULL AND g.cameraType IS NOT NULL GROUP BY g.cameraType")
    List<Object[]> avgQualityByCameraType(Timestamp start, Timestamp end);

    /** 按 owner 过滤的运镜排名 */
    @Query("SELECT g.cameraType, AVG(g.qualityScore) FROM SvGenerationLog g WHERE g.projectId IN (SELECT p.id FROM SvProject p WHERE p.ownerId = ?1) AND g.createTime BETWEEN ?2 AND ?3 AND g.success = true AND g.qualityScore IS NOT NULL AND g.cameraType IS NOT NULL GROUP BY g.cameraType")
    List<Object[]> avgQualityByCameraType(Long ownerId, Timestamp start, Timestamp end);

    /** 按 owner 过滤的按日聚合（单次查询，避免 N+1） */
    @Query(value = "SELECT DATE(g.create_time) AS dt, AVG(g.quality_score) AS avg_score, COUNT(*) AS cnt " +
        "FROM sv_generation_log g " +
        "WHERE g.project_id IN (SELECT id FROM sv_project WHERE owner_id = ?1 AND deleted = 0) " +
        "AND g.create_time BETWEEN ?2 AND ?3 AND g.success = true AND g.quality_score IS NOT NULL " +
        "GROUP BY DATE(g.create_time) ORDER BY dt ASC", nativeQuery = true)
    List<Object[]> dailyAggregateByOwner(Long ownerId, Timestamp start, Timestamp end);
}
