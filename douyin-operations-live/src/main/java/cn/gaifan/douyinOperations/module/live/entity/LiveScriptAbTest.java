package cn.gaifan.douyinOperations.module.live.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 话术版本级 A/B 测试（Phase 2.5）
 */
@Entity
@Table(name = "live_script_ab_test")
@SQLRestriction("deleted = 0")
public class LiveScriptAbTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "version_a_id", nullable = false)
    private Long versionAId;

    @Column(name = "version_b_id", nullable = false)
    private Long versionBId;

    @Column(name = "status", length = 16)
    private String status = "running";

    @Column(name = "traffic_split")
    private Integer trafficSplit = 50;

    @Column(name = "a_impressions")
    private Integer aImpressions = 0;

    @Column(name = "b_impressions")
    private Integer bImpressions = 0;

    @Column(name = "a_conversion_rate")
    private Double aConversionRate;

    @Column(name = "b_conversion_rate")
    private Double bConversionRate;

    @Column(name = "a_retention_rate")
    private Double aRetentionRate;

    @Column(name = "b_retention_rate")
    private Double bRetentionRate;

    @Column(name = "a_interaction_rate")
    private Double aInteractionRate;

    @Column(name = "b_interaction_rate")
    private Double bInteractionRate;

    @Column(name = "p_value")
    private Double pValue;

    @Column(name = "confidence_level")
    private Double confidenceLevel;

    @Column(name = "winner", length = 1)
    private String winner;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

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
        if (startTime == null) startTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getScriptId() { return scriptId; }
    public void setScriptId(Long scriptId) { this.scriptId = scriptId; }
    public Long getVersionAId() { return versionAId; }
    public void setVersionAId(Long versionAId) { this.versionAId = versionAId; }
    public Long getVersionBId() { return versionBId; }
    public void setVersionBId(Long versionBId) { this.versionBId = versionBId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTrafficSplit() { return trafficSplit; }
    public void setTrafficSplit(Integer trafficSplit) { this.trafficSplit = trafficSplit; }
    public Integer getaImpressions() { return aImpressions; }
    public void setaImpressions(Integer aImpressions) { this.aImpressions = aImpressions; }
    public Integer getbImpressions() { return bImpressions; }
    public void setbImpressions(Integer bImpressions) { this.bImpressions = bImpressions; }
    public Double getaConversionRate() { return aConversionRate; }
    public void setaConversionRate(Double aConversionRate) { this.aConversionRate = aConversionRate; }
    public Double getbConversionRate() { return bConversionRate; }
    public void setbConversionRate(Double bConversionRate) { this.bConversionRate = bConversionRate; }
    public Double getaRetentionRate() { return aRetentionRate; }
    public void setaRetentionRate(Double aRetentionRate) { this.aRetentionRate = aRetentionRate; }
    public Double getbRetentionRate() { return bRetentionRate; }
    public void setbRetentionRate(Double bRetentionRate) { this.bRetentionRate = bRetentionRate; }
    public Double getaInteractionRate() { return aInteractionRate; }
    public void setaInteractionRate(Double aInteractionRate) { this.aInteractionRate = aInteractionRate; }
    public Double getbInteractionRate() { return bInteractionRate; }
    public void setbInteractionRate(Double bInteractionRate) { this.bInteractionRate = bInteractionRate; }
    public Double getpValue() { return pValue; }
    public void setpValue(Double pValue) { this.pValue = pValue; }
    public Double getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(Double confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
