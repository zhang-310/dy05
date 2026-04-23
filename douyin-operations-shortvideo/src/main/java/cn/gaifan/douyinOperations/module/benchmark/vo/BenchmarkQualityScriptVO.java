package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 质量脚本返回 VO
 */
@Data
public class BenchmarkQualityScriptVO {

    /**
     * ID
     */
    private Long id;

    /**
     * 视频 ID
     */
    private Long videoId;

    /**
     * 分析 ID
     */
    private Long analysisId;

    /**
     * 脚本内容
     */
    private String scriptContent;

    /**
     * 脚本类型
     */
    private String scriptType;

    /**
     * 行业分类
     */
    private String industry;

    /**
     * 场景类型
     */
    private String sceneType;

    /**
     * 质量评分（0-100）
     */
    private BigDecimal qualityScore;

    /**
     * 互动率（%）
     */
    private BigDecimal engagementRate;

    /**
     * 传播力评分
     */
    private BigDecimal viralScore;

    /**
     * 完播率（%）
     */
    private BigDecimal completionRate;

    /**
     * AI 评分
     */
    private BigDecimal aiRating;

    /**
     * 点赞数
     */
    private Integer likesCount;

    /**
     * 评论数
     */
    private Integer commentsCount;

    /**
     * 分享数
     */
    private Integer sharesCount;

    /**
     * 收藏数
     */
    private Integer collectionsCount;

    /**
     * 播放数
     */
    private Integer viewsCount;

    /**
     * 视频时长（秒）
     */
    private Integer videoDuration;

    /**
     * 关键特征（JSON 格式）
     */
    private String keyFeatures;

    /**
     * 创意元素（JSON 格式）
     */
    private String creativeElements;

    /**
     * 钩子策略
     */
    private String hookStrategy;

    /**
     * 内容结构
     */
    private String contentStructure;

    /**
     * 被引用次数
     */
    private Integer referenceCount;

    /**
     * 最后引用时间
     */
    private LocalDateTime lastReferencedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
