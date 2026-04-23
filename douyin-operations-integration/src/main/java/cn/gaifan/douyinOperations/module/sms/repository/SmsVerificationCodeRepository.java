package cn.gaifan.douyinOperations.module.sms.repository;

import cn.gaifan.douyinOperations.module.sms.entity.SmsVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface SmsVerificationCodeRepository extends JpaRepository<SmsVerificationCode, Long>, JpaSpecificationExecutor<SmsVerificationCode> {

    Optional<SmsVerificationCode> findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc(String phoneNumber, String bizType);

    List<SmsVerificationCode> findByPhoneNumberAndBizTypeAndIsVerified(String phoneNumber, String bizType, Integer isVerified);

    @Modifying
    @Query("UPDATE SmsVerificationCode c SET c.attemptCount = c.attemptCount + 1 WHERE c.id = :id")
    void incrementAttemptCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE SmsVerificationCode c SET c.isVerified = 1, c.verifiedTime = :verifiedTime, c.verifiedIp = :verifiedIp WHERE c.id = :id")
    void markAsVerified(@Param("id") Long id, @Param("verifiedTime") Timestamp verifiedTime, @Param("verifiedIp") String verifiedIp);

    @Modifying
    @Query(value = "DELETE FROM sms_verification_code WHERE expires_at < :expiryTime", nativeQuery = true)
    void deleteExpiredCodes(@Param("expiryTime") Timestamp expiryTime);
}
