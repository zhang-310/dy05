package cn.gaifan.douyinOperations.module.douyinapi.entity;

import cn.gaifan.douyinOperations.module.douyinapi.converter.TokenEncryptionConverter;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * OAuth Token 存储表
 * 存储用户的第三方平台授权 token
 */
@Data
@Entity
@Table(name = "oauth_token")
@SQLRestriction("deleted = 0")
public class OAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** OAuth 提供商: douyin / wechat / qq */
    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "open_id", nullable = false, length = 128)
    private String openId;

    @Column(name = "access_token", nullable = false, length = 1024)
    @Convert(converter = TokenEncryptionConverter.class)
    private String accessToken;

    @Column(name = "refresh_token", length = 1024)
    @Convert(converter = TokenEncryptionConverter.class)
    private String refreshToken;

    /** Token 过期时间 */
    @Column(name = "expires_at", nullable = false)
    private Timestamp expiresAt;

    /** 授权范围 */
    @Column(name = "scope", length = 256)
    private String scope;

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

    /**
     * 检查 token 是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 检查 token 是否即将过期（30分钟内）
     */
    public boolean isExpiringSoon() {
        if (expiresAt == null) return false;
        long thirtyMinutes = 30 * 60 * 1000L;
        return expiresAt.getTime() - System.currentTimeMillis() < thirtyMinutes;
    }
}
