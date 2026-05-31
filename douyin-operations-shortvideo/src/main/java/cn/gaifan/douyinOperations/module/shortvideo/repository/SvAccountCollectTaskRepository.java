package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccountCollectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface SvAccountCollectTaskRepository extends JpaRepository<SvAccountCollectTask, Long>,
        JpaSpecificationExecutor<SvAccountCollectTask> {

    Optional<SvAccountCollectTask> findByIdAndDeleted(Long id, Integer deleted);

    List<SvAccountCollectTask> findByOwnerIdAndDeletedOrderByCreateTimeDesc(Long ownerId, Integer deleted);

    long countByOwnerIdAndStatusAndDeleted(Long ownerId, String status, Integer deleted);

    long countByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    @Query(value = """
            SELECT COUNT(*)
            FROM sv_account_collect_task
            WHERE owner_id = :ownerId
              AND deleted = 0
              AND status = 'pending'
              AND (next_run_at IS NULL OR next_run_at <= CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    long countDuePending(@Param("ownerId") Long ownerId);

    @Query(value = """
            SELECT COUNT(*)
            FROM sv_account_collect_task
            WHERE owner_id = :ownerId
              AND deleted = 0
              AND status = 'collecting'
              AND lease_until IS NOT NULL
              AND lease_until < CURRENT_TIMESTAMP
            """, nativeQuery = true)
    long countExpiredCollecting(@Param("ownerId") Long ownerId);

    @Query(value = """
            SELECT COUNT(*)
            FROM sv_account_collect_task
            WHERE owner_id = :ownerId
              AND deleted = 0
              AND status = 'failed'
              AND retry_count < max_retry_count
              AND (error_message IS NULL OR (
                    error_message NOT LIKE '%不可自动重试%'
                AND error_message NOT LIKE '%Cookie%'
                AND error_message NOT LIKE '%验证码%'
                AND error_message NOT LIKE '%安全验证%'
              ))
            """, nativeQuery = true)
    long countRetryableFailed(@Param("ownerId") Long ownerId);

    @Query(value = """
            SELECT COALESCE(worker_id, 'unassigned') AS worker_id,
                   COALESCE(worker_region, 'unknown') AS worker_region,
                   COUNT(*) AS collecting_tasks,
                   SUM(CASE WHEN lease_until IS NOT NULL AND lease_until < CURRENT_TIMESTAMP THEN 1 ELSE 0 END) AS expired_tasks,
                   MAX(last_heartbeat_at) AS last_heartbeat_at,
                   MAX(lease_until) AS max_lease_until,
                   MIN(claimed_at) AS first_claimed_at
            FROM sv_account_collect_task
            WHERE owner_id = :ownerId
              AND deleted = 0
              AND status = 'collecting'
            GROUP BY COALESCE(worker_id, 'unassigned'), COALESCE(worker_region, 'unknown')
            ORDER BY collecting_tasks DESC, worker_id ASC
            """, nativeQuery = true)
    List<Object[]> aggregateWorkerHealth(@Param("ownerId") Long ownerId);

    @Query(value = """
            SELECT id, account_name, input_type, retry_count, max_retry_count, error_message, update_time
            FROM sv_account_collect_task
            WHERE owner_id = :ownerId
              AND deleted = 0
              AND status = 'failed'
            ORDER BY update_time DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findRecentFailedRows(@Param("ownerId") Long ownerId, @Param("limit") int limit);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET status = :status,
                error_message = :errorMessage,
                finished_at = CASE WHEN :status IN ('collected', 'completed', 'failed') THEN CURRENT_TIMESTAMP ELSE finished_at END,
                lease_until = CASE WHEN :status IN ('collected', 'completed', 'failed') THEN NULL ELSE lease_until END,
                update_time = CURRENT_TIMESTAMP
            WHERE id = :id
            """, nativeQuery = true)
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("errorMessage") String errorMessage);

    @Modifying
    @Transactional
    @Query("UPDATE SvAccountCollectTask t SET t.analyzedVideos = t.analyzedVideos + 1 WHERE t.id = :id")
    int incrementAnalyzedVideos(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE SvAccountCollectTask t SET t.indexedVideos = t.indexedVideos + 1 WHERE t.id = :id")
    int incrementIndexedVideos(@Param("id") Long id);

    @Query(value = """
            SELECT id
            FROM sv_account_collect_task
            WHERE deleted = 0
              AND status = 'pending'
              AND (next_run_at IS NULL OR next_run_at <= CURRENT_TIMESTAMP)
            ORDER BY create_time ASC, id ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Long> findClaimablePendingTaskIds(@Param("limit") int limit);

    @Query(value = """
            SELECT id
            FROM sv_account_collect_task
            WHERE deleted = 0
              AND status = 'collecting'
              AND lease_until IS NOT NULL
              AND lease_until < CURRENT_TIMESTAMP
            ORDER BY lease_until ASC, id ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Long> findExpiredCollectingTaskIds(@Param("limit") int limit);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET status = 'collecting',
                worker_id = :workerId,
                worker_region = :workerRegion,
                lease_until = :leaseUntil,
                claimed_at = CURRENT_TIMESTAMP,
                started_at = COALESCE(started_at, CURRENT_TIMESTAMP),
                last_heartbeat_at = CURRENT_TIMESTAMP,
                update_time = CURRENT_TIMESTAMP,
                error_message = NULL
            WHERE id = :id
              AND deleted = 0
              AND (
                    status = 'pending'
                    OR (status = 'collecting' AND lease_until IS NOT NULL AND lease_until < CURRENT_TIMESTAMP)
                  )
            """, nativeQuery = true)
    int claimForCollect(@Param("id") Long id,
                        @Param("workerId") String workerId,
                        @Param("workerRegion") String workerRegion,
                        @Param("leaseUntil") Timestamp leaseUntil);

    @Query(value = """
            SELECT id
            FROM sv_account_collect_task
            WHERE deleted = 0
              AND (
                    (status = 'pending' AND (next_run_at IS NULL OR next_run_at <= CURRENT_TIMESTAMP))
                    OR (status = 'collecting' AND lease_until IS NOT NULL AND lease_until < CURRENT_TIMESTAMP)
                  )
            ORDER BY
              CASE WHEN status = 'collecting' THEN 0 ELSE 1 END,
              COALESCE(lease_until, next_run_at, create_time) ASC,
              id ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Long> findOneClaimableRemoteTaskId();

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET lease_until = :leaseUntil,
                last_heartbeat_at = CURRENT_TIMESTAMP,
                update_time = CURRENT_TIMESTAMP
            WHERE id = :id
              AND worker_id = :workerId
              AND status = 'collecting'
              AND deleted = 0
            """, nativeQuery = true)
    int heartbeat(@Param("id") Long id,
                  @Param("workerId") String workerId,
                  @Param("leaseUntil") Timestamp leaseUntil);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET account_name = COALESCE(:accountName, account_name),
                sec_uid = COALESCE(:secUid, sec_uid),
                total_videos = :totalVideos,
                collected_videos = :collectedVideos,
                status = :status,
                error_message = :errorMessage,
                lease_until = NULL,
                finished_at = CASE WHEN :status IN ('failed', 'completed') THEN CURRENT_TIMESTAMP ELSE finished_at END,
                update_time = CURRENT_TIMESTAMP
            WHERE id = :id
              AND worker_id = :workerId
              AND deleted = 0
              AND status = 'collecting'
            """, nativeQuery = true)
    int completeRemoteCollect(@Param("id") Long id,
                              @Param("workerId") String workerId,
                              @Param("accountName") String accountName,
                              @Param("secUid") String secUid,
                              @Param("totalVideos") int totalVideos,
                              @Param("collectedVideos") int collectedVideos,
                              @Param("status") String status,
                              @Param("errorMessage") String errorMessage);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET status = 'pending',
                retry_count = retry_count + 1,
                worker_id = NULL,
                worker_region = NULL,
                lease_until = NULL,
                next_run_at = :nextRunAt,
                error_message = :errorMessage,
                update_time = CURRENT_TIMESTAMP
            WHERE id = :id
              AND worker_id = :workerId
              AND deleted = 0
              AND status = 'collecting'
              AND retry_count < max_retry_count
            """, nativeQuery = true)
    int releaseWorkerTaskForRetry(@Param("id") Long id,
                                  @Param("workerId") String workerId,
                                  @Param("nextRunAt") Timestamp nextRunAt,
                                  @Param("errorMessage") String errorMessage);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET status = 'pending',
                retry_count = retry_count + 1,
                worker_id = NULL,
                worker_region = NULL,
                lease_until = NULL,
                next_run_at = :nextRunAt,
                error_message = :errorMessage,
                update_time = CURRENT_TIMESTAMP
            WHERE id = :id
              AND deleted = 0
              AND retry_count < max_retry_count
            """, nativeQuery = true)
    int releaseForRetry(@Param("id") Long id,
                        @Param("nextRunAt") Timestamp nextRunAt,
                        @Param("errorMessage") String errorMessage);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET status = 'failed',
                worker_id = NULL,
                worker_region = NULL,
                lease_until = NULL,
                finished_at = CURRENT_TIMESTAMP,
                error_message = :errorMessage,
                update_time = CURRENT_TIMESTAMP
            WHERE id = :id
              AND deleted = 0
            """, nativeQuery = true)
    int markFailedTerminal(@Param("id") Long id, @Param("errorMessage") String errorMessage);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET status = 'failed',
                worker_id = NULL,
                worker_region = NULL,
                lease_until = NULL,
                finished_at = CURRENT_TIMESTAMP,
                error_message = :errorMessage,
                update_time = CURRENT_TIMESTAMP
            WHERE id = :id
              AND worker_id = :workerId
              AND deleted = 0
              AND status = 'collecting'
            """, nativeQuery = true)
    int markWorkerTaskFailedTerminal(@Param("id") Long id,
                                     @Param("workerId") String workerId,
                                     @Param("errorMessage") String errorMessage);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET status = 'pending',
                retry_count = retry_count + 1,
                worker_id = NULL,
                worker_region = NULL,
                lease_until = NULL,
                next_run_at = CURRENT_TIMESTAMP,
                error_message = :errorMessage,
                update_time = CURRENT_TIMESTAMP
            WHERE owner_id = :ownerId
              AND deleted = 0
              AND status = 'collecting'
              AND lease_until IS NOT NULL
              AND lease_until < CURRENT_TIMESTAMP
              AND retry_count < max_retry_count
            """, nativeQuery = true)
    int releaseExpiredCollectingForRetry(@Param("ownerId") Long ownerId,
                                         @Param("errorMessage") String errorMessage);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET status = 'failed',
                worker_id = NULL,
                worker_region = NULL,
                lease_until = NULL,
                finished_at = CURRENT_TIMESTAMP,
                error_message = :errorMessage,
                update_time = CURRENT_TIMESTAMP
            WHERE owner_id = :ownerId
              AND deleted = 0
              AND status = 'collecting'
              AND lease_until IS NOT NULL
              AND lease_until < CURRENT_TIMESTAMP
              AND retry_count >= max_retry_count
            """, nativeQuery = true)
    int markExpiredCollectingTerminal(@Param("ownerId") Long ownerId,
                                      @Param("errorMessage") String errorMessage);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_task
            SET status = 'pending',
                retry_count = retry_count + 1,
                worker_id = NULL,
                worker_region = NULL,
                lease_until = NULL,
                claimed_at = NULL,
                finished_at = NULL,
                next_run_at = CURRENT_TIMESTAMP,
                error_message = :errorMessage,
                update_time = CURRENT_TIMESTAMP
            WHERE owner_id = :ownerId
              AND deleted = 0
              AND status = 'failed'
              AND retry_count < max_retry_count
              AND (error_message IS NULL OR (
                    error_message NOT LIKE '%不可自动重试%'
                AND error_message NOT LIKE '%Cookie%'
                AND error_message NOT LIKE '%验证码%'
                AND error_message NOT LIKE '%安全验证%'
              ))
            """, nativeQuery = true)
    int retryFailedTasks(@Param("ownerId") Long ownerId,
                         @Param("errorMessage") String errorMessage);
}
