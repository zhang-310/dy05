package cn.gaifan.douyinOperations.module.douyin.repository;

import cn.gaifan.douyinOperations.module.douyin.entity.DyFanProfileStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Repository
public interface DyFanProfileStatsRepository extends JpaRepository<DyFanProfileStats, Long> {

    /**
     * 查找账号的统计数据
     */
    List<DyFanProfileStats> findByAccountIdAndDeletedOrderByCountDesc(Long accountId, Integer deleted);

    /**
     * 查找账号指定类型的统计数据
     */
    List<DyFanProfileStats> findByAccountIdAndStatTypeAndDeletedOrderByCountDesc(
            Long accountId, String statType, Integer deleted);

    /**
     * 删除账号的旧统计数据
     */
    @Modifying
    @Query("UPDATE DyFanProfileStats s SET s.deleted = 1 WHERE s.accountId = :accountId AND s.deleted = 0")
    void deleteOldStatsByAccountId(@Param("accountId") Long accountId);

    /**
     * UPSERT 粉丝画像统计数据（PostgreSQL）
     * 使用原生 SQL 实现 ON CONFLICT DO UPDATE
     */
    @Modifying
    @Query(value = """
        INSERT INTO dy_fan_profile_stats
            (account_id, owner_id, stat_type, stat_key, stat_value, count, percentage, sync_time, deleted, create_time, update_time)
        VALUES
            (:accountId, :ownerId, :statType, :statKey, :statValue, :count, :percentage, :syncTime, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (account_id, stat_type, stat_key)
        DO UPDATE SET
            count = EXCLUDED.count,
            percentage = EXCLUDED.percentage,
            stat_value = EXCLUDED.stat_value,
            sync_time = EXCLUDED.sync_time,
            deleted = 0,
            update_time = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void upsertFanStat(
        @Param("accountId") Long accountId,
        @Param("ownerId") Long ownerId,
        @Param("statType") String statType,
        @Param("statKey") String statKey,
        @Param("statValue") String statValue,
        @Param("count") Long count,
        @Param("percentage") BigDecimal percentage,
        @Param("syncTime") Timestamp syncTime
    );
}
