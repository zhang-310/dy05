package cn.gaifan.douyinOperations.module.messaging.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 企微/飞书接入配置表
 */
@Data
@Entity
@Table(name = "msg_platform_config")
@SQLRestriction("deleted = 0")
public class MsgPlatformConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform;

    @Column(name = "app_id", length = 128)
    private String appId;

    @Column(name = "corp_id", length = 128)
    private String corpId;

    @Column(name = "secret", length = 512)
    private String secret;

    @Column(name = "callback_token", length = 256)
    private String callbackToken;

    @Column(name = "callback_encoding_aes_key", length = 256)
    private String callbackEncodingAesKey;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "extra_config", columnDefinition = "TEXT")
    private String extraConfig;

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
