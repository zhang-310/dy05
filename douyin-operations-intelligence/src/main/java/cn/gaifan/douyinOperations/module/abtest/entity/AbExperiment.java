package cn.gaifan.douyinOperations.module.abtest.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * A/B 测试实验表
 * 与 sql/abtest/schema.sql 中 ab_experiment 一一对应
 */
@Data
@Entity
@Table(name = "ab_experiment")
@SQLRestriction("deleted = 0")
public class AbExperiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 实验类型：video / live / copy */
    @Column(name = "experiment_type", nullable = false, length = 16)
    private String experimentType;

    /** 状态：0=草稿 1=运行中 2=已完成 3=已暂停 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "start_time")
    private Timestamp startTime;

    @Column(name = "end_time")
    private Timestamp endTime;

    @Column(name = "winner_variant_id")
    private Long winnerVariantId;

    /** 目标实体类型：product=产品话术 live_session=直播场次（script_style 实验时使用） */
    @Column(name = "target_entity_type", length = 32)
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private Long targetEntityId;

    @Column(name = "conclusion", columnDefinition = "TEXT")
    private String conclusion;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
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
