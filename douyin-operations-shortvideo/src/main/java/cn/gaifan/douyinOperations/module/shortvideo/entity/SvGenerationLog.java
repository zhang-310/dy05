package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 图生视频生成历史 (Phase 5/6 知识库闭环)
 * 表: sv_generation_log
 */
@Getter
@Setter
@Entity
@Table(name = "sv_generation_log")
@NoArgsConstructor
public class SvGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "shot_id")
    private Long shotId;

    @Column(name = "camera_type", length = 50)
    private String cameraType;

    @Column(name = "quality_level", length = 20)
    private String qualityLevel;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "ai_provider", length = 50)
    private String aiProvider;

    @Column(name = "success", nullable = false)
    private Boolean success = false;

    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "generation_time_ms")
    private Long generationTimeMs;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "content_type", length = 50)
    private String contentType;  // image2video/audio2video/text2video

    @Column(name = "route_reason", length = 200)
    private String routeReason;  // 智能路由选择原因

    @Column(name = "cost_cents")
    private Integer costCents;  // 成本（分）

    @Column(name = "has_audio")
    private Boolean hasAudio = false;  // 是否含音频（音视频联合）

    @Column(name = "create_time")
    private Timestamp createTime;
}
