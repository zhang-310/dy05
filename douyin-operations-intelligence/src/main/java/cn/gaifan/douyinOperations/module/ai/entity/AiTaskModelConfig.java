package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 任务-模型映射表：每个 AI 任务绑定主/备模型
 */
@Data
@Entity
@Table(name = "ai_task_model_config")
@SQLRestriction("deleted = 0")
public class AiTaskModelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_code", nullable = false, length = 64)
    private String taskCode;

    @Column(name = "task_name", nullable = false, length = 128)
    private String taskName;

    @Column(name = "task_group", nullable = false, length = 32)
    private String taskGroup = "evolve";

    @Column(name = "primary_model_id")
    private Long primaryModelId;

    @Column(name = "fallback_model_id")
    private Long fallbackModelId;

    @Column(name = "fallback2_model_id")
    private Long fallback2ModelId;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 1;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

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
}
