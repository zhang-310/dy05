package cn.gaifan.douyinOperations.module.payment.repository;

import cn.gaifan.douyinOperations.module.payment.entity.PaymentCallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * P1-7: 支付回调日志 Repository
 */
@Repository
public interface PaymentCallbackLogRepository extends JpaRepository<PaymentCallbackLog, Long> {

    Optional<PaymentCallbackLog> findByCallbackId(String callbackId);

    @Query("SELECT COUNT(c) FROM PaymentCallbackLog c WHERE c.orderId = :orderId AND c.receivedAt > :since")
    int countRecentCallbacks(@Param("orderId") String orderId, @Param("since") LocalDateTime since);
}
