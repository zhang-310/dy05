package cn.gaifan.douyinOperations.module.auth.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 系统资源表（菜单、API、按钮三类，用于后台授权）
 * 与 sql/auth/schema.sql 中 auth_resource 一一对应
 */
@Data
@Entity
@Table(name = "auth_resource")
@SQLRestriction("deleted = 0")
public class AuthResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 类型：menu/api/button */
    @Column(name = "resource_type", nullable = false, length = 16)
    private String resourceType;

    @Column(name = "resource_code", nullable = false, length = 256)
    private String resourceCode;

    @Column(name = "request_method", length = 16)
    private String requestMethod;

    @Column(name = "module", length = 64)
    private String module;

    @Column(name = "resource_name", length = 128)
    private String resourceName;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

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
