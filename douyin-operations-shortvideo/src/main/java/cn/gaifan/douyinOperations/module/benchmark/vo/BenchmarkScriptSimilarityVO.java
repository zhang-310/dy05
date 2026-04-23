package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 脚本相似度结果 VO
 */
@Data
public class BenchmarkScriptSimilarityVO {

    /**
     * 质量脚本 ID
     */
    private Long scriptId;

    /**
     * 视频 ID
     */
    private Long videoId;

    /**
     * 脚本内容（摘要）
     */
    private String scriptContent;

    /**
     * 脚本类型
     */
    private String scriptType;

    /**
     * 行业
     */
    private String industry;

    /**
     * 场景类型
     */
    private String sceneType;

    /**
     * 质量评分
     */
    private BigDecimal qualityScore;

    /**
     * 相似度分数（0-1）
     */
    private Double similarityScore;

    /**
     * 互动率
     */
    private BigDecimal engagementRate;

    /**
     * 传播力评分
     */
    private BigDecimal viralScore;

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
}
