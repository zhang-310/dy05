package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户认知画像（Phase 3.3）
 */
@Entity
@Table(name = "ai_user_cognitive_profile")
@SQLRestriction("deleted = 0")
public class AiUserCognitiveProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, unique = true)
    private Long ownerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_script_types", columnDefinition = "jsonb")
    private Map<String, Double> preferredScriptTypes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_styles", columnDefinition = "jsonb")
    private Map<String, Double> preferredStyles;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_emotion_curves", columnDefinition = "jsonb")
    private Map<String, Double> preferredEmotionCurves;

    @Column(name = "avg_edit_ratio")
    private Double avgEditRatio = 0.5;

    @Column(name = "generation_frequency")
    private Integer generationFrequency = 0;

    @Column(name = "preferred_length", length = 16)
    private String preferredLength = "medium";

    @Column(name = "optimization_focus", length = 32)
    private String optimizationFocus = "balanced";

    @Column(name = "risk_tolerance")
    private Double riskTolerance = 0.5;

    @Column(name = "profile_version")
    private Integer profileVersion = 1;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

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
        if (lastUpdated == null) lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Map<String, Double> getPreferredScriptTypes() { return preferredScriptTypes; }
    public void setPreferredScriptTypes(Map<String, Double> preferredScriptTypes) { this.preferredScriptTypes = preferredScriptTypes; }
    public Map<String, Double> getPreferredStyles() { return preferredStyles; }
    public void setPreferredStyles(Map<String, Double> preferredStyles) { this.preferredStyles = preferredStyles; }
    public Map<String, Double> getPreferredEmotionCurves() { return preferredEmotionCurves; }
    public void setPreferredEmotionCurves(Map<String, Double> preferredEmotionCurves) { this.preferredEmotionCurves = preferredEmotionCurves; }
    public Double getAvgEditRatio() { return avgEditRatio; }
    public void setAvgEditRatio(Double avgEditRatio) { this.avgEditRatio = avgEditRatio; }
    public Integer getGenerationFrequency() { return generationFrequency; }
    public void setGenerationFrequency(Integer generationFrequency) { this.generationFrequency = generationFrequency; }
    public String getPreferredLength() { return preferredLength; }
    public void setPreferredLength(String preferredLength) { this.preferredLength = preferredLength; }
    public String getOptimizationFocus() { return optimizationFocus; }
    public void setOptimizationFocus(String optimizationFocus) { this.optimizationFocus = optimizationFocus; }
    public Double getRiskTolerance() { return riskTolerance; }
    public void setRiskTolerance(Double riskTolerance) { this.riskTolerance = riskTolerance; }
    public Integer getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Integer profileVersion) { this.profileVersion = profileVersion; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
