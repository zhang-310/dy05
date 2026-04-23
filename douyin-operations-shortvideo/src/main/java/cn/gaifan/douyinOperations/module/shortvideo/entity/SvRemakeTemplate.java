package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 爆款二创模板（Phase 4.1）
 */
@Entity
@Table(name = "sv_remake_template")
@SQLRestriction("deleted = 0")
public class SvRemakeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId = 0L;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "remake_type", nullable = false, length = 32)
    private String remakeType;

    @Column(name = "source_viral_id")
    private Long sourceViralId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structure_template", nullable = false, columnDefinition = "jsonb")
    private Object structureTemplate;

    @Column(name = "emotion_curve", length = 200)
    private String emotionCurve;

    @Column(name = "bgm_style", length = 64)
    private String bgmStyle;

    @Column(name = "duration_range", length = 32)
    private String durationRange;

    @Column(name = "adaptation_guide", columnDefinition = "TEXT")
    private String adaptationGuide;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variable_slots", columnDefinition = "jsonb")
    private List<Object> variableSlots;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "avg_viral_score")
    private Double avgViralScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_types", columnDefinition = "jsonb")
    private List<String> contentTypes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags;

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
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getRemakeType() { return remakeType; }
    public void setRemakeType(String remakeType) { this.remakeType = remakeType; }
    public Long getSourceViralId() { return sourceViralId; }
    public void setSourceViralId(Long sourceViralId) { this.sourceViralId = sourceViralId; }
    public Object getStructureTemplate() { return structureTemplate; }
    public void setStructureTemplate(Object structureTemplate) { this.structureTemplate = structureTemplate; }
    public String getEmotionCurve() { return emotionCurve; }
    public void setEmotionCurve(String emotionCurve) { this.emotionCurve = emotionCurve; }
    public String getBgmStyle() { return bgmStyle; }
    public void setBgmStyle(String bgmStyle) { this.bgmStyle = bgmStyle; }
    public String getDurationRange() { return durationRange; }
    public void setDurationRange(String durationRange) { this.durationRange = durationRange; }
    public String getAdaptationGuide() { return adaptationGuide; }
    public void setAdaptationGuide(String adaptationGuide) { this.adaptationGuide = adaptationGuide; }
    public List<Object> getVariableSlots() { return variableSlots; }
    public void setVariableSlots(List<Object> variableSlots) { this.variableSlots = variableSlots; }
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    public Double getAvgViralScore() { return avgViralScore; }
    public void setAvgViralScore(Double avgViralScore) { this.avgViralScore = avgViralScore; }
    public List<String> getContentTypes() { return contentTypes; }
    public void setContentTypes(List<String> contentTypes) { this.contentTypes = contentTypes; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
