package cn.gaifan.douyinOperations.module.storage.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 分块记录表
 * 与 sql/storage/uploadable-schema.sql 中 sys_upload_chunk 一一对应
 */
@Data
@Entity
@Table(name = "sys_upload_chunk")
public class SysUploadChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /** 分块序号（0-indexed） */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /** 分块大小（字节） */
    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;

    /** 分块 MD5 校验和 */
    @Column(name = "chunk_md5", nullable = false, length = 64)
    private String chunkMd5;

    /** BOS UploadPart 返回的 ETag */
    @Column(name = "bos_etag", length = 64)
    private String bosEtag;

    /** BOS UploadPart 的 part number */
    @Column(name = "bos_part_number")
    private Integer bosPartNumber;

    /** 分块状态 */
    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING";

    /** 重试次数 */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /** 上传完成时间 */
    @Column(name = "uploaded_at")
    private Timestamp uploadedAt;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Timestamp(System.currentTimeMillis());
    }
}
