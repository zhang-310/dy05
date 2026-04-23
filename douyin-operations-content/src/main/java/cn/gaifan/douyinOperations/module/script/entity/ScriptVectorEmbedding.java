package cn.gaifan.douyinOperations.module.script.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 向量嵌入记录表
 * 存储话术的 1024D 向量嵌入，支持 Milvus 向量搜索
 */
@Data
@Entity
@Table(name = "sc_script_vector_embedding")
@SQLRestriction("deleted = 0")
public class ScriptVectorEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    /** 向量嵌入 (BYTEA 格式，1024D FLOAT32 数组) */
    @Column(name = "vector_embedding", nullable = false, columnDefinition = "BYTEA")
    private byte[] vectorEmbedding;

    @Column(name = "vector_dimension", nullable = false)
    private Integer vectorDimension = 1024;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel = "BGE-M3";

    /** 向量生成耗时（毫秒） */
    @Column(name = "embedding_time_ms")
    private Integer embeddingTimeMs = 0;

    /** 是否已导入 Milvus */
    @Column(name = "is_indexed_milvus")
    private Boolean isIndexedMilvus = false;

    /** Milvus 集合 ID */
    @Column(name = "milvus_collection_id")
    private Long milvusCollectionId;

    /** 最后索引时间 */
    @Column(name = "last_indexed_at")
    private Timestamp lastIndexedAt;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Timestamp(System.currentTimeMillis());
        if (updatedAt == null) updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
