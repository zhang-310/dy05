package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SvViralVideoRepository extends JpaRepository<SvViralVideo, Long>, JpaSpecificationExecutor<SvViralVideo> {

    Optional<SvViralVideo> findByIdAndDeleted(Long id, Integer deleted);

    Page<SvViralVideo> findByOwnerIdAndDeleted(Long ownerId, Integer deleted, Pageable pageable);

    Optional<SvViralVideo> findByOwnerIdAndDouyinVideoIdAndDeleted(Long ownerId, String douyinVideoId, Integer deleted);

    List<SvViralVideo> findByIdInAndDeleted(java.util.List<Long> ids, Integer deleted);

    long countByDeleted(Integer deleted);

    long countByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    long countByDeepAnalyzeStatusAndDeleted(String deepAnalyzeStatus, Integer deleted);

    long countByOwnerIdAndDeepAnalyzeStatusAndDeleted(Long ownerId, String deepAnalyzeStatus, Integer deleted);

    long countByAutoCollectedAndDeleted(boolean autoCollected, Integer deleted);

    long countByOwnerIdAndAutoCollectedAndDeleted(Long ownerId, boolean autoCollected, Integer deleted);

    @Query("SELECT MAX(v.createTime) FROM SvViralVideo v WHERE v.deleted = 0")
    java.sql.Timestamp findLastCreateTime();

    @Query("SELECT MAX(v.createTime) FROM SvViralVideo v WHERE v.ownerId = :ownerId AND v.deleted = 0")
    java.sql.Timestamp findLastCreateTimeByOwnerId(@Param("ownerId") Long ownerId);

    List<SvViralVideo> findByOwnerIdAndDeletedOrderByViewCountDesc(Long ownerId, Integer deleted, Pageable pageable);

    /** 深度分析超时检测：status=processing 且 createTime < cutoff */
    @org.springframework.data.jpa.repository.Query("SELECT v FROM SvViralVideo v WHERE v.deleted = 0 AND v.deepAnalyzeStatus = 'processing' AND v.createTime < :before")
    List<SvViralVideo> findStaleDeepAnalyzeProcessing(@org.springframework.data.repository.query.Param("before") java.sql.Timestamp before);

    @Query("SELECT COUNT(v), COALESCE(SUM(v.viewCount),0), COALESCE(SUM(v.likeCount),0), COALESCE(SUM(v.shareCount),0), "
            + "COALESCE(AVG(COALESCE(v.viralScore,0)),0), COALESCE(MAX(v.viewCount),0), COALESCE(MIN(v.viewCount),0) "
            + "FROM SvViralVideo v WHERE v.svAccountId = :accountId AND v.deleted = 0")
    Object[] aggregateStatsBySvAccountId(@Param("accountId") Long accountId);

    @Query("SELECT v.deepAnalyzeStatus, COUNT(v) FROM SvViralVideo v WHERE v.svAccountId = :accountId AND v.deleted = 0 GROUP BY v.deepAnalyzeStatus")
    List<Object[]> countDeepAnalyzeGroupedBySvAccountId(@Param("accountId") Long accountId);
}
