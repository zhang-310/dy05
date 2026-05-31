package cn.gaifan.douyinOperations.module.agent.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 智能体消息
 */
@Getter
@Setter
@Entity
@Table(name = "agent_message")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AgentMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /** 发送者类型：1=用户 2=智能体 */
    @Column(name = "sender_type", nullable = false)
    private Integer senderType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tokens")
    private Integer tokens = 0;

    /** 工具调用记录（JSON 数组） */
    @Column(name = "tool_calls", columnDefinition = "TEXT")
    private String toolCalls;

    /** 用户评价：up=好评 down=差评 null=未评价 */
    @Column(name = "rating", length = 8)
    private String rating;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
