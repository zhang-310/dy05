package cn.gaifan.douyinOperations.module.sms.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 短信验证码表
 * 与 sql/sms/schema.sql 中 sms_verification_code 一一对应
 * 说明：存储 OTP 验证码，短期数据，可定期清理
 * 注意：不需要 deleted 字段，依靠 expires_at 做过期清理
 */
@Data
@Entity
@Table(name = "sms_verification_code")
public class SmsVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 目标手机号 */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /** 业务类型：register/login/password_reset/binding */
    @Column(name = "biz_type", nullable = false, length = 32)
    private String bizType;

    /** 验证码（通常是 6 位数字） */
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    /** 尝试次数（防止暴力破解） */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    /** 最大尝试次数 */
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 5;

    /** 是否已验证：0=否 1=是 */
    @Column(name = "is_verified", nullable = false)
    private Integer isVerified = 0;

    /** 验证成功时间 */
    @Column(name = "verified_time")
    private Timestamp verifiedTime;

    /** 过期时间（通常是 5-10 分钟） */
    @Column(name = "expires_at", nullable = false)
    private Timestamp expiresAt;

    /** 创建时的客户端 IP */
    @Column(name = "created_ip", length = 45)
    private String createdIp;

    /** 验证时的客户端 IP */
    @Column(name = "verified_ip", length = 45)
    private String verifiedIp;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Timestamp(System.currentTimeMillis());
        if (updatedAt == null) updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
