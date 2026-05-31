package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 视频生成任务表，与 sql/shortvideo/schema.sql 中 sv_video_generation 一一对应
 * 注意：无 deleted 字段，生成任务不支持逻辑删除
 */
@Getter
@Setter
@Entity
@Table(name = "sv_video_generation")
@NoArgsConstructor
public class SvVideoGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;  // script / cover / full_video

    @Column(name = "input_params", columnDefinition = "TEXT")
    private String inputParams;  // JSON

    @Column(name = "output_result", columnDefinition = "TEXT")
    private String outputResult;  // JSON

    @Column(name = "task_status", nullable = false)
    private Integer taskStatus = 0;  // 0=待执行 1=执行中 2=已完成 3=失败

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "model_used", length = 64)
    private String modelUsed;

    @Column(name = "tokens_used")
    private Long tokensUsed = 0L;

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
}
