package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.EvolutionRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Evolution Rule Engine Service Interface
 * Evaluates and executes the 4 core evolution rules:
 * 1. INCLUSION_RULE - Auto-import high-performance scripts
 * 2. UPDATE_RULE - Auto-update script versions
 * 3. ARCHIVAL_RULE - Auto-archive low-performers
 * 4. DEDUP_RULE - Auto-merge duplicates
 */
public interface EvolutionRuleEngineService {

    /**
     * Evaluate INCLUSION_RULE: scripts ready for library inclusion
     * Condition: score >= threshold AND usage >= min_usage AND created > min_days_ago
     *
     * @param userId User ID
     * @return List of script version IDs meeting criteria
     */
    List<Long> evaluateInclusionRule(Long userId);

    /**
     * Evaluate UPDATE_RULE: scripts with better versions available
     * Condition: new_score > old_score + threshold AND new_score >= min_threshold
     *
     * @param userId User ID
     * @return Map of updates: scriptId -> {oldVersion, newVersion, reason}
     */
    Map<Long, Map<String, Object>> evaluateUpdateRule(Long userId);

    /**
     * Evaluate ARCHIVAL_RULE: scripts with consistent low performance
     * Condition: consecutive_low_scores >= threshold AND last_low_score_period >= min_days
     *
     * @param userId User ID
     * @return List of script version IDs meeting archival criteria
     */
    List<Long> evaluateArchivalRule(Long userId);

    /**
     * Evaluate DEDUP_RULE: detect and identify duplicate scripts
     * Condition: vector_similarity >= threshold
     *
     * @param userId User ID
     * @return List of dedup groups with master and duplicate IDs
     */
    List<Map<String, Object>> evaluateDedupRule(Long userId);

    /**
     * Execute INCLUSION_RULE: import scripts to library
     *
     * @param userId User ID
     * @param scriptVersionIds Script version IDs to include
     * @return Number of scripts included
     */
    Integer executeInclusionRule(Long userId, List<Long> scriptVersionIds);

    /**
     * Execute UPDATE_RULE: update script versions
     *
     * @param userId User ID
     * @param updates Map of updates from evaluateUpdateRule
     * @return Number of scripts updated
     */
    Integer executeUpdateRule(Long userId, Map<Long, Map<String, Object>> updates);

    /**
     * Execute ARCHIVAL_RULE: archive low-performance scripts
     *
     * @param userId User ID
     * @param scriptVersionIds Script version IDs to archive
     * @return Number of scripts archived
     */
    Integer executeArchivalRule(Long userId, List<Long> scriptVersionIds);

    /**
     * Execute DEDUP_RULE: merge duplicate scripts
     *
     * @param userId User ID
     * @param dedupGroups Dedup groups from evaluateDedupRule
     * @return Number of merges completed
     */
    Integer executeDedupRule(Long userId, List<Map<String, Object>> dedupGroups);

    /**
     * Get rule configuration
     *
     * @param ruleType Rule type: INCLUSION_RULE, UPDATE_RULE, ARCHIVAL_RULE, DEDUP_RULE
     * @return Rule configuration
     */
    EvolutionRule getRule(String ruleType);

    /**
     * Update rule configuration
     *
     * @param ruleType Rule type
     * @param config New configuration (JSON string)
     * @return Updated rule
     */
    EvolutionRule updateRuleConfig(String ruleType, String config);

    /**
     * Enable/disable rule
     *
     * @param ruleType Rule type
     * @param enabled Whether to enable
     */
    void setRuleEnabled(String ruleType, boolean enabled);

    /**
     * Trigger all rules for a user (used by scheduled task)
     *
     * @param userId User ID
     * @return Summary of all rule executions
     */
    Map<String, Object> triggerAllRules(Long userId);
}
