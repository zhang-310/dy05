package cn.gaifan.douyinOperations.module.config.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 系统配置表，与 sql/config/schema-pg.sql 一一对应
 */
@Data
@Entity
@Table(name = "sys_config")
@SQLRestriction("deleted = 0")
public class SysConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, length = 128)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "value_type", nullable = false, length = 16)
    private String valueType = "string";

    @Column(name = "is_sensitive", nullable = false)
    private Integer isSensitive = 0;

    @Column(name = "config_group", length = 64)
    private String configGroup;

    @Column(name = "remark", length = 256)
    private String remark;

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
