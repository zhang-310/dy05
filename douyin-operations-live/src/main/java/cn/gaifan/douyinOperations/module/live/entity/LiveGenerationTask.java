package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 话术生成任务实体
 * 与 sql/live/generation-task-schema.sql 中 live_generation_task 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "live_generation_task")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveGenerationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联直播场次 ID */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 任务状态：running / completed / failed / cancelled */
    @Column(name = "status", length = 20)
    private String status = "running";

    /** 总插槽数 */
    @Column(name = "total_slots")
    private Integer totalSlots = 0;

    /** 已完成插槽数 */
    @Column(name = "completed_slots")
    private Integer completedSlots = 0;

    /** 失败插槽数 */
    @Column(name = "failed_slots")
    private Integer failedSlots = 0;

    /** 话术风格 */
    @Column(name = "style", length = 50)
    private String style;

    /** 使用的 AI 模型 ID */
    @Column(name = "model_id")
    private Long modelId;

    /** 是否引用知识库 */
    @Column(name = "use_kb_ref")
    private Boolean useKbRef = false;

    /** 热门关键词（JSON 数组） */
    @Column(name = "hot_keywords", columnDefinition = "TEXT")
    private String hotKeywords;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 异步一键生成：LiveAiGenerateVO JSON（G-2） */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    /** inline | sse | rabbit */
    @Column(name = "execution_mode", length = 16)
    private String executionMode;

    /** 所属用户 ID（数据隔离） */
    @Column(name = "owner_id")
    private Long ownerId;

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
