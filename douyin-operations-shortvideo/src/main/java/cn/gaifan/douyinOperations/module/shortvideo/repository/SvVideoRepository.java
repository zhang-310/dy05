package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface SvVideoRepository extends JpaRepository<SvVideo, Long>, JpaSpecificationExecutor<SvVideo> {

    Optional<SvVideo> findByIdAndDeleted(Long id, Integer deleted);

    List<SvVideo> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    List<SvVideo> findByOwnerIdAndPublishTimeAfterAndDeleted(Long ownerId, Timestamp after, Integer deleted);

    /** 内容日历：发布时间在范围内的视频 */
    @org.springframework.data.jpa.repository.Query("SELECT v FROM SvVideo v WHERE v.ownerId IN :ownerIds AND v.deleted = 0 " +
            "AND v.publishTime IS NOT NULL AND v.publishTime BETWEEN :startTs AND :endTs ORDER BY v.publishTime")
    List<SvVideo> findByOwnerIdInAndPublishTimeBetweenAndDeleted(
            @Param("ownerIds") List<Long> ownerIds, @Param("startTs") Timestamp startTs,
            @Param("endTs") Timestamp endTs);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM SvVideo v WHERE v.deleted = 0 AND v.publishTime IS NOT NULL AND v.publishTime BETWEEN :startTs AND :endTs ORDER BY v.publishTime")
    List<SvVideo> findByPublishTimeBetweenAndDeleted(@Param("startTs") Timestamp startTs, @Param("endTs") Timestamp endTs);

    Optional<SvVideo> findByDouyinVideoIdAndDeleted(String douyinVideoId, Integer deleted);

    @Modifying
    @Query("UPDATE SvVideo v SET v.viewCount = v.viewCount + 1 WHERE v.id = :id")
    void incrementViewCount(@Param("id") Long id);

    /** 账号下视频的 view_count 列表（用于计算均值） */
    @Query("SELECT v.viewCount FROM SvVideo v WHERE v.accountId = :accountId AND v.viewCount IS NOT NULL AND v.viewCount > 0")
    List<Long> findViewCountsByAccountId(@Param("accountId") Long accountId);

    // === Dashboard 统计方法 ===

    /** 统计总短视频数（未删除） */
    long countByDeleted(Integer deleted);

    /** 统计某用户的短视频数 */
    long countByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    /** 统计某用户在某日期后的短视频数 */
    long countByOwnerIdAndCreateTimeAfterAndDeleted(
            @Param("ownerId") Long ownerId,
            @Param("createTime") Timestamp createTime,
            @Param("deleted") Integer deleted);

    /** 统计今日创建的短视频数（全局） */
    long countByCreateTimeAfterAndDeleted(Timestamp createTime, Integer deleted);

    /** 统计已发布的短视频数（基于 publishTime 不为空） */
    @Query("SELECT COUNT(v) FROM SvVideo v WHERE v.deleted = 0 AND v.publishTime IS NOT NULL")
    long countPublishedVideos();

    /** 统计某用户已发布的短视频数 */
    @Query("SELECT COUNT(v) FROM SvVideo v WHERE v.ownerId = :ownerId AND v.deleted = 0 AND v.publishTime IS NOT NULL")
    long countPublishedVideosByOwnerId(@Param("ownerId") Long ownerId);

    /** 统计今日已发布的短视频数 */
    @Query("SELECT COUNT(v) FROM SvVideo v WHERE v.deleted = 0 AND v.publishTime IS NOT NULL AND v.publishTime >= :startTime")
    long countPublishedVideosSince(@Param("startTime") Timestamp startTime);

    /** 统计某用户今日已发布的短视频数 */
    @Query("SELECT COUNT(v) FROM SvVideo v WHERE v.ownerId = :ownerId AND v.deleted = 0 AND v.publishTime IS NOT NULL AND v.publishTime >= :startTime")
    long countPublishedVideosByOwnerIdSince(@Param("ownerId") Long ownerId, @Param("startTime") Timestamp startTime);
}
