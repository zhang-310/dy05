package cn.gaifan.douyinOperations.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 角色-资源关联表（用于后台授权：角色拥有哪些菜单/API/按钮）
 * 与 sql/auth/schema.sql 中 auth_role_resource 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "auth_role_resource")
@NoArgsConstructor
public class AuthRoleResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
