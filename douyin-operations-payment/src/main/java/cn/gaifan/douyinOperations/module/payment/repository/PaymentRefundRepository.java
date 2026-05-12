package cn.gaifan.douyinOperations.module.payment.repository;

import cn.gaifan.douyinOperations.module.payment.entity.PaymentRefund;
import cn.gaifan.douyinOperations.module.payment.entity.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 退款仓储
 */
@Repository
public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long>, JpaSpecificationExecutor<PaymentRefund> {

    /**
     * 按订单 ID 查询所有退款
     */
    List<PaymentRefund> findByOrderId(Long orderId);

    /**
     * 按订单 ID 和状态查询
     */
    Optional<PaymentRefund> findByOrderIdAndStatus(Long orderId, RefundStatus status);

    /**
     * 分页查询
     */
    Page<PaymentRefund> findAll(Pageable pageable);

    /**
     * 按状态计数
     */
    Long countByStatus(RefundStatus status);

    /**
     * P1-11: 计算订单已退款总额（数据库聚合查询）
     * 仅统计已完成和已批准的退款
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM PaymentRefund r WHERE r.orderId = :orderId AND (r.status = 'COMPLETED' OR r.status = 'APPROVED')")
    BigDecimal sumRefundedAmountByOrderId(@Param("orderId") Long orderId);
}
