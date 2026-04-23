package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiIndexQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiIndexQueueRepository extends JpaRepository<AiIndexQueue, Long>, JpaSpecificationExecutor<AiIndexQueue> {

    @Query("SELECT q FROM AiIndexQueue q WHERE q.status = 'pending' AND q.retryCount < 3 ORDER BY q.priority ASC, q.createTime ASC")
    List<AiIndexQueue> findPendingTasks(org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("UPDATE AiIndexQueue q SET q.status = :status, q.retryCount = q.retryCount + 1, q.errorMsg = :errorMsg WHERE q.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status, @Param("errorMsg") String errorMsg);

    /** 乐观锁：仅当 status=pending 时更新为 processing，返回影响行数 */
    @Modifying
    @Query("UPDATE AiIndexQueue q SET q.status = 'processing' WHERE q.id = :id AND q.status = 'pending'")
    int markProcessing(@Param("id") Long id);

    @Modifying
    @Query("UPDATE AiIndexQueue q SET q.status = 'done' WHERE q.id = :id")
    void markDone(@Param("id") Long id);

    @Modifying
    @Query("UPDATE AiIndexQueue q SET q.status = 'pending', q.retryCount = q.retryCount + 1, q.errorMsg = :errorMsg WHERE q.id = :id")
    void markRetry(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    @Modifying
    @Query("UPDATE AiIndexQueue q SET q.status = 'failed', q.retryCount = q.retryCount + 1, q.errorMsg = :errorMsg WHERE q.id = :id")
    void markFailed(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    @Modifying
    @Query("DELETE FROM AiIndexQueue q WHERE q.status = 'done'")
    int cleanDone();

    @Query("SELECT COUNT(q) FROM AiIndexQueue q WHERE q.status = 'pending' AND q.retryCount < 3")
    long countPendingEligible();

    /**
     * pending 且可重试任务中，最老一条的等待时长（毫秒）；无待处理行时为 0。PostgreSQL。
     */
    @Query(value = "SELECT COALESCE(MAX(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - q.create_time)) * 1000), 0) "
            + "FROM ai_index_queue q WHERE q.status = 'pending' AND q.retry_count < 3",
            nativeQuery = true)
    double maxPendingLagMsEligible();
}
