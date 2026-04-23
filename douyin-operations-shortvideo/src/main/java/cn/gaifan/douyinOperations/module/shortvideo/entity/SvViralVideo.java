package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 爆款库表，与 sql/shortvideo/schema.sql 中 sv_viral_video 一一对应
 */
@Data
@Entity
@Table(name = "sv_viral_video")
@SQLRestriction("deleted = 0")
public class SvViralVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "source_video_id")
    private Long sourceVideoId;

    /** 关联账号主表 ID */
    @Column(name = "sv_account_id")
    private Long svAccountId;

    @Column(name = "douyin_video_id", length = 128)
    private String douyinVideoId;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;

    @Column(name = "video_url", length = 512)
    private String videoUrl;

    @Column(name = "author_name", length = 128)
    private String authorName;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "like_count")
    private Long likeCount = 0L;

    @Column(name = "share_count")
    private Long shareCount = 0L;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "tags", length = 512)
    private String tags;

    @Column(name = "viral_score")
    private Integer viralScore = 0;

    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult;

    @Column(name = "publish_time")
    private Timestamp publishTime;

    /** 深度分析状态：pending/processing/completed/failed */
    @Column(name = "deep_analyze_status", length = 16)
    private String deepAnalyzeStatus = "pending";

    /** 仿制状态：0=未仿制 1=仿制中 2=已仿制 */
    @Column(name = "remake_status")
    private Integer remakeStatus = 0;

    /** 行业标签（JSON 数组） */
    @Column(name = "industry_tags", length = 512)
    private String industryTags;

    /** 是否自动采集 */
    @Column(name = "auto_collected", nullable = false)
    private boolean autoCollected = false;

    /** 采集来源 */
    @Column(name = "collect_source", length = 64)
    private String collectSource;

    /** 采集任务 ID */
    @Column(name = "collect_task_id")
    private Long collectTaskId;

    /** 封面 BOS 存储 URL */
    @Column(name = "cover_bos_url", length = 512)
    private String coverBosUrl;

    /** 视频 BOS 存储 URL */
    @Column(name = "video_bos_url", length = 512)
    private String videoBosUrl;

    /** 评论数 */
    @Column(name = "comment_count")
    private Long commentCount;

    /** 收藏数 */
    @Column(name = "favorite_count")
    private Long favoriteCount;

    /** 视频时长（秒） */
    @Column(name = "video_duration")
    private Integer videoDuration;

    /** 视频语音转文字 */
    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    /** 深度分析结果 JSON */
    @Column(name = "deep_analysis_result", columnDefinition = "TEXT")
    private String deepAnalysisResult;

    /** 视频描述/简介 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 话题标签（JSON 数组） */
    @Column(name = "hashtags", length = 512)
    private String hashtags;

    /** 场景描述（JSON 数组） */
    @Column(name = "scene_descriptions", columnDefinition = "TEXT")
    private String sceneDescriptions;

    /** 深度分析进度描述 */
    @Column(name = "deep_analyze_progress", length = 256)
    private String deepAnalyzeProgress;

    /** 二创变量表（JSON，提炼自爆款拆解） */
    @Column(name = "remake_variable_table", columnDefinition = "TEXT")
    private String remakeVariableTable;

    /** 作者 ID */
    @Column(name = "author_id", length = 128)
    private String authorId;

    /** 作者粉丝数 */
    @Column(name = "author_followers")
    private Long authorFollowers;

    /** 音乐名称 */
    @Column(name = "music_name", length = 128)
    private String musicName;

    /** 元数据 JSON */
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    /** 视频 BOS 存储 Key */
    @Column(name = "video_bos_key", length = 512)
    private String videoBosKey;

    /** 关键帧 BOS Key 列表（JSON） */
    @Column(name = "keyframe_bos_keys", columnDefinition = "TEXT")
    private String keyframeBosKeys;

    /** 关键帧 BOS URL 列表（JSON） */
    @Column(name = "keyframe_bos_urls", columnDefinition = "TEXT")
    private String keyframeBosUrls;

    /** 封面 BOS Key */
    @Column(name = "cover_bos_key", length = 512)
    private String coverBosKey;

    /** 深度分析完成时间 */
    @Column(name = "deep_analyzed_at")
    private java.sql.Timestamp deepAnalyzedAt;

    /** 深度分析步骤记录（JSON） */
    @Column(name = "deep_analyze_steps", columnDefinition = "TEXT")
    private String deepAnalyzeSteps;

    /** AI 二创建议（JSON） */
    @Column(name = "remake_suggestions", columnDefinition = "TEXT")
    private String remakeSuggestions;

    /** 匹配的人设 ID 列表（JSON） */
    @Column(name = "matched_persona_ids", length = 512)
    private String matchedPersonaIds;

    /** 二创确认人 ID */
    @Column(name = "confirmed_by")
    private Long confirmedBy;

    /** 二创确认时间 */
    @Column(name = "confirmed_at")
    private java.sql.Timestamp confirmedAt;

    /** 确认的二创类型 */
    @Column(name = "confirmed_remake_type", length = 32)
    private String confirmedRemakeType;

    /** 关联二创脚本 ID */
    @Column(name = "remake_script_id")
    private Long remakeScriptId;

    /** 关联二创任务 ID */
    @Column(name = "remake_task_id")
    private Long remakeTaskId;

    /** 深度分析开始时间 */
    @Column(name = "deep_analyze_started_at")
    private java.sql.Timestamp deepAnalyzeStartedAt;

    /** 轻量分析结果（JSON，快速预览） */
    @Column(name = "light_analysis_result", columnDefinition = "TEXT")
    private String lightAnalysisResult;

    /** 深度分析错误信息 */
    @Column(name = "deep_analyze_error", length = 512)
    private String deepAnalyzeError;

    /** 深度分析完成时间 */
    @Column(name = "deep_analyze_finished_at")
    private java.sql.Timestamp deepAnalyzeFinishedAt;

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
