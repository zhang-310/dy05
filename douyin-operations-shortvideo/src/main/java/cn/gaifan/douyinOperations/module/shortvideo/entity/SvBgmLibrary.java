package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BGM 素材库（Phase 4.2）
 */
@Entity
@Table(name = "sv_bgm_library")
@SQLRestriction("deleted = 0")
public class SvBgmLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bgm_name", nullable = false, length = 200)
    private String bgmName;

    @Column(name = "artist", length = 100)
    private String artist;

    @Column(name = "style", nullable = false, length = 64)
    private String style;

    @Column(name = "bpm")
    private Integer bpm;

    @Column(name = "mood", length = 64)
    private String mood;

    @Column(name = "energy_level")
    private Integer energyLevel;

    @Column(name = "emotion_curve_match", length = 200)
    private String emotionCurveMatch;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "avg_viral_score")
    private Double avgViralScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "best_content_types", columnDefinition = "jsonb")
    private List<String> bestContentTypes;

    @Column(name = "license_type", length = 32)
    private String licenseType;

    @Column(name = "source_platform", length = 32)
    private String sourcePlatform;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "deleted")
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBgmName() { return bgmName; }
    public void setBgmName(String bgmName) { this.bgmName = bgmName; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public Integer getBpm() { return bpm; }
    public void setBpm(Integer bpm) { this.bpm = bpm; }
    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }
    public Integer getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(Integer energyLevel) { this.energyLevel = energyLevel; }
    public String getEmotionCurveMatch() { return emotionCurveMatch; }
    public void setEmotionCurveMatch(String emotionCurveMatch) { this.emotionCurveMatch = emotionCurveMatch; }
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    public Double getAvgViralScore() { return avgViralScore; }
    public void setAvgViralScore(Double avgViralScore) { this.avgViralScore = avgViralScore; }
    public List<String> getBestContentTypes() { return bestContentTypes; }
    public void setBestContentTypes(List<String> bestContentTypes) { this.bestContentTypes = bestContentTypes; }
    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }
    public String getSourcePlatform() { return sourcePlatform; }
    public void setSourcePlatform(String sourcePlatform) { this.sourcePlatform = sourcePlatform; }
    public LocalDate getExpireDate() { return expireDate; }
    public void setExpireDate(LocalDate expireDate) { this.expireDate = expireDate; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
