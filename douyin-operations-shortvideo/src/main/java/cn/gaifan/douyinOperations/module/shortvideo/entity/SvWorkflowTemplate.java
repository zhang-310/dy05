package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * 短视频工作流预设模板（Phase 4.4 / P2-7）
 * steps 为 JSON 数组，定义步骤依赖与类型（auto/manual）
 */
@Entity
@Table(name = "sv_workflow_template")
@SQLRestriction("deleted = 0")
public class SvWorkflowTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId = 0L;

    @Column(name = "template_name", nullable = false, length = 128)
    private String templateName;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "steps", columnDefinition = "TEXT")
    private String steps;

    @Column(name = "is_system", nullable = false)
    private Integer isSystem = 0;

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

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSteps() { return steps; }
    public void setSteps(String steps) { this.steps = steps; }
    public Integer getIsSystem() { return isSystem; }
    public void setIsSystem(Integer isSystem) { this.isSystem = isSystem; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Timestamp getCreateTime() { return createTime; }
    public void setCreateTime(Timestamp createTime) { this.createTime = createTime; }
    public Timestamp getUpdateTime() { return updateTime; }
    public void setUpdateTime(Timestamp updateTime) { this.updateTime = updateTime; }
}
