package cn.gaifan.douyinOperations.module.payment.repository;

import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单仓储
 */
@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long>, JpaSpecificationExecutor<PaymentOrder> {

    /**
     * 按订单号查询（幂等）
     */
    Optional<PaymentOrder> findByOrderNo(String orderNo);

    /**
     * 按用户 ID 分页查询
     */
    Page<PaymentOrder> findByUserId(Long userId, Pageable pageable);

    /**
     * 按用户和状态查询
     */
    Page<PaymentOrder> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    /**
     * 按产品 ID 查询订单
     */
    List<PaymentOrder> findByProductId(Long productId);

    /**
     * 查询待支付订单（用于过期处理）
     */
    @Query("SELECT o FROM PaymentOrder o WHERE o.status = 'PENDING_PAYMENT' AND o.createdAt < :expiryTime")
    List<PaymentOrder> findExpiredPendingOrders(@Param("expiryTime") LocalDateTime expiryTime);

    /**
     * P1-8: 按状态和创建时间查询订单（用于超时取消）
     */
    List<PaymentOrder> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdAt);

    /**
     * 按日期范围统计金额
     */
    @Query("SELECT COALESCE(SUM(o.actualAmount), 0) FROM PaymentOrder o WHERE o.status = 'COMPLETED' AND o.completedAt BETWEEN :startTime AND :endTime")
    Long sumCompletedAmountByDateRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 按状态计数
     */
    Long countByStatus(OrderStatus status);

    /**
     * P1-13: GMV 对账：按直播场次 ID 和状态列表汇总实付金额
     * 用于直播模块统计场次 GMV
     */
    @Query("SELECT COALESCE(SUM(o.actualAmount), 0) FROM PaymentOrder o WHERE o.liveSessionId = :liveSessionId AND o.status IN :statuses")
    java.math.BigDecimal sumActualAmountByLiveSessionIdAndStatuses(@Param("liveSessionId") Long liveSessionId, @Param("statuses") List<OrderStatus> statuses);
}
