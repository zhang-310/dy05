package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolutionReviewTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface AiEvolutionReviewTaskRepository extends JpaRepository<AiEvolutionReviewTask, Long>,
        JpaSpecificationExecutor<AiEvolutionReviewTask> {

    List<AiEvolutionReviewTask> findByReviewStatusAndDeletedOrderByCreateTimeDesc(String status, int deleted);

    @Query("SELECT COUNT(r) FROM AiEvolutionReviewTask r WHERE r.reviewStatus = :status AND r.deleted = 0")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(r) FROM AiEvolutionReviewTask r WHERE r.reviewStatus IN ('APPROVED','REVISED') AND r.createTime >= :since AND r.deleted = 0")
    long countApprovedSince(@Param("since") Timestamp since);

    @Query("SELECT COUNT(r) FROM AiEvolutionReviewTask r WHERE r.createTime >= :since AND r.deleted = 0")
    long countTotalSince(@Param("since") Timestamp since);

    List<AiEvolutionReviewTask> findByReviewStatusAndAutoExpiredAndCreateTimeBefore(
            String status, boolean autoExpired, Timestamp before);
}
