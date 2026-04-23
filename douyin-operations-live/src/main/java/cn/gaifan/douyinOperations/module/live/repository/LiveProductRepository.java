package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 直播产品 Repository
 */
public interface LiveProductRepository extends JpaRepository<LiveProduct, Long> {

    /**
     * 根据直播场次 ID 查询产品
     */
    List<LiveProduct> findBySessionId(Long sessionId);

    /** 按场次 ID 和逻辑删除状态查询 */
    List<LiveProduct> findBySessionIdAndDeleted(Long sessionId, Integer deleted);

    /**
     * 根据直播场次 ID 和产品 ID 查询
     */
    Optional<LiveProduct> findBySessionIdAndProductId(Long sessionId, Long productId);

    /**
     * 根据直播场次 ID 分页查询
     */
    Page<LiveProduct> findBySessionId(Long sessionId, Pageable pageable);

    /**
     * 根据多个场次 ID 分页查询（DataScope 数据范围过滤）
     */
    Page<LiveProduct> findBySessionIdIn(List<Long> sessionIds, Pageable pageable);

    /**
     * 删除直播场次关联的所有产品
     */
    long deleteBySessionId(Long sessionId);

    /**
     * 统计场次关联产品数量
     */
    long countBySessionId(Long sessionId);

    @Query("SELECT COALESCE(SUM(p.revenue), 0) FROM LiveProduct p WHERE p.createTime >= :createTime")
    BigDecimal sumRevenueByCreateTimeAfter(@Param("createTime") Timestamp createTime);

    @Query("SELECT COALESCE(SUM(p.revenue), 0) FROM LiveProduct p JOIN LiveSession s ON p.sessionId = s.id WHERE s.userId = :ownerId AND p.createTime >= :createTime")
    BigDecimal sumRevenueByOwnerIdAndCreateTimeAfter(@Param("ownerId") Long ownerId, @Param("createTime") Timestamp createTime);

    /** 按场次 ID 查询产品（按 position + id 升序，用于话术模板排列） */
    List<LiveProduct> findBySessionIdOrderByPositionAscIdAsc(Long sessionId);

    /** 按场次 ID 汇总产品收益 */
    @Query("SELECT COALESCE(SUM(p.revenue), 0) FROM LiveProduct p WHERE p.sessionId = :sessionId AND p.deleted = 0")
    java.math.BigDecimal sumRevenueBySessionId(@Param("sessionId") Long sessionId);

    /** 批量按场次 ID 汇总收益，返回 Object[]{sessionId, sum} */
    @Query("SELECT p.sessionId, COALESCE(SUM(p.revenue), 0) FROM LiveProduct p WHERE p.sessionId IN :sessionIds AND p.deleted = 0 GROUP BY p.sessionId")
    List<Object[]> sumRevenueGroupBySessionIdIn(@Param("sessionIds") List<Long> sessionIds);

    /** 按时间范围汇总收益 */
    @Query("SELECT COALESCE(SUM(p.revenue), 0) FROM LiveProduct p WHERE p.deleted = 0 AND p.createTime BETWEEN :start AND :end")
    java.math.BigDecimal sumRevenueBetween(@Param("start") java.sql.Timestamp start, @Param("end") java.sql.Timestamp end);
}
