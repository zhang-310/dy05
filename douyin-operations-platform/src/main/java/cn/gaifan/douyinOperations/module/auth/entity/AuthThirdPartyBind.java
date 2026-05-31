package cn.gaifan.douyinOperations.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 第三方账号绑定表（微信、QQ、抖音、火山）
 * 与 sql/auth/schema.sql 中 auth_third_party_bind 一一对应 
 */
@Getter
@Setter
@Entity
@Table(name = "auth_third_party_bind")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AuthThirdPartyBind {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "open_id", nullable = false, length = 256)
    private String openId;

    @Column(name = "union_id", length = 256)
    private String unionId;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "avatar", length = 512)
    private String avatar;

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
