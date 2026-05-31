package cn.gaifan.douyinOperations.module.agent.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 智能体评分与评论
 */
@Getter
@Setter
@Entity
@Table(name = "agent_review")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AgentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 评分 1-5 星 */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    /** 评论内容（可选） */
    @Column(name = "content", length = 1000)
    private String content;

    /** 智能体作者回复 */
    @Column(name = "reply_content", length = 1000)
    private String replyContent;

    /** 回复时间 */
    @Column(name = "reply_time")
    private Timestamp replyTime;

    /** 状态：1=可见 0=隐藏 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
        if (replyTime == null && replyContent != null) replyTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
