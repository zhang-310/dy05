package cn.gaifan.douyinOperations.module.ai.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 统一质量评分服务。
 * 整合知识库质量评分（KnowledgeQualityScoringService）和话术质量评估（ScriptQualityEvaluator）
 * 为两套体系提供统一的评分接口和维度。
 */
public interface QualityScoreService {

    /** 知识库整体质量评分 */
    BigDecimal getLibraryScore(Long userId);

    /** 单篇话术质量评分（调用 ScriptQualityEvaluator） */
    Map<String, Object> evaluateScript(String content, String ipType, Long userId);

    /**
     * 综合评估：知识库质量 + 话术生成质量 + 效果反馈
     * @return {libraryScore, avgScriptScore, effectivenessScore, overallScore, grade}
     */
    Map<String, Object> getOverallQuality(Long userId);
}
