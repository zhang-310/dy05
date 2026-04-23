package cn.gaifan.douyinOperations.module.sms.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 短信发送日志表
 * 与 sql/sms/schema.sql 中 sms_send_log 一一对应
 * 注意：无 deleted 字段，日志不支持逻辑删除，超期由定时任务物理清理
 */
@Data
@Entity
@Table(name = "sms_send_log")
public class SmsSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 目标手机号 */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /** 使用的模板代码 */
    @Column(name = "template_code", nullable = false, length = 64)
    private String templateCode;

    /** 服务商代码 */
    @Column(name = "provider_code", nullable = false, length = 32)
    private String providerCode;

    /** 服务商返回的请求 ID */
    @Column(name = "provider_request_id", length = 128)
    private String providerRequestId;

    /** 实际发送的短信内容 */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 状态：pending=待发送, sending=发送中, success=成功, failed=失败 */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "pending";

    /** 错误信息（失败时） */
    @Column(name = "error_message", length = 256)
    private String errorMessage;

    /** 错误代码（服务商返回） */
    @Column(name = "error_code", length = 32)
    private String errorCode;

    /** 发送时间 */
    @Column(name = "send_time")
    private Timestamp sendTime;

    /** 送达时间（如服务商支持） */
    @Column(name = "delivered_time")
    private Timestamp deliveredTime;

    /** 成本（通常按条数计算） */
    @Column(name = "cost")
    private BigDecimal cost;

    /** 业务 ID（用于关联业务数据，如注册/登录） */
    @Column(name = "biz_id", length = 128)
    private String bizId;

    /** 业务类型：register/login/password_reset/binding */
    @Column(name = "biz_type", length = 32)
    private String bizType;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
