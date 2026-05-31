package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Evolution Opportunity VO
 * Response for evolution analysis: scripts ready for inclusion, needing optimization, duplicates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolutionOpportunityVO {

    /**
     * Analysis ID for tracking
     */
    private String analysisId;

    /**
     * Period start date
     */
    private String periodStart;

    /**
     * Period end date
     */
    private String periodEnd;

    /**
     * Scripts ready for library inclusion
     */
    private List<ScriptOpportunity> readyForInclusion;

    /**
     * Scripts needing optimization
     */
    private List<OptimizationOpportunity> needsOptimization;

    /**
     * Detected duplicates
     */
    private List<DuplicateGroup> duplicatesDetected;

    /**
     * Scripts ready for archival
     */
    private List<ArchivalOpportunity> readyForArchival;

    /**
     * Expected impact from execution
     */
    private ExpectedImpact expectedImpact;

    /**
     * Creation timestamp
     */
    private String createdAt;

    /**
     * When true, analysis is a stub (e.g. {@code EvolutionRuleEngineService} not available).
     */
    private Boolean degraded;

    // ─── Inner Classes ───

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptOpportunity {
        private Long scriptVersionId;
        private String title;
        private BigDecimal score;
        private Integer usageCount;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizationOpportunity {
        private Long scriptVersionId;
        private String title;
        private BigDecimal score;
        private Integer consecutiveLowScore;
        private String suggestion;
        private Long referenceScriptId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DuplicateGroup {
        private Long masterScriptId;
        private String masterTitle;
        private BigDecimal masterScore;
        private List<Long> duplicateScriptIds;
        private BigDecimal similarityScore;
        private String recommendation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchivalOpportunity {
        private Long scriptVersionId;
        private String title;
        private BigDecimal currentScore;
        private String reason;
        private Integer monthsSinceDeprecation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpectedImpact {
        private Integer newInclusionsCount;
        private Integer deduplicationCount;
        private BigDecimal improvementRate;
    }
}
