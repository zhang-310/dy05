package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccountCollectWorkerNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface SvAccountCollectWorkerNodeRepository extends JpaRepository<SvAccountCollectWorkerNode, String> {

    List<SvAccountCollectWorkerNode> findByDeletedOrderByLastSeenAtDesc(Integer deleted);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO sv_account_collect_worker_node (
                worker_id, worker_region, status, current_task_id, lease_until,
                last_seen_at, last_claim_at, success_count, fail_count, deleted,
                create_time, update_time
            )
            VALUES (
                :workerId, :workerRegion, 'collecting', :taskId, :leaseUntil,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (worker_id) DO UPDATE SET
                worker_region = EXCLUDED.worker_region,
                status = 'collecting',
                current_task_id = EXCLUDED.current_task_id,
                lease_until = EXCLUDED.lease_until,
                last_seen_at = CURRENT_TIMESTAMP,
                last_claim_at = CURRENT_TIMESTAMP,
                last_error = NULL,
                deleted = 0,
                update_time = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int markClaimed(@Param("workerId") String workerId,
                    @Param("workerRegion") String workerRegion,
                    @Param("taskId") Long taskId,
                    @Param("leaseUntil") Timestamp leaseUntil);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO sv_account_collect_worker_node (
                worker_id, worker_region, status, current_task_id, lease_until, last_seen_at,
                success_count, fail_count, deleted, create_time, update_time
            )
            VALUES (
                :workerId, :workerRegion,
                CASE WHEN :taskId IS NULL THEN 'idle' ELSE 'collecting' END,
                :taskId, :leaseUntil, CURRENT_TIMESTAMP,
                0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (worker_id) DO UPDATE SET
                worker_region = COALESCE(EXCLUDED.worker_region, sv_account_collect_worker_node.worker_region),
                status = EXCLUDED.status,
                current_task_id = EXCLUDED.current_task_id,
                lease_until = COALESCE(EXCLUDED.lease_until, sv_account_collect_worker_node.lease_until),
                last_seen_at = CURRENT_TIMESTAMP,
                deleted = 0,
                update_time = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int markSeen(@Param("workerId") String workerId,
                 @Param("workerRegion") String workerRegion,
                 @Param("taskId") Long taskId,
                 @Param("leaseUntil") Timestamp leaseUntil);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_worker_node
            SET status = 'idle',
                current_task_id = NULL,
                lease_until = NULL,
                last_seen_at = CURRENT_TIMESTAMP,
                last_submit_at = CURRENT_TIMESTAMP,
                success_count = success_count + 1,
                last_error = NULL,
                update_time = CURRENT_TIMESTAMP
            WHERE worker_id = :workerId
              AND deleted = 0
            """, nativeQuery = true)
    int markSubmitted(@Param("workerId") String workerId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sv_account_collect_worker_node
            SET status = 'idle',
                current_task_id = NULL,
                lease_until = NULL,
                last_seen_at = CURRENT_TIMESTAMP,
                last_fail_at = CURRENT_TIMESTAMP,
                fail_count = fail_count + 1,
                last_error = :errorMessage,
                update_time = CURRENT_TIMESTAMP
            WHERE worker_id = :workerId
              AND deleted = 0
            """, nativeQuery = true)
    int markFailed(@Param("workerId") String workerId, @Param("errorMessage") String errorMessage);
}
