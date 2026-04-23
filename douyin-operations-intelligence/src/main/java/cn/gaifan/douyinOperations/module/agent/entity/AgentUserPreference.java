package cn.gaifan.douyinOperations.module.agent.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * Agent 用户偏好与长期记忆表
 */
@Data
@Entity
@Table(name = "agent_user_preference")
@SQLRestriction("deleted = 0")
public class AgentUserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pref_key", nullable = false, length = 128)
    private String prefKey;

    @Column(name = "pref_value", nullable = false, length = 512)
    private String prefValue;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 1;

    @Column(name = "last_used_at", nullable = false)
    private Timestamp lastUsedAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
        if (lastUsedAt == null) lastUsedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
        if (lastUsedAt == null) lastUsedAt = updateTime;
    }
}
