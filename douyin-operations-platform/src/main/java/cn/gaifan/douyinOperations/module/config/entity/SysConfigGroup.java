package cn.gaifan.douyinOperations.module.config.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 配置分组表，与 sql/config/schema.sql 中 sys_config_group 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "sys_config_group")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SysConfigGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(name = "group_code", nullable = false, length = 64)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 64)
    private String groupName;

    @Column(name = "icon", length = 64)
    private String icon;

    @Column(name = "description", length = 256)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_system", nullable = false)
    private Integer isSystem = 0;

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
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
