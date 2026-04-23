package cn.gaifan.douyinOperations.module.wecom.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 企业微信机器人配置表
 * 与 sql/wecom/schema.sql 中 wc_robot_config 一一对应
 */
@Data
@Entity
@Table(name = "wc_robot_config")
@SQLRestriction("deleted = 0")
public class WcRobotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "robot_name", nullable = false, length = 128)
    private String robotName;

    @Column(name = "webhook_url", nullable = false, length = 512)
    private String webhookUrl;

    /** 机器人类型：data_report / alert / task_reminder / custom */
    @Column(name = "robot_type", nullable = false, length = 32)
    private String robotType = "custom";

    /** 状态：0=禁用 1=启用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "description", length = 256)
    private String description;

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
