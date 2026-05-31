package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Evolution Report VO
 * Response for weekly/monthly evolution reports
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolutionReportVO {

    /**
     * Report ID
     */
    private String reportId;

    /**
     * Report period
     */
    private String period;

    /**
     * Overview statistics
     */
    private Overview overview;

    /**
     * Top scripts ranking
     */
    private List<TopScript> topScripts;

    /**
     * Analysis by style/category
     */
    private Map<String, StyleAnalysis> styleAnalysis;

    /**
     * Recommendations for improvement
     */
    private List<Recommendation> recommendations;

    /**
     * Generation timestamp
     */
    private String generatedAt;

    // ─── Inner Classes ───

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        /** Total scripts in library */
        private Integer totalScriptsInLibrary;
        /** Newly added count in period */
        private Integer newAddedCount;
        /** Archived count in period */
        private Integer archivedCount;
        /** Deduplicated count in period */
        private Integer deduplicatedCount;
        /** Average library quality score */
        private BigDecimal averageScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopScript {
        private Integer rank;
        private Long scriptId;
        private String title;
        private BigDecimal score;
        private Integer usageCount;
        private BigDecimal adoptionRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StyleAnalysis {
        /** Script count in style */
        private Integer count;
        /** Average score for style */
        private BigDecimal averageScore;
        /** Trend: UP, DOWN, STABLE */
        private String trend;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        /** Type: STYLE_GAP, QUALITY_ISSUE, DEDUP_OPPORTUNITY, OPTIMIZATION_NEEDED */
        private String type;
        /** Description */
        private String description;
        /** Priority: HIGH, MEDIUM, LOW */
        private String priority;
    }
}
