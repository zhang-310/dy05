package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 知识库表，与 sql/ai/schema.sql 中 ai_knowledge_base 一一对应
 */
@Data
@Entity
@Table(name = "ai_knowledge_base")
@SQLRestriction("deleted = 0")
public class AiKnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "kb_name", nullable = false, length = 128)
    private String kbName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_documents", nullable = false)
    private Integer totalDocuments = 0;

    @Column(name = "total_tokens", nullable = false)
    private Long totalTokens = 0L;

    @Column(name = "embedding_model", length = 64)
    private String embeddingModel;

    /** 知识库类型：general=通用 huashu=话术 zhishi=知识 douyin=抖音运营 */
    @Column(name = "kb_type", length = 32)
    private String kbType;

    /** 状态：0=构建中 1=就绪 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

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
