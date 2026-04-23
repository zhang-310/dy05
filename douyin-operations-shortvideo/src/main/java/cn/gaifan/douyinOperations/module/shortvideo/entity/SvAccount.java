package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 短视频账号主表
 * <p>
 * 统一管理所有采集的账号信息，支持账号级别的数据分析和统计。
 * 一个账号（sec_uid）对应多个采集任务和多个爆款视频。
 */
@Data
@Entity
@Table(name = "sv_account")
@SQLRestriction("deleted = 0")
public class SvAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // ─── 账号基本信息 ──────────────────────────────────────

    /** 抖音唯一标识（全局唯一） */
    @Column(name = "sec_uid", nullable = false, length = 256)
    private String secUid;

    /** 抖音号 */
    @Column(name = "douyin_id", length = 128)
    private String douyinId;

    /** 昵称 */
    @Column(name = "nickname", length = 128)
    private String nickname;

    /** 头像 URL */
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    /** 个人简介 */
    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;

    // ─── 账号数据 ──────────────────────────────────────

    /** 粉丝数 */
    @Column(name = "follower_count")
    private Long followerCount = 0L;

    /** 关注数 */
    @Column(name = "following_count")
    private Long followingCount = 0L;

    /** 获赞总数 */
    @Column(name = "total_favorited")
    private Long totalFavorited = 0L;

    /** 作品数 */
    @Column(name = "video_count")
    private Integer videoCount = 0;

    // ─── 认证信息 ──────────────────────────────────────

    /** 是否认证 */
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    /** 认证类型：personal/enterprise/government */
    @Column(name = "verification_type", length = 32)
    private String verificationType;

    // ─── 采集统计 ──────────────────────────────────────

    /** 采集次数 */
    @Column(name = "collect_count")
    private Integer collectCount = 0;

    /** 最后采集时间 */
    @Column(name = "last_collect_time")
    private Timestamp lastCollectTime;

    /** 累计采集视频数 */
    @Column(name = "total_collected_videos")
    private Integer totalCollectedVideos = 0;

    // ─── 分析统计 ──────────────────────────────────────

    /** 平均播放量 */
    @Column(name = "avg_view_count")
    private Long avgViewCount = 0L;

    /** 平均点赞数 */
    @Column(name = "avg_like_count")
    private Integer avgLikeCount = 0;

    /** 平均分享数 */
    @Column(name = "avg_share_count")
    private Integer avgShareCount = 0;

    /** 平均评论数 */
    @Column(name = "avg_comment_count")
    private Integer avgCommentCount = 0;

    /** 平均爆款评分 */
    @Column(name = "avg_viral_score", precision = 5, scale = 2)
    private BigDecimal avgViralScore = BigDecimal.ZERO;

    /** 最高爆款评分 */
    @Column(name = "top_viral_score", precision = 5, scale = 2)
    private BigDecimal topViralScore = BigDecimal.ZERO;

    // ─── 标签与分类 ──────────────────────────────────────

    /** 行业标签（JSON 数组） */
    @Column(name = "industry_tags", length = 512)
    private String industryTags;

    /** 内容标签（JSON 数组） */
    @Column(name = "content_tags", length = 512)
    private String contentTags;

    /** 账号分类 */
    @Column(name = "account_category", length = 64)
    private String accountCategory;

    // ─── 来源信息 ──────────────────────────────────────

    /** 来源类型：manual/keyword_search/recommend/import */
    @Column(name = "source_type", length = 32)
    private String sourceType = "manual";

    /** 来源关键词（关键词采集时记录） */
    @Column(name = "source_keyword", length = 256)
    private String sourceKeyword;

    /** 来源采集任务 ID */
    @Column(name = "source_task_id")
    private Long sourceTaskId;

    // ─── 备注与状态 ──────────────────────────────────────

    /** 备注 */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** 账号状态：active/archived/blocked */
    @Column(name = "status", length = 32)
    private String status = "active";

    // ─── 系统字段 ──────────────────────────────────────

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
