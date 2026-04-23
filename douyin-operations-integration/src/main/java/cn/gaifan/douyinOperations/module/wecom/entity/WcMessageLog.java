package cn.gaifan.douyinOperations.module.wecom.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 企业微信消息日志表
 * 与 sql/wecom/schema.sql 中 wc_message_log 一一对应
 * 注意：无 deleted 字段，不支持逻辑删除
 */
@Data
@Entity
@Table(name = "wc_message_log")
public class WcMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "robot_id", nullable = false)
    private Long robotId;

    @Column(name = "rule_id")
    private Long ruleId;

    /** 消息类型：text / markdown / news */
    @Column(name = "message_type", nullable = false, length = 16)
    private String messageType = "text";

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    /** 发送状态：0=失败 1=成功 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "send_time", nullable = false)
    private Timestamp sendTime;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (sendTime == null) sendTime = new Timestamp(System.currentTimeMillis());
    }
}
