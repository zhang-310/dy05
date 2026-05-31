package cn.gaifan.douyinOperations.module.douyin.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 粉丝画像统计表
 */
@Getter
@Setter
@Entity
@Table(name = "dy_fan_profile_stats")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class DyFanProfileStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "stat_type", nullable = false, length = 32)
    private String statType;

    @Column(name = "stat_key", nullable = false, length = 64)
    private String statKey;

    @Column(name = "stat_value", length = 128)
    private String statValue;

    @Column(name = "count")
    private Long count = 0L;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "sync_time")
    private Timestamp syncTime;

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
