package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 话术使用日志（效果归因反哺评分）
 */
@Getter
@Setter
@Entity
@Table(name = "script_usage_log")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class ScriptUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "script_source", nullable = false, length = 16)
    private String scriptSource;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "script_type", length = 32)
    private String scriptType;

    @Column(name = "used_at")
    private Timestamp usedAt;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ab_experiment_id")
    private Long abExperimentId;

    @Column(name = "ab_variant_id")
    private Long abVariantId;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (usedAt == null) usedAt = new Timestamp(System.currentTimeMillis());
    }
}
