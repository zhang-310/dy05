package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 待深化问题表，供主题扩展使用
 */
@Data
@Entity
@Table(name = "ai_evolve_pending_deepen")
public class AiEvolvePendingDeepen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /** 所属知识库（用于跨库过滤） */
    @Column(name = "kb_id")
    private Long kbId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "priority_level", nullable = false)
    private Integer priorityLevel = 1;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "pending";

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
