package cn.gaifan.douyinOperations.module.storage.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 文件上传记录表
 * 与 sql/storage/schema.sql 中 sys_file 一一对应
 */
@Data
@Entity
@Table(name = "sys_file")
@SQLRestriction("deleted = 0")
public class SysFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "original_name", nullable = false, length = 256)
    private String originalName;

    @Column(name = "storage_name", nullable = false, length = 256)
    private String storageName;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "file_url", nullable = false, length = 512)
    private String fileUrl;

    /** 文件类型：image / video / audio */
    @Column(name = "file_type", length = 32)
    private String fileType;

    /** 扩展名：jpg / png / mp4 等 */
    @Column(name = "file_ext", length = 16)
    private String fileExt;

    @Column(name = "file_size")
    private Long fileSize = 0L;

    /** 所属模块：auth / douyin / live / shortvideo / ai */
    @Column(name = "module", length = 64)
    private String module;

    /** 存储提供商：local / bos / oss */
    @Column(name = "provider", length = 32)
    private String provider = "local";

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
