package cn.gaifan.douyinOperations.module.douyin.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 粉丝画像表
 */
@Data
@Entity
@Table(name = "dy_fan_profile")
@SQLRestriction("deleted = 0")
public class DyFanProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "age_range", length = 32)
    private String ageRange;

    @Column(name = "gender", length = 16)
    private String gender;

    @Column(name = "province", length = 64)
    private String province;

    @Column(name = "city", length = 64)
    private String city;

    @Column(name = "interest_tags", columnDefinition = "TEXT")
    private String interestTags;

    @Column(name = "active_time", length = 32)
    private String activeTime;

    @Column(name = "device_type", length = 32)
    private String deviceType;

    @Column(name = "fan_count")
    private Long fanCount = 0L;

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
