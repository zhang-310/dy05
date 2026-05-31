package cn.gaifan.douyinOperations.module.ai.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 进化主题池表
 */
@Getter
@Setter
@Entity
@Table(name = "ai_evolve_topic")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AiEvolveTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id")
    private Long kbId;

    /** 抖音账号ID(douyin_account.id)，与kbId互斥：归属知识库时kbId有值，归属抖音时accountId有值，全局时两者均为null */
    @Column(name = "account_id")
    private Long accountId;

    /** 持久化列 topic；API 与前端统一用 JSON 属性名 topicName */
    @Column(name = "topic", nullable = false, length = 256)
    @JsonProperty("topicName")
    @JsonAlias({ "topic", "topicName" })
    private String topic;

    @Column(name = "category", length = 32)
    private String category = "basic";

    /** 1=P1 紧急，2=P2 普通，3=P3 低（与进化页下拉一致） */
    @Column(name = "priority", nullable = false)
    private Integer priority = 2;

    @Column(name = "source", nullable = false, length = 16)
    private String source = "initial";

    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(name = "last_used_time")
    private Timestamp lastUsedTime;

    @Column(name = "score_avg", precision = 5, scale = 2)
    private BigDecimal scoreAvg;

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
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
