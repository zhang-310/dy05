package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

/**
 * 直播监控数据 Repository
 */
public interface LiveMonitorRepository extends JpaRepository<LiveMonitor, Long> {

    /**
     * 根据直播场次 ID 查询监控数据
     */
    List<LiveMonitor> findBySessionId(Long sessionId);

    /**
     * 根据直播场次 ID 查询监控数据（按时间升序，用于时序图）
     */
    List<LiveMonitor> findBySessionIdOrderByTimestampAsc(Long sessionId);

    /**
     * 根据直播场次 ID 和时间范围查询
     */
    List<LiveMonitor> findBySessionIdAndTimestampBetween(Long sessionId, Timestamp startTime, Timestamp endTime);

    /**
     * 根据直播场次 ID 分页查询
     */
    Page<LiveMonitor> findBySessionId(Long sessionId, Pageable pageable);

    /**
     * 根据多个场次 ID 分页查询（DataScope 数据范围过滤）
     */
    Page<LiveMonitor> findBySessionIdIn(List<Long> sessionIds, Pageable pageable);

    /**
     * 删除直播场次的监控数据
     */
    long deleteBySessionId(Long sessionId);

    /**
     * 归档：删除早于指定时间的监控数据（超过 90 天）
     */
    @Modifying
    @Query("DELETE FROM LiveMonitor m WHERE m.timestamp < :cutoff")
    int deleteByTimestampBefore(@Param("cutoff") Timestamp cutoff);

    /** GMV 对账：获取场次监控数据中最高 GMV 值 */
    @Query("SELECT MAX(m.gmv) FROM LiveMonitor m WHERE m.sessionId = :sessionId")
    java.math.BigDecimal findMaxGmvBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT COALESCE(MAX(m.gmv), 0) FROM LiveMonitor m WHERE m.deleted = 0")
    java.math.BigDecimal findGlobalMaxGmv();

    @Query("SELECT AVG(m.viewers) FROM LiveMonitor m WHERE m.deleted = 0")
    Double findGlobalAvgViewers();

    @Query("SELECT COUNT(m) FROM LiveMonitor m WHERE m.deleted = 0")
    long countActiveRecords();
}
