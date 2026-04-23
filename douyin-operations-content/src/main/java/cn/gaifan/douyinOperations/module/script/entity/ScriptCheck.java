package cn.gaifan.douyinOperations.module.script.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 话术检测记录表
 * 与 sql/script/schema.sql 中 script_check 一一对应
 */
@Data
@Entity
@Table(name = "script_check")
@SQLRestriction("deleted = 0")
public class ScriptCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "check_time", nullable = false)
    private Timestamp checkTime;

    @Column(name = "violation_count", nullable = false)
    private Integer violationCount = 0;

    @Column(name = "violations", columnDefinition = "TEXT")
    private String violations;

    /** 检测状态：0=不通过 1=通过 */
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
        if (checkTime == null) checkTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
