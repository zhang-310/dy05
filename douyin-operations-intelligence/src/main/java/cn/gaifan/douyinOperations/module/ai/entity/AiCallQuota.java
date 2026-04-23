package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * AI 调用额度表（按用户/日）
 * 表：ai_call_quota
 */
@Data
@Entity
@Table(name = "ai_call_quota", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quota_date"}))
public class AiCallQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quota_date", nullable = false)
    private Date quotaDate;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "used_units", precision = 10, scale = 2)
    private BigDecimal usedUnits = BigDecimal.ZERO;

    @Column(name = "max_count", nullable = false)
    private Integer maxCount = 10;

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
