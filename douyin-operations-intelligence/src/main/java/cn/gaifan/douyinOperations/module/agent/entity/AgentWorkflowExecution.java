package cn.gaifan.douyinOperations.module.agent.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 工作流执行记录
 */
@Data
@Entity
@Table(name = "agent_workflow_execution")
@SQLRestriction("deleted = 0")
public class AgentWorkflowExecution {

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_FAILED = 2;
    public static final int STATUS_CANCELLED = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 执行状态：0=running, 1=completed, 2=failed, 3=cancelled */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    /** 当前执行到的步骤顺序 */
    @Column(name = "current_step_order")
    private Integer currentStepOrder = 0;

    /** 总步骤数 */
    @Column(name = "total_steps")
    private Integer totalSteps = 0;

    /** 执行上下文的 JSON */
    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData;

    @Column(name = "start_time")
    private Timestamp startTime;

    @Column(name = "end_time")
    private Timestamp endTime;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
        if (startTime == null) startTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
