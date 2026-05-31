package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiCallQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface AiCallQuotaRepository extends JpaRepository<AiCallQuota, Long> {

    Optional<AiCallQuota> findByUserIdAndQuotaDate(Long userId, Date quotaDate);

    /** 额度趋势：按日汇总 used_count、max_count */
    @Query(value = "SELECT quota_date AS d, SUM(used_count) AS used, SUM(max_count) AS max FROM ai_call_quota WHERE quota_date >= :start AND quota_date <= :end GROUP BY quota_date ORDER BY d", nativeQuery = true)
    List<Object[]> aggregateByDateRange(@Param("start") Date start, @Param("end") Date end);

    @Modifying
    @Query("UPDATE AiCallQuota q SET q.maxCount = :maxCount WHERE q.quotaDate = :quotaDate")
    int updateMaxCountByQuotaDate(@Param("quotaDate") Date quotaDate, @Param("maxCount") Integer maxCount);
}
