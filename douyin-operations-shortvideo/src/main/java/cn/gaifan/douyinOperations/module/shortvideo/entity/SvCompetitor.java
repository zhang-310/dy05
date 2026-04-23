package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 竞品账号（Phase 4.3）
 */
@Entity
@Table(name = "sv_competitor")
@org.hibernate.annotations.SQLRestriction("deleted = 0")
public class SvCompetitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "competitor_name", nullable = false, length = 100)
    private String competitorName;

    @Column(name = "platform", length = 32)
    private String platform = "douyin";

    @Column(name = "account_id", length = 100)
    private String accountId;

    @Column(name = "account_url", length = 500)
    private String accountUrl;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "fan_count")
    private Long fanCount = 0L;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "deleted")
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
        if (updateTime == null) updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getCompetitorName() { return competitorName; }
    public void setCompetitorName(String competitorName) { this.competitorName = competitorName; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getAccountUrl() { return accountUrl; }
    public void setAccountUrl(String accountUrl) { this.accountUrl = accountUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getFanCount() { return fanCount; }
    public void setFanCount(Long fanCount) { this.fanCount = fanCount; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
