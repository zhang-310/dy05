package cn.gaifan.douyinOperations.module.sms.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 短信模板表
 * 与 sql/sms/schema.sql 中 sms_template 一一对应
 */
@Data
@Entity
@Table(name = "sms_template")
@SQLRestriction("deleted = 0")
public class SmsTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 模板代码（唯一标识） */
    @Column(name = "template_code", nullable = false, length = 64)
    private String templateCode;

    /** 模板名称 */
    @Column(name = "template_name", nullable = false, length = 128)
    private String templateName;

    /** 模板内容（支持 {{variable}} 占位符） */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 关联的服务商代码 */
    @Column(name = "provider_code", nullable = false, length = 32)
    private String providerCode;

    /** 服务商的模板 ID */
    @Column(name = "provider_template_id", length = 128)
    private String providerTemplateId;

    /** 状态：0=禁用 1=启用 2=审核中 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 模板类型：verification=验证码, notification=通知, marketing=营销 */
    @Column(name = "template_type", nullable = false, length = 32)
    private String templateType = "notification";

    /** 备注 */
    @Column(name = "remark", length = 256)
    private String remark;

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
