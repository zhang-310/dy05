package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 知识源管理表，与 sql/ai/knowledge-source-schema.sql 一一对应
 * 用于知识源目录、扫描、索引状态管理
 */
@Data
@Entity
@Table(name = "ai_knowledge_source")
@SQLRestriction("deleted = 0")
public class AiKnowledgeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name", nullable = false, length = 128)
    private String sourceName;

    @Column(name = "source_path", nullable = false, length = 512)
    private String sourcePath;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType = "local";

    @Column(name = "file_count", nullable = false)
    private Integer fileCount = 0;

    @Column(name = "index_count", nullable = false)
    private Integer indexCount = 0;

    @Column(name = "last_index_time")
    private Timestamp lastIndexTime;

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
