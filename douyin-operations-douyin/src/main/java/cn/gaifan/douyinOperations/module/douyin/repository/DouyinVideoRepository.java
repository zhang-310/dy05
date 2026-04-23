package cn.gaifan.douyinOperations.module.douyin.repository;

import cn.gaifan.douyinOperations.module.douyin.entity.DouyinVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.Optional;

/**
 * 抖音视频表 Repository
 */
public interface DouyinVideoRepository extends JpaRepository<DouyinVideo, Long>, JpaSpecificationExecutor<DouyinVideo> {

    Page<DouyinVideo> findByAccountIdAndDeleted(Long accountId, Integer deleted, Pageable pageable);

    Optional<DouyinVideo> findByIdAndDeleted(Long id, Integer deleted);

    Page<DouyinVideo> findByVideoTypeAndDeleted(String videoType, Integer deleted, Pageable pageable);

    boolean existsByVideoIdAndDeleted(String videoId, Integer deleted);

    Optional<DouyinVideo> findByVideoIdAndDeleted(String videoId, Integer deleted);

    long countByAccountIdAndDeleted(Long accountId, Integer deleted);

    @Query("SELECT COALESCE(SUM(v.viewCount), 0) FROM DouyinVideo v WHERE v.accountId = :accountId AND v.deleted = :deleted")
    Long sumViewCountByAccountIdAndDeleted(@Param("accountId") Long accountId, @Param("deleted") Integer deleted);

    @Query("SELECT COALESCE(SUM(v.likeCount), 0) FROM DouyinVideo v WHERE v.accountId = :accountId AND v.deleted = :deleted")
    Long sumLikeCountByAccountIdAndDeleted(@Param("accountId") Long accountId, @Param("deleted") Integer deleted);

    @Query("SELECT COALESCE(SUM(v.shareCount), 0) FROM DouyinVideo v WHERE v.accountId = :accountId AND v.deleted = :deleted")
    Long sumShareCountByAccountIdAndDeleted(@Param("accountId") Long accountId, @Param("deleted") Integer deleted);

    @Query("SELECT COALESCE(SUM(v.commentCount), 0) FROM DouyinVideo v WHERE v.accountId = :accountId AND v.deleted = :deleted")
    Long sumCommentCountByAccountIdAndDeleted(@Param("accountId") Long accountId, @Param("deleted") Integer deleted);

    @Query("SELECT COALESCE(SUM(v.downloadCount), 0) FROM DouyinVideo v WHERE v.accountId = :accountId AND v.deleted = :deleted")
    Long sumDownloadCountByAccountIdAndDeleted(@Param("accountId") Long accountId, @Param("deleted") Integer deleted);

    long countByDeleted(Integer deleted);

    @Query("SELECT COUNT(v) FROM DouyinVideo v WHERE v.publishTime IS NOT NULL AND (:status IS NULL OR :status >= 0) AND v.deleted = :deleted")
    long countByStatusAndDeleted(@Param("status") Integer status, @Param("deleted") Integer deleted);

    long countByCreateTimeAfterAndDeleted(Timestamp createTime, Integer deleted);

    @Query("SELECT COUNT(v) FROM DouyinVideo v JOIN DouyinAccount a ON v.accountId = a.id WHERE a.userId = :ownerId AND a.deleted = 0 AND v.deleted = :deleted")
    long countByOwnerIdAndDeleted(@Param("ownerId") Long ownerId, @Param("deleted") Integer deleted);

    @Query("SELECT COUNT(v) FROM DouyinVideo v JOIN DouyinAccount a ON v.accountId = a.id WHERE a.userId = :ownerId AND a.deleted = 0 AND v.publishTime IS NOT NULL AND (:status IS NULL OR :status >= 0) AND v.deleted = :deleted")
    long countByOwnerIdAndStatusAndDeleted(@Param("ownerId") Long ownerId, @Param("status") Integer status, @Param("deleted") Integer deleted);

    @Query("SELECT COUNT(v) FROM DouyinVideo v JOIN DouyinAccount a ON v.accountId = a.id WHERE a.userId = :ownerId AND a.deleted = 0 AND v.createTime >= :createTime AND v.deleted = :deleted")
    long countByOwnerIdAndCreateTimeAfterAndDeleted(@Param("ownerId") Long ownerId, @Param("createTime") Timestamp createTime, @Param("deleted") Integer deleted);
}
