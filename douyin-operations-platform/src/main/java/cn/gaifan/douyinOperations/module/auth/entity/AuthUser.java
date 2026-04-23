package cn.gaifan.douyinOperations.module.auth.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 用户账号表（登录、个人信息中心；管理员可做基本信息操作、封禁）
 * 与 sql/auth/schema.sql 中 auth_user 一一对应
 */
@Data
@Entity
@Table(name = "auth_user")
@SQLRestriction("deleted = 0")
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "avatar_url", length = 256)
    private String avatarUrl;

    @Column(name = "role_code", nullable = false, length = 32)
    private String roleCode = "user";

    /** 状态 0正常 1封禁 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "banned_at")
    private Timestamp bannedAt;

    @Column(name = "banned_reason", length = 256)
    private String bannedReason;

    @Column(name = "last_login_at")
    private Timestamp lastLoginAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "organization_id")
    private Long organizationId;

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
