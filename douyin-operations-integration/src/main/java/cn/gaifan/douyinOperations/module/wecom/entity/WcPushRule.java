package cn.gaifan.douyinOperations.module.wecom.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 企业微信推送规则表
 * 与 sql/wecom/schema.sql 中 wc_push_rule 一一对应
 */
@Data
@Entity
@Table(name = "wc_push_rule")
@SQLRestriction("deleted = 0")
public class WcPushRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "robot_id", nullable = false)
    private Long robotId;

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    /** 触发类型：scheduled / event */
    @Column(name = "trigger_type", nullable = false, length = 16)
    private String triggerType;

    @Column(name = "trigger_config", nullable = false, columnDefinition = "TEXT")
    private String triggerConfig;

    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate;

    /** 状态：0=禁用 1=启用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "last_trigger_time")
    private Timestamp lastTriggerTime;

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
