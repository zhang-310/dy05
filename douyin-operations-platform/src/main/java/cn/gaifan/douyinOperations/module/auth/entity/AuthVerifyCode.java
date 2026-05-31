package cn.gaifan.douyinOperations.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 验证码记录表（短信/邮箱验证码）
 * 与 sql/auth/schema.sql 中 auth_verify_code 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "auth_verify_code")
@NoArgsConstructor
public class AuthVerifyCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target", nullable = false, length = 128)
    private String target;

    @Column(name = "code", nullable = false, length = 16)
    private String code;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "expire_at", nullable = false)
    private Timestamp expireAt;

    @Column(name = "used", nullable = false)
    private Integer used = 0;

    @Column(name = "try_count", nullable = false)
    private Integer tryCount = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
