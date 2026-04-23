package cn.gaifan.douyinOperations.module.douyin.repository;

import cn.gaifan.douyinOperations.module.douyin.entity.DyFanProfileStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
