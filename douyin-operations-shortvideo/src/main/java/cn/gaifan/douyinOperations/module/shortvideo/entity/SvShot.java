package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 分镜详情表，与 sql/shortvideo/migration-bos-production.sql 中 sv_shot 对应
 * 所有媒体 URL 均为 BOS CDN URL
 */
@Data
@Entity
@Table(name = "sv_shot")
@SQLRestriction("deleted = 0")
public class SvShot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shot_list_id", nullable = false)
    private Long shotListId;

    @Column(name = "shot_number", nullable = false)
    private Integer shotNumber;

    @Column(name = "time_range", length = 50)
    private String timeRange;  // 0-3s

    @Column(name = "scene_description", columnDefinition = "TEXT")
    private String sceneDescription;

    @Column(name = "camera_angle", length = 100)
    private String cameraAngle;

    @Column(name = "camera_type", length = 50)
    private String cameraType;  // 运镜类型 (CameraType.code: zoom-in, dolly-in 等)

    @Column(name = "camera_params", columnDefinition = "TEXT")
    private String cameraParams;  // 运镜参数 (JSON)

    @Column(name = "quality_level", length = 20)
    private String qualityLevel;  // 质量级别 (QualityLevel.code)

    @Column(name = "ai_model", length = 50)
    private String aiModel;  // AI 视频生成模型名称

    @Column(name = "quality_score", precision = 5, scale = 2)
    private java.math.BigDecimal qualityScore;  // 质量评分 (0-100)

    @Column(name = "action", length = 255)
    private String action;

    @Column(name = "dialogue", columnDefinition = "TEXT")
    private String dialogue;

    @Column(name = "mood", length = 100)
    private String mood;

    @Column(name = "keyframe_url", length = 500)
    private String keyframeUrl;  // 首帧 BOS CDN URL

    @Column(name = "keyframe_bos_key", length = 500)
    private String keyframeBosKey;

    @Column(name = "end_frame_url", length = 500)
    private String endFrameUrl;  // 尾帧 BOS CDN URL（同场景动作延续，如炒菜从拿起锅铲到翻炒中）

    @Column(name = "end_frame_bos_key", length = 500)
    private String endFrameBosKey;

    @Column(name = "video_url", length = 500)
    private String videoUrl;  // BOS CDN URL

    @Column(name = "video_bos_key", length = 500)
    private String videoBosKey;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;  // BOS CDN URL

    @Column(name = "audio_bos_key", length = 500)
    private String audioBosKey;

    @Column(name = "dialogue_text", columnDefinition = "TEXT")
    private String dialogueText;  // TTS 输入文本

    @Column(name = "sfx_hints", length = 500)
    private String sfxHints;  // 音效提示

    @Column(name = "tts_url", length = 500)
    private String ttsUrl;  // TTS 合成音频 URL

    @Column(name = "bgm_url", length = 500)
    private String bgmUrl;  // BGM 背景音乐 URL

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "review_status", length = 16)
    private String reviewStatus = "pending";  // pending/approved/needs_revision（daily 类型分镜审核）

    @Column(name = "reviewer_note", columnDefinition = "TEXT")
    private String reviewerNote;

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
