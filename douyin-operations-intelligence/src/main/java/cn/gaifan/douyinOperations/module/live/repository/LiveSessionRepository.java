package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface LiveSessionRepository extends JpaRepository<LiveSession, Long>, JpaSpecificationExecutor<LiveSession> {

    Optional<LiveSession> findByIdAndUserIdAndDeleted(Long id, Long userId, Integer deleted);

    Optional<LiveSession> findByIdAndDeleted(Long id, Integer deleted);

    Page<LiveSession> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);

    Page<LiveSession> findByStatusAndDeleted(Integer status, Integer deleted, Pageable pageable);

    Page<LiveSession> findByUserIdAndStatusAndDeleted(Long userId, Integer status, Integer deleted, Pageable pageable);

    Page<LiveSession> findByDeleted(Integer deleted, Pageable pageable);

    long countByUserIdAndDeleted(Long userId, Integer deleted);

    @Query("SELECT s.id FROM LiveSession s WHERE s.userId IN :userIds AND s.deleted = 0")
    List<Long> findIdsByUserIdIn(@Param("userIds") List<Long> userIds);

    List<LiveSession> findByAccountIdAndStartTimeBetweenAndDeleted(
            Long accountId, Timestamp startTime, Timestamp endTime, Integer deleted);

    long countByDeleted(Integer deleted);

    long countByStatusAndDeleted(Integer status, Integer deleted);

    long countByCreateTimeAfterAndDeleted(Timestamp createTime, Integer deleted);

    @Query("SELECT COUNT(s) FROM LiveSession s WHERE s.userId = :ownerId AND s.deleted = :deleted")
    long countByOwnerIdAndDeleted(@Param("ownerId") Long ownerId, @Param("deleted") Integer deleted);

    @Query("SELECT COUNT(s) FROM LiveSession s WHERE s.userId = :ownerId AND s.status = :status AND s.deleted = :deleted")
    long countByOwnerIdAndStatusAndDeleted(@Param("ownerId") Long ownerId, @Param("status") Integer status, @Param("deleted") Integer deleted);

    @Query("SELECT COUNT(s) FROM LiveSession s WHERE s.userId = :ownerId AND s.createTime >= :createTime AND s.deleted = :deleted")
    long countByOwnerIdAndCreateTimeAfterAndDeleted(@Param("ownerId") Long ownerId, @Param("createTime") Timestamp createTime, @Param("deleted") Integer deleted);

    Page<LiveSession> findByUserIdInAndStatusAndDeletedOrderByEndTimeDesc(
            List<Long> userIds, Integer status, Integer deleted, Pageable pageable);

    Page<LiveSession> findByUserIdInAndAccountIdAndStatusAndDeletedOrderByEndTimeDesc(
            List<Long> userIds, Long accountId, Integer status, Integer deleted, Pageable pageable);

    Page<LiveSession> findByStatusAndDeletedOrderByEndTimeDesc(Integer status, Integer deleted, Pageable pageable);

    Page<LiveSession> findByAccountIdAndStatusAndDeletedOrderByEndTimeDesc(Long accountId, Integer status, Integer deleted, Pageable pageable);

    Optional<LiveSession> findTop1ByAccountIdAndStatusAndDeletedAndEndTimeBeforeOrderByEndTimeDesc(
            Long accountId, Integer status, Integer deleted, Timestamp beforeEndTime);

    @Query("SELECT COUNT(s) FROM LiveSession s WHERE s.userId IN :userIds AND s.deleted = :deleted")
    long countByUserIdInAndDeleted(@Param("userIds") List<Long> userIds, @Param("deleted") Integer deleted);

    List<LiveSession> findByStatusAndScheduledEndTimeBeforeAndAutoSyncEnabledAndDeleted(
            Integer status, Timestamp scheduledEndTime, Integer autoSyncEnabled, Integer deleted);

    List<LiveSession> findByStatusAndScheduledEndTimeBeforeAndDeletedOrderByScheduledEndTimeAsc(
            Integer status, Timestamp scheduledEndTime, Integer deleted);

    List<LiveSession> findByStatusAndPlannedEndTimeBeforeAndAutoSyncEnabledAndDeleted(
            Integer status, Timestamp plannedEndTime, Integer autoSyncEnabled, Integer deleted);

    @Query("SELECT s FROM LiveSession s WHERE s.deleted = 0 AND s.status = 2 AND s.endTime >= :since ORDER BY s.endTime DESC")
    List<LiveSession> findEndedSessionsWithEndTimeSince(@Param("since") Timestamp since);

    List<LiveSession> findByPersonaIdAndUserIdAndDeleted(Long personaId, Long userId, Integer deleted);

    @Query("SELECT s FROM LiveSession s WHERE s.deleted = 0 AND s.scheduledTime >= :start AND s.scheduledTime < :end ORDER BY s.scheduledTime ASC")
    List<LiveSession> findScheduledBetween(@Param("start") Timestamp start, @Param("end") Timestamp end);

    /**
     * 搜索场次（按标题关键词，支持分页）
     */
    @Query("SELECT s FROM LiveSession s WHERE s.deleted = 0 AND s.userId = :userId AND LOWER(s.liveTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY s.scheduledTime DESC")
    Page<LiveSession> searchByTitleKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);
}
