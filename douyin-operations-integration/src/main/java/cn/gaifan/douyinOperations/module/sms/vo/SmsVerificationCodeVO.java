package cn.gaifan.douyinOperations.module.sms.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class SmsVerificationCodeVO {
    private Long id;
    private Long ownerId;
    private String phoneNumber;
    private String bizType;
    private Integer attemptCount;
    private Integer maxAttempts;
    private Integer isVerified;
    private Timestamp verifiedTime;
    private Timestamp expiresAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
