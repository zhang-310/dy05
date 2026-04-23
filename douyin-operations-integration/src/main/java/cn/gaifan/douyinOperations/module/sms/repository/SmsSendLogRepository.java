package cn.gaifan.douyinOperations.module.sms.repository;

import cn.gaifan.douyinOperations.module.sms.entity.SmsSendLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface SmsSendLogRepository extends JpaRepository<SmsSendLog, Long>, JpaSpecificationExecutor<SmsSendLog> {

    Optional<SmsSendLog> findByIdAndOwnerId(Long id, Long ownerId);

    Page<SmsSendLog> findByOwnerIdOrderByCreateTimeDesc(Long ownerId, Pageable pageable);

    Page<SmsSendLog> findByOwnerIdAndStatusOrderByCreateTimeDesc(Long ownerId, String status, Pageable pageable);

    Page<SmsSendLog> findByOwnerIdAndPhoneNumberOrderByCreateTimeDesc(Long ownerId, String phoneNumber, Pageable pageable);

    List<SmsSendLog> findByOwnerIdAndBizTypeAndBizId(Long ownerId, String bizType, String bizId);

    @Query(value = "SELECT COUNT(*) FROM sms_send_log WHERE owner_id = :ownerId AND status = 'success' AND create_time::date = CURRENT_DATE", nativeQuery = true)
    long countDailySentByOwner(@Param("ownerId") Long ownerId);

    @Modifying
    @Query("UPDATE SmsSendLog l SET l.status = :status, l.deliveredTime = :deliveredTime WHERE l.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status, @Param("deliveredTime") Timestamp deliveredTime);

    @Modifying
    @Query(value = "DELETE FROM sms_send_log WHERE create_time < :expiryTime", nativeQuery = true)
    void deleteExpiredLogs(@Param("expiryTime") Timestamp expiryTime);
}
