package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.*;
import cn.gaifan.douyinOperations.module.ai.repository.*;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeEvolutionService;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionRuleEngineService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeQualityScoringService;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionOpportunityVO;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionReportVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptVersion;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptVersionRepository;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Knowledge Evolution Service Implementation
 * Core implementation for evolution analysis and execution
 */
@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class KnowledgeEvolutionServiceImpl implements KnowledgeEvolutionService {

    @Resource
    private KnowledgeEvolutionLogRepository evolutionLogRepository;
    @Resource
    private KnowledgeQualityScoreRepository qualityScoreRepository;
    @Resource
    private KnowledgeDeduplicationGroupRepository dedupGroupRepository;
    @Resource
    private EvolutionRuleRepository ruleRepository;
    @Autowired(required = false)
    private LiveScriptVersionRepository liveScriptVersionRepository;
    @Autowired(required = false)
    private EvolutionRuleEngineService ruleEngineService;
    @Autowired(required = false)
    private KnowledgeQualityScoringService qualityScoringService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    @Override
    public EvolutionOpportunityVO analyzeEvolutionOpportunities(Long userId, int periodDays) {
        log.info("Analyzing evolution opportunities for user {} over {} days", userId, periodDays);
        if (ruleEngineService == null) {
            log.warn("EvolutionRuleEngineService 未注入，返回空分析结果");
            EvolutionOpportunityVO empty = new EvolutionOpportunityVO();
            empty.setAnalysisId("evol_" + System.currentTimeMillis());
            empty.setPeriodStart(LocalDate.now().minusDays(periodDays).format(DATE_FORMATTER));
            empty.setPeriodEnd(LocalDate.now().format(DATE_FORMATTER));
            empty.setReadyForInclusion(List.of());
            empty.setNeedsOptimization(List.of());
            empty.setDuplicatesDetected(List.of());
            empty.setReadyForArchival(List.of());
            empty.setExpectedImpact(new EvolutionOpportunityVO.ExpectedImpact(0, 0, BigDecimal.ZERO));
            empty.setCreatedAt(LocalDate.now().format(DATE_FORMATTER));
            empty.setDegraded(Boolean.TRUE);
            return empty;
        }

        LocalDate now = LocalDate.now();
        LocalDate periodStart = now.minusDays(periodDays);

        // Evaluate all rules
        List<Long> readyForInclusion = ruleEngineService.evaluateInclusionRule(userId);
        Map<Long, Map<String, Object>> updateOpportunities = ruleEngineService.evaluateUpdateRule(userId);
        List<Long> readyForArchival = ruleEngineService.evaluateArchivalRule(userId);
        List<Map<String, Object>> duplicates = ruleEngineService.evaluateDedupRule(userId);

        // Build response VOs
        EvolutionOpportunityVO result = new EvolutionOpportunityVO();
        result.setAnalysisId("evol_" + System.currentTimeMillis());
        result.setPeriodStart(periodStart.format(DATE_FORMATTER));
        result.setPeriodEnd(now.format(DATE_FORMATTER));

        // Build inclusion opportunities
        result.setReadyForInclusion(buildInclusionOpportunities(userId, readyForInclusion));

        // Build optimization opportunities
        result.setNeedsOptimization(buildOptimizationOpportunities(userId, updateOpportunities));

        // Build duplicate groups
        result.setDuplicatesDetected(buildDuplicateGroups(duplicates));

        // Build archival opportunities
        result.setReadyForArchival(buildArchivalOpportunities(userId, readyForArchival));

        // Calculate expected impact
        result.setExpectedImpact(calculateExpectedImpact(userId, result));
        result.setCreatedAt(now.format(DATE_FORMATTER));
        result.setDegraded(Boolean.FALSE);

        log.info("Analysis complete: {} inclusions, {} optimizations, {} duplicates, {} archival",
                result.getReadyForInclusion().size(),
                result.getNeedsOptimization().size(),
                result.getDuplicatesDetected().size(),
                result.getReadyForArchival().size());

        return result;
    }

    @Override
    public Map<String, Object> executeAutoOptimization(Long userId, String analysisId,
            boolean autoInclude, boolean autoMerge, boolean autoArchive) {
        log.info("Executing auto-optimization for user {}: include={}, merge={}, archive={}",
                userId, autoInclude, autoMerge, autoArchive);

        Map<String, Object> results = new HashMap<>();
        results.put("executionId", "exec_" + System.currentTimeMillis());
        if (ruleEngineService == null) {
            log.warn("EvolutionRuleEngineService 未注入，跳过自动优化");
            results.put("status", "DEGRADED");
            results.put("degraded", true);
            results.put("results", Map.of());
            results.put("summary", Map.of(
                    "totalProcessed", 0,
                    "qualityImprovement", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "estimatedUserBenefit", "规则引擎未启用，本次未执行任何优化动作"
            ));
            results.put("executedAt", LocalDate.now().format(DATE_FORMATTER));
            return results;
        }

        results.put("status", "COMPLETED");
        results.put("degraded", false);

        Map<String, Map<String, Object>> execResults = new HashMap<>();

        if (autoInclude) {
            List<Long> readyForInclusion = ruleEngineService.evaluateInclusionRule(userId);
            Integer includeCount = ruleEngineService.executeInclusionRule(userId, readyForInclusion);
            execResults.put("included", Map.of("count", includeCount, "scriptIds", readyForInclusion));
        }

        if (autoMerge) {
            List<Map<String, Object>> duplicates = ruleEngineService.evaluateDedupRule(userId);
            Integer mergeCount = ruleEngineService.executeDedupRule(userId, duplicates);
            execResults.put("merged", Map.of("count", mergeCount));
        }

        if (autoArchive) {
            List<Long> readyForArchival = ruleEngineService.evaluateArchivalRule(userId);
            Integer archiveCount = ruleEngineService.executeArchivalRule(userId, readyForArchival);
            execResults.put("archived", Map.of("count", archiveCount, "scriptIds", readyForArchival));
        }

        results.put("results", execResults);
        results.put("summary", Map.of(
                "totalProcessed", execResults.values().stream()
                        .mapToInt(m -> (Integer) m.get("count"))
                        .sum(),
                "qualityImprovement", calculateQualityImprovement(userId),
                "estimatedUserBenefit", buildExecutionBenefitSummary(execResults)
        ));
        results.put("executedAt", LocalDate.now().format(DATE_FORMATTER));

        return results;
    }

    @Override
    public EvolutionReportVO generateEvolutionReport(Long userId, String reportType,
            LocalDate startDate, LocalDate endDate) {
        log.info("Generating {} report for user {} from {} to {}", reportType, userId, startDate, endDate);

        EvolutionReportVO report = new EvolutionReportVO();
        report.setReportId("report_" + System.currentTimeMillis());
        report.setPeriod(startDate.format(DATE_FORMATTER) + " ~ " + endDate.format(DATE_FORMATTER));

        // Build overview
        EvolutionReportVO.Overview overview = buildReportOverview(userId, startDate, endDate);
        report.setOverview(overview);

        // Top scripts
        report.setTopScripts(buildTopScripts(userId, 10));

        // 当前阶段无可靠 style 事实源，不返回伪分析
        report.setStyleAnalysis(Map.of());

        // Recommendations
        report.setRecommendations(buildRecommendations(userId));

        report.setGeneratedAt(LocalDate.now().format(DATE_FORMATTER));

        return report;
    }

    @Override
    public Map<String, Object> deduplicateKnowledge(Long userId, BigDecimal similarityThreshold) {
        log.info("Deduplicating knowledge for user {} with threshold {}", userId, similarityThreshold);

        List<Map<String, Object>> duplicates = ruleEngineService.evaluateDedupRule(userId);
        Integer mergeCount = ruleEngineService.executeDedupRule(userId, duplicates);

        return Map.of(
                "mergeCount", mergeCount,
                "duplicatesFound", duplicates.size(),
                "timestamp", LocalDate.now().format(DATE_FORMATTER)
        );
    }

    @Override
    public PageResultVO<Map<String, Object>> getEvolutionHistory(Long userId, Long scriptVersionId, int page, int pageSize) {
        Specification<KnowledgeEvolutionLog> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("scriptVersionId"), scriptVersionId));
            predicates.add(cb.equal(root.get("deleted"), 0));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<KnowledgeEvolutionLog> p = evolutionLogRepository.findAll(spec, pageable);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) (Object) p.getContent().stream().map(log -> Map.ofEntries(
                Map.entry("id", log.getId()),
                Map.entry("action", log.getAction()),
                Map.entry("ruleType", log.getRuleType()),
                Map.entry("reason", log.getReason()),
                Map.entry("executedBy", log.getExecutedBy()),
                Map.entry("status", log.getStatus()),
                Map.entry("createdAt", log.getCreatedAt())
        )).toList();

        return PageResultVO.of(p.getTotalElements(), content, page, pageSize);
    }

    @Override
    public Integer recalculateQualityScores(Long userId, int periodDays) {
        log.info("Recalculating quality scores for user {} over {} days", userId, periodDays);
        if (qualityScoringService == null) {
            log.warn("KnowledgeQualityScoringService 未注入，跳过");
            return 0;
        }

        LocalDate now = LocalDate.now();
        LocalDate periodStart = now.minusDays(periodDays);

        return qualityScoringService.batchRecalculateQualityScores(userId, periodStart, now);
    }

    // ─── Private Helper Methods ───

    private List<EvolutionOpportunityVO.ScriptOpportunity> buildInclusionOpportunities(
            Long userId, List<Long> scriptIds) {
        Map<Long, KnowledgeQualityScore> latest = latestScoreMap(userId);
        return scriptIds.stream().map(id -> {
            EvolutionOpportunityVO.ScriptOpportunity opp = new EvolutionOpportunityVO.ScriptOpportunity();
            opp.setScriptVersionId(id);
            opp.setTitle(resolveScriptTitle(id));
            KnowledgeQualityScore score = latest.get(id);
            opp.setScore(firstNonNull(score != null ? score.getQualityScore() : null, resolveVersionEffectiveness(id), BigDecimal.ZERO));
            opp.setUsageCount(resolveUsageCount(id, score));
            opp.setReason("Meets inclusion threshold based on latest quality score and usage");
            return opp;
        }).collect(Collectors.toList());
    }

    private List<EvolutionOpportunityVO.OptimizationOpportunity> buildOptimizationOpportunities(
            Long userId, Map<Long, Map<String, Object>> updates) {
        Map<Long, KnowledgeQualityScore> latest = latestScoreMap(userId);
        return updates.entrySet().stream().map(e -> {
            EvolutionOpportunityVO.OptimizationOpportunity opp = new EvolutionOpportunityVO.OptimizationOpportunity();
            opp.setScriptVersionId(e.getKey());
            opp.setTitle(resolveScriptTitle(e.getKey()));
            KnowledgeQualityScore score = latest.get(e.getKey());
            opp.setScore(extractBigDecimal(e.getValue().get("newScore"),
                    firstNonNull(score != null ? score.getQualityScore() : null, resolveVersionEffectiveness(e.getKey()), BigDecimal.ZERO)));
            opp.setConsecutiveLowScore(score != null && score.getConsecutiveLowScores() != null ? score.getConsecutiveLowScores() : 0);
            opp.setSuggestion(String.valueOf(e.getValue().getOrDefault("reason", "Newer quality signal suggests refreshing this script")));
            Object ref = e.getValue().get("referenceScriptId");
            if (ref instanceof Number n) {
                opp.setReferenceScriptId(n.longValue());
            }
            return opp;
        }).collect(Collectors.toList());
    }

    private List<EvolutionOpportunityVO.DuplicateGroup> buildDuplicateGroups(List<Map<String, Object>> duplicates) {
        Map<Long, KnowledgeQualityScore> latest = latestScoreMapFromDuplicates(duplicates);
        return duplicates.stream().map(d -> {
            EvolutionOpportunityVO.DuplicateGroup group = new EvolutionOpportunityVO.DuplicateGroup();
            group.setMasterScriptId((Long) d.get("masterScriptId"));
            group.setMasterTitle(resolveScriptTitle(group.getMasterScriptId()));
            KnowledgeQualityScore score = latest.get(group.getMasterScriptId());
            group.setMasterScore(firstNonNull(score != null ? score.getQualityScore() : null,
                    resolveVersionEffectiveness(group.getMasterScriptId()), BigDecimal.ZERO));
            @SuppressWarnings("unchecked")
            List<Long> duplicateIds = (List<Long>) d.getOrDefault("duplicateIds", new ArrayList<>());
            group.setDuplicateScriptIds(duplicateIds);
            group.setSimilarityScore(extractBigDecimal(d.get("similarity"), BigDecimal.ZERO));
            group.setRecommendation("Merge or archive duplicate variants based on latest similarity evidence");
            return group;
        }).collect(Collectors.toList());
    }

    private List<EvolutionOpportunityVO.ArchivalOpportunity> buildArchivalOpportunities(
            Long userId, List<Long> scriptIds) {
        Map<Long, KnowledgeQualityScore> latest = latestScoreMap(userId);
        return scriptIds.stream().map(id -> {
            EvolutionOpportunityVO.ArchivalOpportunity arch = new EvolutionOpportunityVO.ArchivalOpportunity();
            arch.setScriptVersionId(id);
            arch.setTitle(resolveScriptTitle(id));
            KnowledgeQualityScore score = latest.get(id);
            arch.setCurrentScore(firstNonNull(score != null ? score.getQualityScore() : null,
                    resolveVersionEffectiveness(id), BigDecimal.ZERO));
            arch.setReason("Consecutive low-score periods reached archival threshold");
            arch.setMonthsSinceDeprecation(score != null && score.getPeriodEnd() != null
                    ? Math.max(0, (int) java.time.temporal.ChronoUnit.MONTHS.between(
                    score.getPeriodEnd().toLocalDate().withDayOfMonth(1),
                    LocalDate.now().withDayOfMonth(1)))
                    : 0);
            return arch;
        }).collect(Collectors.toList());
    }

    private EvolutionOpportunityVO.ExpectedImpact calculateExpectedImpact(Long userId, EvolutionOpportunityVO analysis) {
        int actionableUnique = new HashSet<Long>() {{
            analysis.getReadyForInclusion().forEach(i -> add(i.getScriptVersionId()));
            analysis.getNeedsOptimization().forEach(i -> add(i.getScriptVersionId()));
            analysis.getReadyForArchival().forEach(i -> add(i.getScriptVersionId()));
            analysis.getDuplicatesDetected().forEach(i -> {
                add(i.getMasterScriptId());
                addAll(i.getDuplicateScriptIds());
            });
        }}.size();
        int totalScripts = Math.max(1, totalScriptCount(userId));
        BigDecimal improvementRate = BigDecimal.valueOf(actionableUnique * 100.0 / totalScripts)
                .setScale(2, RoundingMode.HALF_UP);
        return new EvolutionOpportunityVO.ExpectedImpact(
                analysis.getReadyForInclusion().size(),
                analysis.getDuplicatesDetected().size(),
                improvementRate
        );
    }

    private BigDecimal calculateQualityImprovement(Long userId) {
        List<KnowledgeQualityScore> scores = Optional.ofNullable(
                qualityScoreRepository.findByUserIdAndDeletedOrderByPeriodEndDesc(userId, 0)
        ).orElse(List.of());
        if (scores.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Map<LocalDate, List<KnowledgeQualityScore>> byPeriodEnd = scores.stream()
                .filter(score -> score.getPeriodEnd() != null && score.getQualityScore() != null)
                .collect(Collectors.groupingBy(score -> score.getPeriodEnd().toLocalDate(), TreeMap::new, Collectors.toList()));
        List<LocalDate> periods = new ArrayList<>(byPeriodEnd.keySet());
        periods.sort(Comparator.reverseOrder());
        if (periods.size() < 2) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal latestAvg = averageQuality(byPeriodEnd.get(periods.get(0)));
        BigDecimal previousAvg = averageQuality(byPeriodEnd.get(periods.get(1)));
        return latestAvg.subtract(previousAvg).setScale(2, RoundingMode.HALF_UP);
    }

    private EvolutionReportVO.Overview buildReportOverview(Long userId, LocalDate start, LocalDate end) {
        Timestamp startTs = Timestamp.valueOf(start.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(end.plusDays(1).atStartOfDay());
        return new EvolutionReportVO.Overview(
                totalScriptCount(userId),
                Math.toIntExact(evolutionLogRepository.countByUserIdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeleted(
                        userId, "auto_import", startTs, endTs, 0)),
                Math.toIntExact(evolutionLogRepository.countByUserIdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeleted(
                        userId, "auto_archive", startTs, endTs, 0)),
                Math.toIntExact(evolutionLogRepository.countByUserIdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeleted(
                        userId, "merge", startTs, endTs, 0)),
                qualityScoringService.calculateLibraryQualityScore(userId)
        );
    }

    private List<EvolutionReportVO.TopScript> buildTopScripts(Long userId, int limit) {
        List<KnowledgeQualityScore> latestScores = new ArrayList<>(latestScoreMap(userId).values());
        latestScores.sort(Comparator.comparing(KnowledgeQualityScore::getQualityScore,
                Comparator.nullsLast(BigDecimal::compareTo)).reversed());
        List<EvolutionReportVO.TopScript> scripts = new ArrayList<>();
        int rank = 1;
        for (KnowledgeQualityScore score : latestScores.stream().limit(limit).toList()) {
            scripts.add(new EvolutionReportVO.TopScript(
                    rank++,
                    score.getScriptVersionId(),
                    resolveScriptTitle(score.getScriptVersionId()),
                    firstNonNull(score.getQualityScore(), resolveVersionEffectiveness(score.getScriptVersionId()), BigDecimal.ZERO),
                    resolveUsageCount(score.getScriptVersionId(), score),
                    score.getAdoptionRate() != null ? score.getAdoptionRate() : BigDecimal.ZERO
            ));
        }
        return scripts;
    }

    private List<EvolutionReportVO.Recommendation> buildRecommendations(Long userId) {
        List<EvolutionReportVO.Recommendation> recommendations = new ArrayList<>();
        Map<Long, KnowledgeQualityScore> latest = latestScoreMap(userId);
        long lowQualityCount = latest.values().stream()
                .filter(score -> score.getQualityScore() != null && score.getQualityScore().compareTo(BigDecimal.valueOf(40)) < 0)
                .count();
        long decliningCount = latest.values().stream()
                .filter(score -> "DOWN".equalsIgnoreCase(score.getTrend()))
                .count();
        long pendingDedup = dedupGroupRepository.countPendingReview(userId);

        if (lowQualityCount > 0) {
            recommendations.add(new EvolutionReportVO.Recommendation(
                    "QUALITY_ISSUE",
                    lowQualityCount + " 个脚本处于低质量区间，建议优先优化或归档",
                    lowQualityCount >= 3 ? "HIGH" : "MEDIUM"));
        }
        if (decliningCount > 0) {
            recommendations.add(new EvolutionReportVO.Recommendation(
                    "OPTIMIZATION_NEEDED",
                    decliningCount + " 个脚本最新趋势下滑，建议回看近两期评分差异",
                    decliningCount >= 3 ? "HIGH" : "MEDIUM"));
        }
        if (pendingDedup > 0) {
            recommendations.add(new EvolutionReportVO.Recommendation(
                    "DEDUP_OPPORTUNITY",
                    pendingDedup + " 组重复候选待处理，可降低知识库冗余",
                    pendingDedup >= 5 ? "MEDIUM" : "LOW"));
        }
        if (recommendations.isEmpty()) {
            recommendations.add(new EvolutionReportVO.Recommendation(
                    "OPTIMIZATION_NEEDED",
                    "当前库内质量总体稳定，建议继续累积新样本并关注趋势变化",
                    "LOW"));
        }
        return recommendations;
    }

    private Map<Long, KnowledgeQualityScore> latestScoreMap(Long userId) {
        Map<Long, KnowledgeQualityScore> latest = new LinkedHashMap<>();
        List<KnowledgeQualityScore> scores = Optional.ofNullable(
                qualityScoreRepository.findByUserIdAndDeletedOrderByPeriodEndDesc(userId, 0)
        ).orElse(List.of());
        for (KnowledgeQualityScore score : scores) {
            latest.putIfAbsent(score.getScriptVersionId(), score);
        }
        return latest;
    }

    private Map<Long, KnowledgeQualityScore> latestScoreMapFromDuplicates(List<Map<String, Object>> duplicates) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Map<String, Object> duplicate : duplicates) {
            Object master = duplicate.get("masterScriptId");
            if (master instanceof Long l) {
                ids.add(l);
            }
            Object rawDuplicateIds = duplicate.get("duplicateIds");
            if (rawDuplicateIds instanceof List<?> list) {
                for (Object id : list) {
                    if (id instanceof Long l) {
                        ids.add(l);
                    }
                }
            }
        }
        Map<Long, KnowledgeQualityScore> latest = new HashMap<>();
        for (Long id : ids) {
            Optional<KnowledgeQualityScore> score = qualityScoreRepository.findFirstByScriptVersionIdAndDeletedOrderByPeriodEndDesc(id, 0);
            if (score != null) {
                score.ifPresent(s -> latest.put(id, s));
            }
        }
        return latest;
    }

    private BigDecimal extractBigDecimal(Object value, BigDecimal fallback) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return fallback;
    }

    private String buildExecutionBenefitSummary(Map<String, Map<String, Object>> execResults) {
        int totalProcessed = execResults.values().stream()
                .mapToInt(m -> (Integer) m.get("count"))
                .sum();
        if (totalProcessed == 0) {
            return "本次未执行任何优化动作";
        }
        List<String> parts = new ArrayList<>();
        if (execResults.containsKey("included")) parts.add("纳入 " + execResults.get("included").get("count") + " 个脚本");
        if (execResults.containsKey("merged")) parts.add("合并 " + execResults.get("merged").get("count") + " 组重复脚本");
        if (execResults.containsKey("archived")) parts.add("归档 " + execResults.get("archived").get("count") + " 个低质脚本");
        return String.join("，", parts);
    }

    private int totalScriptCount(Long userId) {
        if (liveScriptVersionRepository != null) {
            return Math.toIntExact(liveScriptVersionRepository.countByOwnerIdAndDeleted(userId, 0));
        }
        return latestScoreMap(userId).size();
    }

    private String resolveScriptTitle(Long scriptVersionId) {
        LiveScriptVersion version = resolveVersion(scriptVersionId);
        if (version == null) {
            return "Script #" + scriptVersionId;
        }
        if (version.getVersionLabel() != null && !version.getVersionLabel().isBlank()) {
            return version.getVersionLabel();
        }
        if (version.getScriptContent() != null && !version.getScriptContent().isBlank()) {
            String preview = version.getScriptContent().replaceAll("\\s+", " ").trim();
            return preview.length() > 48 ? preview.substring(0, 48) + "..." : preview;
        }
        return "Script #" + scriptVersionId;
    }

    private BigDecimal resolveVersionEffectiveness(Long scriptVersionId) {
        LiveScriptVersion version = resolveVersion(scriptVersionId);
        if (version == null || version.getEffectivenessScore() == null) {
            return null;
        }
        return version.getEffectivenessScore().setScale(2, RoundingMode.HALF_UP);
    }

    private int resolveUsageCount(Long scriptVersionId, KnowledgeQualityScore score) {
        LiveScriptVersion version = resolveVersion(scriptVersionId);
        if (version != null && version.getUsageCount() != null) {
            return Math.max(0, version.getUsageCount());
        }
        return score != null && score.getUsageCount() != null ? score.getUsageCount() : 0;
    }

    private LiveScriptVersion resolveVersion(Long scriptVersionId) {
        if (liveScriptVersionRepository == null || scriptVersionId == null) {
            return null;
        }
        return liveScriptVersionRepository.findById(scriptVersionId).orElse(null);
    }

    private BigDecimal averageQuality(List<KnowledgeQualityScore> scores) {
        if (scores == null || scores.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = scores.stream()
                .map(KnowledgeQualityScore::getQualityScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = scores.stream().map(KnowledgeQualityScore::getQualityScore).filter(Objects::nonNull).count();
        if (count == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    @SafeVarargs
    private final BigDecimal firstNonNull(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
