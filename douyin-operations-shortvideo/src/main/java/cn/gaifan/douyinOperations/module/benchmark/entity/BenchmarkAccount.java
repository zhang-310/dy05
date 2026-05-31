package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 对标账号实体
 */
@Getter
@Setter
@Entity
@Table(name = "benchmark_account")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class BenchmarkAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "account_name", nullable = false, length = 128)
    private String accountName;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform = "douyin";

    @Column(name = "account_url", length = 512)
    private String accountUrl;

    @Column(name = "sec_uid", length = 128)
    private String secUid;

    @Column(name = "douyin_id", length = 128)
    private String douyinId;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "fan_count")
    private Long fanCount = 0L;

    @Column(name = "video_count")
    private Integer videoCount = 0;

    @Column(name = "avg_view_count")
    private Long avgViewCount = 0L;

    @Column(name = "avg_like_count")
    private Integer avgLikeCount = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_collect_time")
    private LocalDateTime lastCollectTime;

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
