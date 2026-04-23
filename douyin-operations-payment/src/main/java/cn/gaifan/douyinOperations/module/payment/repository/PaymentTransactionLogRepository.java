package cn.gaifan.douyinOperations.module.payment.repository;

import cn.gaifan.douyinOperations.module.payment.entity.PaymentTransactionLog;
import cn.gaifan.douyinOperations.module.payment.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支付交易日志仓储
 */
@Repository
public interface PaymentTransactionLogRepository extends JpaRepository<PaymentTransactionLog, Long>, JpaSpecificationExecutor<PaymentTransactionLog> {

    /**
     * 按外部交易号查询（幂等），对应实体字段 {@code externalTransactionId}
     */
    Optional<PaymentTransactionLog> findByExternalTransactionId(String externalTransactionId);

    /**
     * 按订单 ID 查询
     */
    List<PaymentTransactionLog> findByOrderId(Long orderId);

    /**
     * 按订单 ID 和状态查询
     */
    Optional<PaymentTransactionLog> findByOrderIdAndStatus(Long orderId, TransactionStatus status);

    /**
     * 按用户 ID 分页查询（通过订单表关联，交易日志表无 user_id 列）
     */
    @Query("SELECT l FROM PaymentTransactionLog l JOIN PaymentOrder o ON l.orderId = o.id WHERE o.userId = :userId AND o.deleted = 0")
    Page<PaymentTransactionLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 按状态分页查询
     */
    Page<PaymentTransactionLog> findByStatus(TransactionStatus status, Pageable pageable);
}
