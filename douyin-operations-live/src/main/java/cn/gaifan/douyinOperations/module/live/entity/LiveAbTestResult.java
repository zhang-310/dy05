package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * A/B 测试结果记录
 * 与 sql/live/ab-analysis-schema.sql 中 live_ab_test_result 一一对应
 */
@Data
@Entity
@Table(name = "live_ab_test_result")
@SQLRestriction("deleted = 0")
public class LiveAbTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "experiment_key", length = 100, nullable = false)
    private String experimentKey;

    @Column(name = "variant", length = 50)
    private String variant;

    @Column(name = "style", length = 50)
    private String style;

    @Column(name = "effectiveness_score", precision = 5, scale = 2)
    private BigDecimal effectivenessScore;

    @Column(name = "conversion_rate", precision = 5, scale = 4)
    private BigDecimal conversionRate;

    @Column(name = "interaction_rate", precision = 5, scale = 4)
    private BigDecimal interactionRate;

    @Column(name = "sample_size")
    private Integer sampleSize = 0;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

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
