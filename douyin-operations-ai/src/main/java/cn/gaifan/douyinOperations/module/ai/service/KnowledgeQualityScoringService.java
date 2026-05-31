package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeQualityScore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Knowledge Quality Scoring Service Interface
 * Manages quality score calculation and tracking for all scripts
 */
public interface KnowledgeQualityScoringService {

    /**
     * Calculate and save quality score for a script in a period
     *
     * @param userId User ID
     * @param scriptVersionId Script version ID
     * @param periodStart Period start date
     * @param periodEnd Period end date
     * @return Calculated quality score
     */
    KnowledgeQualityScore calculateAndSaveQualityScore(Long userId, Long scriptVersionId,
            LocalDate periodStart, LocalDate periodEnd);

    /**
     * Calculate library-wide quality score (average)
     *
     * @param userId User ID
     * @return Average quality score for user's library
     */
    BigDecimal calculateLibraryQualityScore(Long userId);

    /**
     * Get quality score trend for a script
     *
     * @param userId User ID
     * @param scriptVersionId Script version ID
     * @param periodCount Number of recent periods to analyze
     * @return List of scores with trend direction
     */
    List<Map<String, Object>> getQualityScoreTrend(Long userId, Long scriptVersionId, int periodCount);

    /**
     * Identify scripts with consecutive low scores
     *
     * @param userId User ID
     * @param threshold Score threshold
     * @param consecutiveCount Number of consecutive low-score periods
     * @return List of script IDs with pattern
     */
    List<Long> findScriptsWithConsecutiveLowScores(Long userId, BigDecimal threshold, Integer consecutiveCount);

    /**
     * Update consecutive low score counter
     *
     * @param userId User ID
     * @param scriptVersionId Script version ID
     * @param score Latest score
     * @param lowThreshold Low score threshold
     */
    void updateConsecutiveLowScoreCounter(Long userId, Long scriptVersionId, BigDecimal score, BigDecimal lowThreshold);

    /**
     * Batch recalculate quality scores for all scripts
     *
     * @param userId User ID
     * @param periodStart Period start
     * @param periodEnd Period end
     * @return Number of scores recalculated
     */
    Integer batchRecalculateQualityScores(Long userId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * Get quality score history for export/reporting
     *
     * @param userId User ID
     * @param scriptVersionId Script version ID (null = all scripts)
     * @param periodStart Start date
     * @param periodEnd End date
     * @return Quality score history
     */
    List<KnowledgeQualityScore> getQualityScoreHistory(Long userId, Long scriptVersionId,
            LocalDate periodStart, LocalDate periodEnd);

    /**
     * Determine quality score trend (UP, DOWN, STABLE)
     *
     * @param currentScore Current score
     * @param previousScore Previous period score
     * @return Trend direction
     */
    String determineTrend(BigDecimal currentScore, BigDecimal previousScore);
}
