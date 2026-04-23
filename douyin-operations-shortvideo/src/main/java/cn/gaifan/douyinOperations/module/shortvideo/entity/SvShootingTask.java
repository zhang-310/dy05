package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 短视频拍摄任务工单，与 sql/shortvideo/schema.sql 中 sv_shooting_task 对应。
 */
@Data
@Entity
@Table(name = "sv_shooting_task")
@SQLRestriction("deleted = 0")
public class SvShootingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "anchor_user_id")
    private Long anchorUserId;

    @Column(name = "persona_id")
    private Long personaId;

    @Column(name = "photographer_id")
    private Long photographerId;

    @Column(name = "script_id")
    private Long scriptId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "script_content", columnDefinition = "TEXT")
    private String scriptContent;

    @Column(name = "shooting_brief", columnDefinition = "TEXT")
    private String shootingBrief;

    @Column(name = "shoot_date", nullable = false)
    private Date shootDate;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    /** 0待分配 1已分配 2拍摄中 3已上传 4已审核 5已发布 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "material_urls", columnDefinition = "TEXT")
    private String materialUrls;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reference_video_url", length = 512)
    private String referenceVideoUrl;

    @Column(name = "reference_video_task_id")
    private Long referenceVideoTaskId;

    @Column(name = "reference_generated_at")
    private Timestamp referenceGeneratedAt;

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
        if (priority == null) priority = 0;
        if (status == null) status = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
