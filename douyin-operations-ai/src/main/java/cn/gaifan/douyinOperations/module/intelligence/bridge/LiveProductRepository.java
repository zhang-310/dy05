package cn.gaifan.douyinOperations.module.intelligence.bridge;

import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository("intelligenceLiveProductRepository")
public interface LiveProductRepository extends JpaRepository<LiveProduct, Long> {

    List<LiveProduct> findBySessionId(Long sessionId);

    List<LiveProduct> findBySessionIdAndDeleted(Long sessionId, Integer deleted);

    Optional<LiveProduct> findBySessionIdAndProductId(Long sessionId, Long productId);

    Page<LiveProduct> findBySessionId(Long sessionId, Pageable pageable);

    Page<LiveProduct> findBySessionIdIn(List<Long> sessionIds, Pageable pageable);

    long deleteBySessionId(Long sessionId);

    long countBySessionId(Long sessionId);

    @Query("SELECT COALESCE(SUM(p.revenue), 0) FROM LiveProduct p WHERE p.createTime >= :createTime")
    BigDecimal sumRevenueByCreateTimeAfter(@Param("createTime") Timestamp createTime);

    @Query("SELECT COALESCE(SUM(p.revenue), 0) FROM LiveProduct p JOIN LiveSession s ON p.sessionId = s.id WHERE s.userId = :ownerId AND p.createTime >= :createTime")
    BigDecimal sumRevenueByOwnerIdAndCreateTimeAfter(@Param("ownerId") Long ownerId, @Param("createTime") Timestamp createTime);

    List<LiveProduct> findBySessionIdOrderByPositionAscIdAsc(Long sessionId);

    @Query("SELECT COALESCE(SUM(p.revenue), 0) FROM LiveProduct p WHERE p.sessionId = :sessionId AND p.deleted = 0")
    BigDecimal sumRevenueBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT p.sessionId, COALESCE(SUM(p.revenue), 0) FROM LiveProduct p WHERE p.sessionId IN :sessionIds AND p.deleted = 0 GROUP BY p.sessionId")
    List<Object[]> sumRevenueGroupBySessionIdIn(@Param("sessionIds") List<Long> sessionIds);

    @Query("SELECT COALESCE(SUM(p.revenue), 0) FROM LiveProduct p WHERE p.deleted = 0 AND p.createTime BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("start") Timestamp start, @Param("end") Timestamp end);

    /**
     * 搜索产品（按名称关键词，支持分页）
     */
    @Query("SELECT p FROM LiveProduct p WHERE p.deleted = 0 AND LOWER(p.productName) LIKE LOWER(CONCAT('%', :kw, '%'))")
    Page<LiveProduct> searchProducts(@Param("kw") String keyword, Pageable pageable);
}
