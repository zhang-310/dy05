package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 内容分类验证结果VO
 */
@Data
public class ContentClassificationVO {

    /**
     * 脚本ID
     */
    private Long scriptId;

    /**
     * 脚本内容
     */
    private String scriptContent;

    /**
     * 目标行业
     */
    private String targetIndustry;

    /**
     * AI识别的行业
     */
    private String detectedIndustry;

    /**
     * 行业相关度分数（0-100）
     */
    private BigDecimal industryRelevanceScore;

    /**
     * 目标场景
     */
    private String targetSceneType;

    /**
     * AI识别的场景
     */
    private String detectedSceneType;

    /**
     * 场景相关度分数（0-100）
     */
    private BigDecimal sceneRelevanceScore;

    /**
     * AI识别的脚本类型
     */
    private String detectedScriptType;

    /**
     * 综合相关度分数（0-100）
     */
    private BigDecimal overallRelevanceScore;

    /**
     * 是否匹配目标分类
     */
    private Boolean isMatch;

    /**
     * 是否需要人工审核
     */
    private Boolean needsManualReview;

    /**
     * 建议的分类
     */
    private String suggestedIndustry;

    /**
     * 建议的场景
     */
    private String suggestedSceneType;

    /**
     * 建议的脚本类型
     */
    private String suggestedScriptType;

    /**
     * 不匹配原因
     */
    private String mismatchReason;

    /**
     * 关键词匹配列表
     */
    private List<String> matchedKeywords;

    /**
     * 不匹配关键词列表
     */
    private List<String> mismatchedKeywords;

    /**
     * AI分析详情
     */
    private String aiAnalysisDetail;

    /**
     * 置信度（0-1）
     */
    private BigDecimal confidence;
}
