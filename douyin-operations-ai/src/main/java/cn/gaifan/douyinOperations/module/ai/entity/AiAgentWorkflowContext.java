package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 多 Agent 工作流执行上下文 — 支持断点续跑
 * <p>
 * 每个 DAG 节点执行后持久化 input/output，重启后可跳过已完成节点。
 */
@Getter
@Setter
@Entity
@Table(name = "ai_agent_workflow_context")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AiAgentWorkflowContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "workflow_id", length = 64, nullable = false)
    private String workflowId;

    @Column(name = "node_role", length = 50, nullable = false)
    private String nodeRole;

    @Column(name = "node_status", length = 20, nullable = false)
    private String nodeStatus;

    @Column(name = "input_json", columnDefinition = "TEXT")
    private String inputJson;

    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "started_at")
    private Timestamp startedAt;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
