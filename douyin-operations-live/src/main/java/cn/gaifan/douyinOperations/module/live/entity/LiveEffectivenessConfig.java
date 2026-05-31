package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播效果评分公式权重配置
 * Q3-5: Configurable Effectiveness Score Formula
 * 对应表 live_effectiveness_config
 */
@Getter
@Setter
@Entity
@Table(name = "live_effectiveness_config")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveEffectivenessConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 ID（数据隔离，兼容旧字段 user_id） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 数据所有者 ID（owner_id 列，可与 user_id 不同以支持多租户委派场景） */
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "config_name", nullable = false, length = 128)
    private String configName = "默认配置";

    /** 同义别名：name 列（SQL DDL 中列名为 name，JPA 同时映射 config_name 和 name） */
    @Column(name = "name", insertable = false, updatable = false)
    private String name;

    @Column(name = "conversion_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal conversionWeight = new BigDecimal("0.30");

    @Column(name = "interaction_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal interactionWeight = new BigDecimal("0.25");

    @Column(name = "retention_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal retentionWeight = new BigDecimal("0.25");

    @Column(name = "gmv_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal gmvWeight = new BigDecimal("0.20");

    @Column(name = "viewer_weight", precision = 5, scale = 2)
    private BigDecimal viewerWeight = new BigDecimal("0.30");

    @Column(name = "is_default")
    private Integer isDefault = 0;

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
