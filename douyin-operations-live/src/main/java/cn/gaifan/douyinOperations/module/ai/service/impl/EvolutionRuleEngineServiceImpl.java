package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.*;
import cn.gaifan.douyinOperations.module.ai.repository.*;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionRuleEngineService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeQualityScoringService;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptVersion;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptVersionRepository;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.*;

/**
 * Evolution Rule Engine Service Implementation
 * Implements 4 core rules: INCLUSION, UPDATE, ARCHIVAL, DEDUP
 */
@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class EvolutionRuleEngineServiceImpl implements EvolutionRuleEngineService {

    @Resource
    private EvolutionRuleRepository ruleRepository;
    @Resource
    private KnowledgeEvolutionLogRepository evolutionLogRepository;
    @Resource
    private KnowledgeQualityScoreRepository qualityScoreRepository;
    @Resource
    private KnowledgeDeduplicationGroupRepository dedupGroupRepository;
    @Autowired(required = false)
    private LiveScriptVersionRepository liveScriptVersionRepository;
    @Autowired(required = false)
    private KnowledgeQualityScoringService qualityScoringService;

    // Rule type constants
    private static final String INCLUSION_RULE = "INCLUSION_RULE";
    private static final String UPDATE_RULE = "UPDATE_RULE";
    private static final String ARCHIVAL_RULE = "ARCHIVAL_RULE";
    private static final String DEDUP_RULE = "DEDUP_RULE";

    @Override
    public List<Long> evaluateInclusionRule(Long userId) {
        log.debug("Evaluating INCLUSION_RULE for user {}", userId);

        EvolutionRule rule = getEnabledRule(INCLUSION_RULE);
        if (rule == null) {
            log.warn("INCLUSION_RULE is disabled");
            return new ArrayList<>();
        }

        JSONObject config = JSON.parseObject(rule.getConfig());
        BigDecimal scoreThreshold = new BigDecimal(config.getIntValue("score_threshold"));
        Integer minUsageCount = config.getIntValue("min_usage_count");
        Integer minCreationDaysAgo = config.getIntValue("min_creation_days_ago");

        List<KnowledgeQualityScore> allScores = Optional
                .ofNullable(qualityScoreRepository.findByUserIdAndDeletedOrderByPeriodEndDesc(userId, 0))
                .orElse(List.of());
        Map<Long, KnowledgeQualityScore> latestByScript = latestScoresByScript(allScores);
        LocalDate cutoff = minCreationDaysAgo > 0 ? LocalDate.now().minusDays(minCreationDaysAgo) : null;
        List<Long> scriptIds = latestByScript.values().stream()
                .filter(score -> score.getQualityScore() != null && score.getQualityScore().compareTo(scoreThreshold) >= 0)
                .filter(score -> score.getUsageCount() != null && score.getUsageCount() >= minUsageCount)
                .filter(score -> cutoff == null || !score.getPeriodEnd().toLocalDate().isAfter(cutoff))
                .map(KnowledgeQualityScore::getScriptVersionId)
                .toList();

        log.debug("INCLUSION_RULE evaluation complete: {} scripts ready", scriptIds.size());
        return scriptIds;
    }

    @Override
    public Map<Long, Map<String, Object>> evaluateUpdateRule(Long userId) {
        log.debug("Evaluating UPDATE_RULE for user {}", userId);

        EvolutionRule rule = getEnabledRule(UPDATE_RULE);
        if (rule == null) {
            log.warn("UPDATE_RULE is disabled");
            return new HashMap<>();
        }

        JSONObject config = JSON.parseObject(rule.getConfig());
        BigDecimal scoreImprovementThreshold = new BigDecimal(
                config.getDoubleValue("score_improvement_threshold"));
        BigDecimal minNewVersionScore = new BigDecimal(config.getIntValue("min_new_version_score"));
        Integer stabilityDays = config.getIntValue("stability_days");

        Map<Long, Map<String, Object>> updates = new HashMap<>();
        Map<Long, List<KnowledgeQualityScore>> grouped = Optional
                .ofNullable(qualityScoreRepository.findByUserIdAndDeletedOrderByPeriodEndDesc(userId, 0))
                .orElse(List.of())
                .stream()
                .collect(Collectors.groupingBy(KnowledgeQualityScore::getScriptVersionId));
        Timestamp stableBefore = stabilityDays != null && stabilityDays > 0
                ? Timestamp.valueOf(LocalDate.now().minusDays(stabilityDays).atStartOfDay())
                : null;

        for (Map.Entry<Long, List<KnowledgeQualityScore>> entry : grouped.entrySet()) {
            List<KnowledgeQualityScore> scores = entry.getValue().stream()
                    .sorted(Comparator.comparing(KnowledgeQualityScore::getPeriodEnd).reversed())
                    .toList();
            if (scores.size() < 2) {
                continue;
            }
            KnowledgeQualityScore latest = scores.get(0);
            KnowledgeQualityScore previous = scores.get(1);
            if (latest.getQualityScore() == null || previous.getQualityScore() == null) {
                continue;
            }
            if (latest.getQualityScore().compareTo(minNewVersionScore) < 0) {
                continue;
            }
            if (stableBefore != null && latest.getUpdatedAt() != null && latest.getUpdatedAt().after(stableBefore)) {
                continue;
            }
            BigDecimal improvement = latest.getQualityScore().subtract(previous.getQualityScore());
            if (improvement.compareTo(scoreImprovementThreshold) > 0) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("oldScore", previous.getQualityScore());
                detail.put("newScore", latest.getQualityScore());
                detail.put("improvement", improvement);
                detail.put("usageCount", latest.getUsageCount());
                detail.put("reason", "Latest score exceeds previous score by threshold");
                updates.put(entry.getKey(), detail);
            }
        }

        log.debug("UPDATE_RULE evaluation complete: {} scripts eligible for update", updates.size());
        return updates;
    }

    @Override
    public List<Long> evaluateArchivalRule(Long userId) {
        log.debug("Evaluating ARCHIVAL_RULE for user {}", userId);

        EvolutionRule rule = getEnabledRule(ARCHIVAL_RULE);
        if (rule == null) {
            log.warn("ARCHIVAL_RULE is disabled");
            return new ArrayList<>();
        }

        JSONObject config = JSON.parseObject(rule.getConfig());
        Integer consecutiveLowScorePeriods = config.getIntValue("consecutive_low_score_periods");
        BigDecimal lowScoreThreshold = new BigDecimal(config.getIntValue("low_score_threshold"));
        if (qualityScoringService == null) {
            log.warn("KnowledgeQualityScoringService 未注入，无法评估归档规则");
            return new ArrayList<>();
        }

        // Find scripts with consecutive low scores
        List<Long> archivalCandidates = qualityScoringService.findScriptsWithConsecutiveLowScores(
                userId, lowScoreThreshold, consecutiveLowScorePeriods);

        log.debug("ARCHIVAL_RULE evaluation complete: {} scripts eligible for archival", archivalCandidates.size());
        return archivalCandidates;
    }

    @Override
    public List<Map<String, Object>> evaluateDedupRule(Long userId) {
        log.debug("Evaluating DEDUP_RULE for user {}", userId);

        EvolutionRule rule = getEnabledRule(DEDUP_RULE);
        if (rule == null) {
            log.warn("DEDUP_RULE is disabled");
            return new ArrayList<>();
        }

        JSONObject config = JSON.parseObject(rule.getConfig());
        BigDecimal similarityThreshold = new BigDecimal(
                config.getDoubleValue("similarity_threshold"));

        // Find duplicate candidates above similarity threshold
        List<KnowledgeDeduplicationGroup> dedupCandidates =
                dedupGroupRepository.findDuplicateCandidates(userId, similarityThreshold);

        List<Map<String, Object>> results = new ArrayList<>();
        for (KnowledgeDeduplicationGroup group : dedupCandidates) {
            Map<String, Object> item = new HashMap<>();
            item.put("masterScriptId", group.getMasterScriptId());
            item.put("duplicateIds", List.of(group.getDuplicateScriptId()));
            item.put("similarity", group.getSimilarityScore());
            results.add(item);
        }

        log.debug("DEDUP_RULE evaluation complete: {} duplicate groups detected", results.size());
        return results;
    }

    @Override
    public Integer executeInclusionRule(Long userId, List<Long> scriptVersionIds) {
        log.info("Executing INCLUSION_RULE for user {}: {} scripts", userId, scriptVersionIds.size());

        int count = 0;
        for (Long scriptId : scriptVersionIds) {
            try {
                LiveScriptVersion version = resolveOwnedVersionFlexible(userId, scriptId).orElse(null);
                if (version == null) {
                    log.warn("纳入跳过：无法解析话术版本 candidateId={} userId={}（无主键记录或 script_version_id 存成话术主键 live_script.id）", scriptId, userId);
                    continue;
                }
                BigDecimal score = latestQualityScore(version.getId());
                String oldValue = versionSnapshot(version);
                version.setVersionStatus("active");
                version.setIsRecommended(1);
                version.setRecommendReason("知识进化自动纳入：满足入库阈值");
                if (score != null) {
                    version.setRecommendScore(score);
                }
                version.setChangeSummary(JSON.toJSONString(Map.of(
                        "action", "auto_import",
                        "qualityScore", score,
                        "userId", userId
                )));
                liveScriptVersionRepository.save(version);

                // Log the action
                KnowledgeEvolutionLog log = new KnowledgeEvolutionLog();
                log.setUserId(userId);
                log.setScriptVersionId(version.getId());
                log.setAction("auto_import");
                log.setRuleType(INCLUSION_RULE);
                log.setReason("Score >= 80 and usage >= 5");
                log.setStatus("COMPLETED");
                log.setExecutedBy("system");
                log.setOldValue(oldValue);
                log.setNewValue(versionSnapshot(version));
                evolutionLogRepository.save(log);

                count++;
            } catch (Exception e) {
                log.warn("纳入失败 scriptVersionCandidate={} userId={}: {}", scriptId, userId, e.getMessage());
            }
        }

        log.info("INCLUSION_RULE execution complete: {} scripts included", count);
        return count;
    }

    @Override
    public Integer executeUpdateRule(Long userId, Map<Long, Map<String, Object>> updates) {
        log.info("Executing UPDATE_RULE for user {}: {} scripts", userId, updates.size());

        int count = 0;
        for (Map.Entry<Long, Map<String, Object>> entry : updates.entrySet()) {
            Long scriptId = entry.getKey();
            try {
                LiveScriptVersion version = resolveOwnedVersionFlexible(userId, scriptId).orElse(null);
                if (version == null) {
                    log.warn("升级跳过：无法解析话术版本 candidateId={} userId={}", scriptId, userId);
                    continue;
                }
                Map<String, Object> detail = entry.getValue() != null ? entry.getValue() : Map.of();
                String oldValue = versionSnapshot(version);
                version.setVersionStatus("active");
                version.setIsRecommended(1);
                version.setRecommendReason(String.valueOf(detail.getOrDefault("reason", "知识进化自动升级版本")));
                BigDecimal newScore = extractBigDecimal(detail.get("newScore"));
                if (newScore != null) {
                    version.setRecommendScore(newScore);
                }
                version.setChangeSummary(JSON.toJSONString(detail));
                liveScriptVersionRepository.save(version);

                KnowledgeEvolutionLog log = new KnowledgeEvolutionLog();
                log.setUserId(userId);
                log.setScriptVersionId(version.getId());
                log.setAction("version_update");
                log.setRuleType(UPDATE_RULE);
                log.setReason("New version score > old + threshold");
                log.setStatus("COMPLETED");
                log.setExecutedBy("system");
                log.setOldValue(oldValue);
                log.setNewValue(versionSnapshot(version));
                evolutionLogRepository.save(log);

                count++;
            } catch (Exception e) {
                log.warn("升级失败 scriptVersionCandidate={} userId={}: {}", scriptId, userId, e.getMessage());
            }
        }

        log.info("UPDATE_RULE execution complete: {} scripts updated", count);
        return count;
    }

    @Override
    public Integer executeArchivalRule(Long userId, List<Long> scriptVersionIds) {
        log.info("Executing ARCHIVAL_RULE for user {}: {} scripts", userId, scriptVersionIds.size());

        int count = 0;
        for (Long scriptId : scriptVersionIds) {
            try {
                LiveScriptVersion version = resolveOwnedVersionFlexible(userId, scriptId).orElse(null);
                if (version == null) {
                    log.warn("归档跳过：无法解析话术版本 candidateId={} userId={}（无主键记录或 script_version_id 存成话术主键 live_script.id）", scriptId, userId);
                    continue;
                }
                String oldValue = versionSnapshot(version);
                version.setVersionStatus("archived");
                version.setIsRecommended(0);
                version.setRecommendReason("知识进化自动归档：连续低分");
                version.setRecommendScore(BigDecimal.ZERO);
                version.setChangeSummary(JSON.toJSONString(Map.of(
                        "action", "auto_archive",
                        "reason", "3+ consecutive low scores"
                )));
                liveScriptVersionRepository.save(version);

                KnowledgeEvolutionLog log = new KnowledgeEvolutionLog();
                log.setUserId(userId);
                log.setScriptVersionId(version.getId());
                log.setAction("auto_archive");
                log.setRuleType(ARCHIVAL_RULE);
                log.setReason("3+ consecutive low scores");
                log.setStatus("COMPLETED");
                log.setExecutedBy("system");
                log.setOldValue(oldValue);
                log.setNewValue(versionSnapshot(version));
                evolutionLogRepository.save(log);

                count++;
            } catch (Exception e) {
                log.warn("归档失败 scriptVersionCandidate={} userId={}: {}", scriptId, userId, e.getMessage());
            }
        }

        log.info("ARCHIVAL_RULE execution complete: {} scripts archived", count);
        return count;
    }

    @Override
    public Integer executeDedupRule(Long userId, List<Map<String, Object>> dedupGroups) {
        log.info("Executing DEDUP_RULE for user {}: {} merge operations", userId, dedupGroups.size());

        int count = 0;
        for (Map<String, Object> group : dedupGroups) {
            Long masterScriptId = (Long) group.get("masterScriptId");
            @SuppressWarnings("unchecked")
            List<Long> duplicateIds = (List<Long>) group.get("duplicateIds");

            for (Long duplicateId : duplicateIds) {
                try {
                    LiveScriptVersion master = resolveOwnedVersionFlexible(userId, masterScriptId).orElse(null);
                    LiveScriptVersion duplicate = resolveOwnedVersionFlexible(userId, duplicateId).orElse(null);
                    if (master == null || duplicate == null) {
                        log.warn("去重跳过：无法解析 masterCandidate={} duplicateCandidate={} userId={}", masterScriptId, duplicateId, userId);
                        continue;
                    }
                    String oldDuplicateValue = versionSnapshot(duplicate);
                    duplicate.setVersionStatus("archived");
                    duplicate.setIsRecommended(0);
                    duplicate.setRecommendReason("知识去重合并到主版本 " + masterScriptId);
                    duplicate.setRecommendScore(BigDecimal.ZERO);
                    duplicate.setBasedOnVersionId(master.getId());
                    duplicate.setChangeSummary(JSON.toJSONString(Map.of(
                            "action", "merge",
                            "masterScriptId", masterScriptId,
                            "similarity", group.get("similarity")
                    )));
                    liveScriptVersionRepository.save(duplicate);

                    master.setVersionStatus("active");
                    master.setIsRecommended(1);
                    if (master.getRecommendReason() == null || master.getRecommendReason().isBlank()) {
                        master.setRecommendReason("知识去重保留主版本");
                    }
                    liveScriptVersionRepository.save(master);

                    dedupGroupRepository.findByMasterScriptIdAndDuplicateScriptIdAndDeleted(masterScriptId, duplicateId, 0)
                            .ifPresent(dedup -> {
                                dedup.setMergeStatus("MERGED");
                                dedup.setVariantType("ARCHIVED");
                                dedup.setIsActive(0);
                                dedup.setMergeReason("系统自动合并重复话术");
                                dedup.setMergedAt(new Timestamp(System.currentTimeMillis()));
                                dedup.setNotes(JSON.toJSONString(Map.of(
                                        "similarity", group.get("similarity"),
                                        "executedBy", "system"
                                )));
                                dedupGroupRepository.save(dedup);
                            });

                    KnowledgeEvolutionLog log = new KnowledgeEvolutionLog();
                    log.setUserId(userId);
                    log.setScriptVersionId(master.getId());
                    log.setAction("merge");
                    log.setRuleType(DEDUP_RULE);
                    log.setReason("Duplicate detected (similarity > 0.85)");
                    log.setStatus("COMPLETED");
                    log.setExecutedBy("system");
                    log.setOldValue(oldDuplicateValue);
                    log.setNewValue(JSON.toJSONString(Map.of(
                            "mergedWith", duplicate.getId(),
                            "duplicateVersion", versionSnapshot(duplicate)
                    )));
                    evolutionLogRepository.save(log);

                    count++;
                } catch (Exception e) {
                    log.warn("去重失败 masterCandidate={} duplicateCandidate={} userId={}: {}", masterScriptId, duplicateId, userId, e.getMessage());
                }
            }
        }

        log.info("DEDUP_RULE execution complete: {} merges completed", count);
        return count;
    }

    @Override
    public EvolutionRule getRule(String ruleType) {
        return ruleRepository.findByRuleTypeAndDeleted(ruleType, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND,
                        "Evolution rule not found: " + ruleType));
    }

    @Override
    public EvolutionRule updateRuleConfig(String ruleType, String config) {
        log.info("Updating configuration for rule: {}", ruleType);

        EvolutionRule rule = getRule(ruleType);
        rule.setConfig(config);
        rule.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return ruleRepository.save(rule);
    }

    @Override
    public void setRuleEnabled(String ruleType, boolean enabled) {
        log.info("Setting rule {} enabled={}", ruleType, enabled);

        EvolutionRule rule = getRule(ruleType);
        rule.setIsEnabled(enabled ? 1 : 0);
        rule.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        ruleRepository.save(rule);
    }

    @Override
    public Map<String, Object> triggerAllRules(Long userId) {
        log.info("Triggering all evolution rules for user {}", userId);

        Map<String, Object> results = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Evaluate all rules
            List<Long> inclusion = evaluateInclusionRule(userId);
            Map<Long, Map<String, Object>> updates = evaluateUpdateRule(userId);
            List<Long> archival = evaluateArchivalRule(userId);
            List<Map<String, Object>> dedups = evaluateDedupRule(userId);

            // Execute rules
            Integer includeCount = executeInclusionRule(userId, inclusion);
            Integer updateCount = executeUpdateRule(userId, updates);
            Integer archiveCount = executeArchivalRule(userId, archival);
            Integer dedupCount = executeDedupRule(userId, dedups);

            results.put("status", "SUCCESS");
            results.put("included", includeCount);
            results.put("updated", updateCount);
            results.put("archived", archiveCount);
            results.put("merged", dedupCount);
            results.put("executionTimeMs", System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Error triggering all rules for user {}", userId, e);
            results.put("status", "FAILED");
            results.put("error", e.getMessage());
        }

        return results;
    }

    // ─── Private Helper Methods ───

    private EvolutionRule getEnabledRule(String ruleType) {
        return ruleRepository.findByRuleTypeAndIsEnabledAndDeleted(ruleType, 1, 0).orElse(null);
    }

    /**
     * 解析进化规则涉及的 {@link LiveScriptVersion}：优先按版本主键 id；若无行再按话术主键 {@link LiveScriptVersion#getScriptId()} 取该用户下最新未删版本。
     * 用于兼容 {@code ai_knowledge_quality_score.script_version_id} 等字段历史上误存为 {@code live_script.id} 的数据。
     */
    private Optional<LiveScriptVersion> resolveOwnedVersionFlexible(Long userId, Long candidateId) {
        if (candidateId == null || liveScriptVersionRepository == null) {
            return Optional.empty();
        }
        Optional<LiveScriptVersion> byPk = liveScriptVersionRepository.findById(candidateId);
        if (byPk.isPresent()) {
            LiveScriptVersion v = byPk.get();
            if (!Objects.equals(v.getOwnerId(), userId)) {
                return Optional.empty();
            }
            return Optional.of(v);
        }
        List<LiveScriptVersion> byScript = liveScriptVersionRepository.findByScriptIdAndDeletedOrderByVersionNoDesc(candidateId, 0);
        if (byScript == null || byScript.isEmpty()) {
            return Optional.empty();
        }
        return byScript.stream()
                .filter(v -> Objects.equals(v.getOwnerId(), userId))
                .findFirst();
    }

    private BigDecimal latestQualityScore(Long scriptVersionId) {
        return qualityScoreRepository.findFirstByScriptVersionIdAndDeletedOrderByPeriodEndDesc(scriptVersionId, 0)
                .map(KnowledgeQualityScore::getQualityScore)
                .orElse(null);
    }

    private BigDecimal extractBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return null;
    }

    private String versionSnapshot(LiveScriptVersion version) {
        if (version == null) {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", version.getId());
        snapshot.put("versionStatus", version.getVersionStatus());
        snapshot.put("isRecommended", version.getIsRecommended());
        snapshot.put("recommendReason", version.getRecommendReason());
        snapshot.put("recommendScore", version.getRecommendScore());
        return JSON.toJSONString(snapshot);
    }

    private Map<Long, KnowledgeQualityScore> latestScoresByScript(List<KnowledgeQualityScore> scores) {
        Map<Long, KnowledgeQualityScore> latestByScript = new LinkedHashMap<>();
        for (KnowledgeQualityScore score : scores) {
            latestByScript.putIfAbsent(score.getScriptVersionId(), score);
        }
        return latestByScript;
    }
}
