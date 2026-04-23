package cn.gaifan.douyinOperations.module.payment.repository;

import cn.gaifan.douyinOperations.module.payment.entity.PaymentRefund;
import cn.gaifan.douyinOperations.module.payment.entity.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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
}
