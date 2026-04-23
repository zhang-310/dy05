package cn.gaifan.douyinOperations.module.payment.repository;

import cn.gaifan.douyinOperations.module.payment.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
    @Query("SELECT COALESCE(SUM(u.delta), 0) FROM UsageRecord u WHERE u.userId = :userId AND u.metric = :metric AND u.recordedAt >= :since AND u.deleted = 0")
    int sumUsageSince(@Param("userId") Long userId, @Param("metric") String metric, @Param("since") Timestamp since);
}
