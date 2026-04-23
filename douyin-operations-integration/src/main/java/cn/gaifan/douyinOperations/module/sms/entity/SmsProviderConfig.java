package cn.gaifan.douyinOperations.module.sms.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 短信服务商配置表
 * 与 sql/sms/schema.sql 中 sms_provider_config 一一对应
 */
@Data
@Entity
@Table(name = "sms_provider_config")
@SQLRestriction("deleted = 0")
public class SmsProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 服务商代码：tencent/aliyun/customize */
    @Column(name = "provider_code", nullable = false, length = 32)
    private String providerCode;

    /** 服务商名称 */
    @Column(name = "provider_name", nullable = false, length = 128)
    private String providerName;

    /** API Key / Access Key */
    @Column(name = "api_key", nullable = false, length = 256)
    private String apiKey;

    /** API Secret / Secret Key */
    @Column(name = "api_secret", nullable = false, length = 256)
    private String apiSecret;

    /** App ID（某些服务商需要） */
    @Column(name = "app_id", length = 128)
    private String appId;

    /** 短信签名 */
    @Column(name = "sign_name", nullable = false, length = 128)
    private String signName = "DefaultSign";

    /** 地区：cn/us/eu 等 */
    @Column(name = "region", length = 32)
    private String region = "cn";

    /** 状态：0=禁用 1=启用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 是否为默认配置：0=否 1=是 */
    @Column(name = "is_default", nullable = false)
    private Integer isDefault = 0;

    /** 每日发送配额 */
    @Column(name = "daily_quota")
    private Integer dailyQuota = 1000;

    /** 今日已发送数 */
    @Column(name = "daily_sent_count")
    private Integer dailySentCount = 0;

    /** 最后一次重置时间 */
    @Column(name = "last_reset_time")
    private Timestamp lastResetTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

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
