package cn.gaifan.douyinOperations.module.abtest.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * A/B 测试事件记录表
 * 与 sql/abtest/schema.sql 中 ab_event 一一对应
 * 注意：无 deleted 字段，事件不可删除
 */
@Getter
@Setter
@Entity
@Table(name = "ab_event")
@NoArgsConstructor
public class AbEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_id", nullable = false)
    private Long experimentId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    /** 事件类型：view / click / conversion */
    @Column(name = "event_type", nullable = false, length = 16)
    private String eventType;

    @Column(name = "user_fingerprint", nullable = false, length = 64)
    private String userFingerprint;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
