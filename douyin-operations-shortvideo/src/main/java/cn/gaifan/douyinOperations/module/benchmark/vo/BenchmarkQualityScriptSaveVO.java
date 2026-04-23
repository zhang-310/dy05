package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 质量脚本保存 VO
 */
@Data
public class BenchmarkQualityScriptSaveVO {

    /**
     * ID（更新时必填）
     */
    private Long id;

    /**
     * 视频 ID
     */
    @NotNull(message = "视频 ID 不能为空")
    private Long videoId;

    /**
     * 分析 ID
     */
    @NotNull(message = "分析 ID 不能为空")
    private Long analysisId;

    /**
     * 脚本内容
     */
    @NotBlank(message = "脚本内容不能为空")
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
    @NotNull(message = "质量评分不能为空")
    @DecimalMin(value = "0.0", message = "质量评分不能小于 0")
    @DecimalMax(value = "100.0", message = "质量评分不能大于 100")
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
}
