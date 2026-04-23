package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 图生视频异步任务 (Phase 2.2)
 * 表: sv_video_generation_task
 */
@Data
@Entity
@Table(name = "sv_video_generation_task")
public class SvVideoGenerationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "shot_list_id")
    private Long shotListId;

    @Column(name = "request_json", nullable = false, columnDefinition = "TEXT")
    private String requestJson;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "progress_current")
    private Integer progressCurrent = 0;

    @Column(name = "progress_total")
    private Integer progressTotal = 0;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 处理开始时间（超时检测用） */
    @Column(name = "processing_started_at")
    private Timestamp processingStartedAt;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = createTime;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
