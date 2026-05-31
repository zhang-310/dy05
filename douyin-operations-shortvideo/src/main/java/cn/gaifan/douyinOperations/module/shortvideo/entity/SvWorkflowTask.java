package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * 工作流任务状态 (P0 持久化)
 * 表: sv_workflow_task
 * 用于 Redis 不可用或应用重启后恢复任务状态
 */
@Getter
@Setter
@Entity
@Table(name = "sv_workflow_task")
@NoArgsConstructor
public class SvWorkflowTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, unique = true, length = 128)
    private String taskId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "processing";

    @Column(name = "current_step", length = 64)
    private String currentStep = "script";

    @Column(name = "progress")
    private Integer progress = 0;

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
