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

/**
 * 直播场次 Repository
 */
public interface LiveSessionRepository extends JpaRepository<LiveSession, Long>, JpaSpecificationExecutor<LiveSession> {

    /**
     * 根据 ID 和用户 ID 查询（未删除）
     */
    Optional<LiveSession> findByIdAndUserIdAndDeleted(Long id, Long userId, Integer deleted);

    /**
     * 根据 ID 查询（未删除）
     */
    Optional<LiveSession> findByIdAndDeleted(Long id, Integer deleted);

    /**
     * 根据用户 ID 查询（未删除，分页）
     */
    Page<LiveSession> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);

    /**
     * 根据状态查询（未删除，分页）
     */
    Page<LiveSession> findByStatusAndDeleted(Integer status, Integer deleted, Pageable pageable);

    /**
     * 根据用户 ID 和状态查询（未删除，分页）
     */
    Page<LiveSession> findByUserIdAndStatusAndDeleted(Long userId, Integer status, Integer deleted, Pageable pageable);

    /**
     * 查询所有（未删除，分页）
     */
    Page<LiveSession> findByDeleted(Integer deleted, Pageable pageable);

    /**
     * 统计用户直播总数
     */
    long countByUserIdAndDeleted(Long userId, Integer deleted);

    /**
     * 按可见用户 ID 列表查询场次 ID（用于子资源数据范围过滤）
     */
    @Query("SELECT s.id FROM LiveSession s WHERE s.userId IN :userIds AND s.deleted = 0")
    List<Long> findIdsByUserIdIn(@Param("userIds") List<Long> userIds);

    /**
     * 根据账号 ID 和时间范围查询直播场次
     */
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

    /**
     * 历史对比：按用户 ID 列表、已结束状态，按结束时间倒序
     */
    Page<LiveSession> findByUserIdInAndStatusAndDeletedOrderByEndTimeDesc(
            List<Long> userIds, Integer status, Integer deleted, Pageable pageable);

    /**
     * 历史对比：按账号 ID 筛选
     */
    Page<LiveSession> findByUserIdInAndAccountIdAndStatusAndDeletedOrderByEndTimeDesc(
            List<Long> userIds, Long accountId, Integer status, Integer deleted, Pageable pageable);

    /**
     * 历史对比：管理员不限用户（status=ended）
     */
    Page<LiveSession> findByStatusAndDeletedOrderByEndTimeDesc(Integer status, Integer deleted, Pageable pageable);

    Page<LiveSession> findByAccountIdAndStatusAndDeletedOrderByEndTimeDesc(Long accountId, Integer status, Integer deleted, Pageable pageable);

    /** 上一场直播（同账号、已结束、结束时间早于指定时间） */
    Optional<LiveSession> findTop1ByAccountIdAndStatusAndDeletedAndEndTimeBeforeOrderByEndTimeDesc(
            Long accountId, Integer status, Integer deleted, Timestamp beforeEndTime);

    /** 按用户 ID 列表统计场次数（机构 Dashboard 用） */
    @Query("SELECT COUNT(s) FROM LiveSession s WHERE s.userId IN :userIds AND s.deleted = :deleted")
    long countByUserIdInAndDeleted(@Param("userIds") List<Long> userIds, @Param("deleted") Integer deleted);

    /** 超时自动同步：状态=进行中 且 scheduledEndTime 超时 且 autoSyncEnabled=1 */
    List<LiveSession> findByStatusAndScheduledEndTimeBeforeAndAutoSyncEnabledAndDeleted(
            Integer status, Timestamp scheduledEndTime, Integer autoSyncEnabled, Integer deleted);

    /** 超时自动同步：状态=进行中 且 scheduledEndTime 超时 */
    List<LiveSession> findByStatusAndScheduledEndTimeBeforeAndDeletedOrderByScheduledEndTimeAsc(
            Integer status, Timestamp scheduledEndTime, Integer deleted);

    /** 超时自动同步（plannedEndTime 维度） */
    List<LiveSession> findByStatusAndPlannedEndTimeBeforeAndAutoSyncEnabledAndDeleted(
            Integer status, Timestamp plannedEndTime, Integer autoSyncEnabled, Integer deleted);

    /** GMV 对账：已结束且 endTime 在指定时间之后的场次 */
    @Query("SELECT s FROM LiveSession s WHERE s.deleted = 0 AND s.status = 2 AND s.endTime >= :since ORDER BY s.endTime DESC")
    List<LiveSession> findEndedSessionsWithEndTimeSince(@Param("since") Timestamp since);

    /** 按人设 ID + userId + deleted 查询 */
    List<LiveSession> findByPersonaIdAndUserIdAndDeleted(Long personaId, Long userId, Integer deleted);

    /** 按计划开始时间范围查询 */
    @Query("SELECT s FROM LiveSession s WHERE s.deleted = 0 AND s.scheduledTime >= :start AND s.scheduledTime < :end ORDER BY s.scheduledTime ASC")
    List<LiveSession> findScheduledBetween(@Param("start") Timestamp start, @Param("end") Timestamp end);

    /** 搜索场次（按标题关键词，支持分页） */
    @Query("SELECT s FROM LiveSession s WHERE s.deleted = 0 AND s.userId = :userId AND LOWER(s.liveTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY s.scheduledTime DESC")
    Page<LiveSession> searchByTitleKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);
}
