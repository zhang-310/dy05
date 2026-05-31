package cn.gaifan.douyinOperations.module.agent.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "agent_conversation")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AgentConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "conversation_topic", nullable = false, length = 256)
    private String conversationTopic;

    /** 状态：1=进行中 2=已完成 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @Column(name = "last_message_time")
    private Timestamp lastMessageTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    // P1-3: N+1 查询优化 - 关联智能体实体
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", insertable = false, updatable = false)
    private Agent agent;

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
