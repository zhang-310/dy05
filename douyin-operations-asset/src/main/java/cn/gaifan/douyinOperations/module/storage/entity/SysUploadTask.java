package cn.gaifan.douyinOperations.module.storage.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 分块上传任务表
 * 与 sql/storage/uploadable-schema.sql 中 sys_upload_task 一一对应
 */
@Data
@Entity
@Table(name = "sys_upload_task")
@SQLRestriction("deleted = 0")
public class SysUploadTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 上传会话 ID（UUID，唯一） */
    @Column(name = "upload_id", nullable = false, unique = true, length = 64)
    private String uploadId;

    /** BOS 多部分上传 ID */
    @Column(name = "bos_upload_id", length = 256)
    private String bosUploadId;

    /** 原始文件名 */
    @Column(name = "original_filename", nullable = false, length = 256)
    private String originalFilename;

    /** 存储路径 key */
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    /** 总文件大小（字节） */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** 全文件 MD5 */
    @Column(name = "file_md5", length = 64)
    private String fileMd5;

    /** 单块大小（默认 5 MB） */
    @Column(name = "chunk_size")
    private Integer chunkSize = 5242880;

    /** 总块数 */
    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    /** 已上传块数 */
    @Column(name = "uploaded_chunks")
    private Integer uploadedChunks = 0;

    /** 已上传字节数 */
    @Column(name = "uploaded_bytes")
    private Long uploadedBytes = 0L;

    /** 上传状态 */
    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING";

    /** 失败原因 */
    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    /** 所属模块 */
    @Column(name = "module", length = 64)
    private String module;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Column(name = "expire_at", nullable = false)
    private Timestamp expireAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Timestamp(System.currentTimeMillis());
        if (updatedAt == null) updatedAt = new Timestamp(System.currentTimeMillis());
        if (expireAt == null) {
            // 默认 7 天后过期
            expireAt = new Timestamp(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L);
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
