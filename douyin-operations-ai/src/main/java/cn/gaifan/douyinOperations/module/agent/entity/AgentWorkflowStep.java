package cn.gaifan.douyinOperations.module.agent.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 智能体协作工作流步骤
 */
@Getter
@Setter
@Entity
@Table(name = "agent_workflow_step")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AgentWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    /** 执行顺序（1-based） */
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    /** 关联的智能体ID */
    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    /** 步骤名称 */
    @Column(name = "step_name", length = 128)
    private String stepName;

    /** 输入模板（支持 ${input}/${prev.output} 占位符） */
    @Column(name = "input_template", length = 2000)
    private String inputTemplate;

    /** 输出结果的 key（用于后续步骤引用） */
    @Column(name = "output_key", length = 128)
    private String outputKey;

    /** 跳过条件（简单关键字匹配） */
    @Column(name = "skip_condition", length = 512)
    private String skipCondition;

    /** DAG 依赖：JSON 数组，存储被依赖的 outputKey（如 ["step1","step2"]） */
    @Column(name = "depends_on", columnDefinition = "TEXT")
    private String dependsOn;

    /** 执行模式：0=顺序（默认），1=强制并行（仅在同一 DAG 层内有效） */
    @Column(name = "execution_mode")
    private Integer executionMode = 0;

    /** 重试次数：默认1（不重试），>1 时失败后重试 */
    @Column(name = "retry_count")
    private Integer retryCount = 1;

    /** 超时时间（秒） */
    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds = 120;

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
        if (timeoutSeconds == null) timeoutSeconds = 120;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
