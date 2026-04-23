package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 素材库表，与 sql/shortvideo/migration-bos-production.sql 中 sv_material 对应
 * 所有媒体 URL 均为 BOS CDN URL
 */
@Data
@Entity
@Table(name = "sv_material")
@SQLRestriction("deleted = 0")
public class SvMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "material_type", nullable = false, length = 50)
    private String materialType;  // image/video/audio

    @Column(name = "url", nullable = false, length = 500)
    private String url;  // BOS CDN URL

    @Column(name = "bos_key", length = 500)
    private String bosKey;  // BOS 对象 Key（用于删除）

    @Column(name = "shot_id")
    private Long shotId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "format_type", length = 50)
    private String formatType;  // mp4/png/mp3

    @Column(name = "generation_type", length = 50)
    private String generationType;  // ai/upload

    @Column(name = "ai_prompt", columnDefinition = "TEXT")
    private String aiPrompt;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "post_processing_config", columnDefinition = "TEXT")
    private String postProcessingConfig;  // 后期处理配置 (JSON: 调色/稳定/降噪)

    @Column(name = "ai_provider", length = 50)
    private String aiProvider;  // AI 视频生成提供者 (kling/minimax/runway 等)

    /** 感知哈希（用于近似重复检测） */
    @Column(name = "perceptual_hash", length = 64)
    private String perceptualHash;

    /** 缩略图 URL */
    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    /** 内容 SHA-256 哈希（用于精确去重） */
    @Column(name = "content_sha256", length = 64)
    private String contentSha256;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
