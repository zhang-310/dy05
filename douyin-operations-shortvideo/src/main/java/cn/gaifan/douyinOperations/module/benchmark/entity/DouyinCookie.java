package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 抖音Cookie管理实体
 */
@Getter
@Setter
@Entity
@Table(name = "douyin_cookie")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class DouyinCookie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "cookie_name", nullable = false, length = 128)
    private String cookieName;

    @Column(name = "cookie_value", nullable = false, columnDefinition = "TEXT")
    private String cookieValue;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform = "douyin";

    @Column(name = "account_name", length = 128)
    private String accountName;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "is_valid")
    private Boolean isValid = true;

    @Column(name = "last_check_time")
    private LocalDateTime lastCheckTime;

    @Column(name = "check_status", length = 32)
    private String checkStatus = "unknown";

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "last_used_time")
    private LocalDateTime lastUsedTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
