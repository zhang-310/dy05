package cn.gaifan.douyinOperations.module.agent.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 智能体对话分享
 * 支持分享对话给其他用户，支持导入查看
 */
@Data
@Entity
@Table(name = "agent_share")
@SQLRestriction("deleted = 0")
public class AgentShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 分享码（8位随机字符串，唯一） */
    @Column(name = "share_code", nullable = false, unique = true, length = 32)
    private String shareCode;

    /** 对话ID */
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /** 智能体ID */
    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    /** 分享人ID */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 对话标题 */
    @Column(name = "title", nullable = false, length = 256)
    private String title;

    /** 对话摘要 */
    @Column(name = "summary", length = 512)
    private String summary;

    /** 消息数量 */
    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    /** 浏览次数 */
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    /** 是否公开：1=公开, 0=私密 */
    @Column(name = "is_public", nullable = false)
    private Integer isPublic = 1;

    /** 过期时间（为空表示永不过期） */
    @Column(name = "expires_at")
    private Timestamp expiresAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
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
