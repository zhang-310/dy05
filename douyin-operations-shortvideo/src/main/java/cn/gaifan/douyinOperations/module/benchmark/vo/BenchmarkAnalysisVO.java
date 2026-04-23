package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 深度分析结果返回VO
 */
@Data
public class BenchmarkAnalysisVO {

    private Long id;
    private Long benchmarkVideoId;
    private String transcriptText;
    private String ocrText;
    private String apiDescription;
    private String mergedContent;
    private Integer sceneCount;
    private String keyFramesJson;
    private String sceneDescription;
    private String creativeType;
    private String hookStrategy;
    private String contentStructure;
    private String emotionalCurve;
    private String pacingAnalysis;
    private String viralFactors;
    private String strengths;
    private String weaknesses;
    private String replicableElements;
    private String aiSummary;
    private String scriptBreakdown;
    private String improvementSuggestions;
    private String targetAudience;
    private String comparisonReport;
    private String differentiationPoints;
    private String aiModelUsed;
    private Long tokensUsed;
    private Integer analysisDurationMs;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
