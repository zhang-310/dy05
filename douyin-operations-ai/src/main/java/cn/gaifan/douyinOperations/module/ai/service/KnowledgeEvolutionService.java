package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionOpportunityVO;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionReportVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Knowledge Library Evolution Service Interface
 * Core service for analyzing and executing evolution opportunities
 */
public interface KnowledgeEvolutionService {

    /**
     * Analyze evolution opportunities in a period
     *
     * @param userId User ID
     * @param periodDays Analysis period in days (e.g., 7, 30, 90)
     * @return Analysis result with opportunities
     */
    EvolutionOpportunityVO analyzeEvolutionOpportunities(Long userId, int periodDays);

    /**
     * Execute auto-optimization on analyzed opportunities
     *
     * @param userId User ID
     * @param analysisId Analysis ID from previous analysis
     * @param autoInclude Whether to auto-include high-performance scripts
     * @param autoMerge Whether to auto-merge duplicates
     * @param autoArchive Whether to auto-archive low-performers
     * @return Execution result
     */
    Map<String, Object> executeAutoOptimization(Long userId, String analysisId,
            boolean autoInclude, boolean autoMerge, boolean autoArchive);

    /**
     * Generate evolution report for a period
     *
     * @param userId User ID
     * @param reportType WEEKLY or MONTHLY
     * @param startDate Period start
     * @param endDate Period end
     * @return Report data
     */
    EvolutionReportVO generateEvolutionReport(Long userId, String reportType, LocalDate startDate, LocalDate endDate);

    /**
     * Manually trigger deduplication scan and merge
     *
     * @param userId User ID
     * @param similarityThreshold Similarity threshold for dedup (default 0.85)
     * @return Merge results
     */
    Map<String, Object> deduplicateKnowledge(Long userId, BigDecimal similarityThreshold);

    /**
     * Get evolution history for a specific script
     *
     * @param userId User ID
     * @param scriptVersionId Script version ID
     * @param page Page index
     * @param pageSize Page size
     * @return Paginated evolution history
     */
    PageResultVO<Map<String, Object>> getEvolutionHistory(Long userId, Long scriptVersionId, int page, int pageSize);

    /**
     * Recalculate quality scores for all scripts
     * (Usually called by scheduled task)
     *
     * @param userId User ID
     * @param periodDays Evaluation period
     * @return Number of scores recalculated
     */
    Integer recalculateQualityScores(Long userId, int periodDays);
}
