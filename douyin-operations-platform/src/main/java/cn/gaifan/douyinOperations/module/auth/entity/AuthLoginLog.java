package cn.gaifan.douyinOperations.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 登录记录表（设备/方式：Web、iOS、安卓）
 * 与 sql/auth/schema.sql 中 auth_login_log 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "auth_login_log")
@NoArgsConstructor
public class AuthLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "fail_reason", length = 256)
    private String failReason;

    @Column(name = "login_type", nullable = false, length = 32)
    private String loginType;

    @Column(name = "device_type", nullable = false, length = 16)
    private String deviceType;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @Column(name = "login_time", nullable = false)
    private Timestamp loginTime;

    @PrePersist
    public void prePersist() {
        if (loginTime == null) loginTime = new Timestamp(System.currentTimeMillis());
    }
}
