package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 知识索引队列表，与 sql/ai/evolution-schema.sql 中 ai_index_queue 一一对应
 * 注意：无 deleted 字段，处理完成后物理删除或归档
 */
@Getter
@Setter
@Entity
@Table(name = "ai_index_queue")
@NoArgsConstructor
public class AiIndexQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;  // viral_video / live_review / manual

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "target_kb_id")
    private Long targetKbId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "priority", nullable = false)
    private Integer priority = 5;  // 1=高 5=普通 10=低

    @Column(name = "status", nullable = false, length = 16)
    private String status = "pending";  // pending / processing / done / failed

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_msg", length = 512)
    private String errorMsg;

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
