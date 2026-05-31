package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "ai_generation_task")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AiGenerationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    @Column(name = "input_content", columnDefinition = "TEXT")
    private String inputContent;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "model_used", length = 64)
    private String modelUsed;

    @Column(name = "output_content", columnDefinition = "TEXT")
    private String outputContent;

    @Column(name = "tokens_used", nullable = false)
    private Long tokensUsed = 0L;

    /** 任务状态：0=待处理 1=处理中 2=完成 3=失败 */
    @Column(name = "task_status", nullable = false)
    private Integer taskStatus = 0;

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

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
