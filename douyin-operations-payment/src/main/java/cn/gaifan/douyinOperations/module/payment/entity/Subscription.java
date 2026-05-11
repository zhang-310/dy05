package cn.gaifan.douyinOperations.module.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "payment_subscription")
@SQLRestriction("deleted = 0")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "org_id")
    private Long orgId;

    @Column(nullable = false, length = 20)
    private String plan = "free";

    @Column(nullable = false, length = 20)
    private String status = "active";

    @Column(name = "started_at")
    private Timestamp startedAt;

    @Column(name = "expires_at")
    private Timestamp expiresAt;

    @Column(name = "auto_renew")
    private Boolean autoRenew = false;

    @Column(name = "max_live_sessions")
    private Integer maxLiveSessions = 5;

    @Column(name = "max_sv_projects")
    private Integer maxSvProjects = 10;

    @Column(name = "max_ai_generations")
    private Integer maxAiGenerations = 50;

    @Column(name = "max_storage_mb")
    private Integer maxStorageMb = 500;

    @Column
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createTime = now;
        this.updateTime = now;
        if (this.startedAt == null) this.startedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()));
    }
}
