package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionRuleEngineService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeEvolutionService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeQualityScoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Knowledge Library Evolution Scheduled Tasks
 * Automatically executes evolution rules on configured schedules
 */
@Slf4j
@Component
public class KnowledgeEvolutionScheduler {

    @Resource
    private EvolutionRuleEngineService ruleEngineService;
    @Resource
    private KnowledgeEvolutionService evolutionService;
    @Resource
    private KnowledgeQualityScoringService qualityScoringService;
    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Value("${app.ai.knowledge-evolution.enabled:true}")
    private boolean knowledgeEvolutionEnabled;

    @Value("${app.ai.knowledge-evolution.max-users-per-run:200}")
    private int maxUsersPerRun;

    @Value("${app.ai.knowledge-evolution.daily-period-days:30}")
    private int dailyPeriodDays;

    @Value("${app.ai.knowledge-evolution.quality-period-days:30}")
    private int qualityPeriodDays;

    /**
     * Daily evolution task (00:00 UTC)
     * - Evaluate all rules
     * - Execute auto-import, auto-archive, deduplication
     * - Recalculate quality scores
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void dailyEvolutionTask() {
        if (!knowledgeEvolutionEnabled) {
            log.debug("Knowledge evolution scheduler disabled, skipping daily evolution task");
            return;
        }
        log.info("Starting daily evolution task");
        long startTime = System.currentTimeMillis();

        try {
            List<Long> userIds = resolveTargetUserIds();
            int processed = processUsers("daily-evolution", userIds, userId -> {
                var summary = ruleEngineService.triggerAllRules(userId);
                Integer recalculated = evolutionService.recalculateQualityScores(userId, dailyPeriodDays);
                log.info("Daily evolution processed userId={}, summary={}, recalculated={}",
                        userId, summary, recalculated);
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("Daily evolution task completed in {}ms, processedUsers={}", duration, processed);
        } catch (Exception e) {
            log.error("Error in daily evolution task", e);
        }
    }

    /**
     * Weekly report generation task (Monday 09:00 UTC)
     * - Generate weekly evolution reports for all users
     * - Identify trends and patterns
     * - Create recommendations
     */
    @Scheduled(cron = "0 0 9 ? * MON", zone = "UTC")
    public void weeklyReportGenerationTask() {
        if (!knowledgeEvolutionEnabled) {
            log.debug("Knowledge evolution scheduler disabled, skipping weekly report generation task");
            return;
        }
        log.info("Starting weekly report generation task");
        long startTime = System.currentTimeMillis();

        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusWeeks(1);
            List<Long> userIds = resolveTargetUserIds();
            int processed = processUsers("weekly-report", userIds, userId -> {
                evolutionService.generateEvolutionReport(userId, "WEEKLY", startDate, endDate);
                log.info("Weekly evolution report generated for userId={}, period={}~{}",
                        userId, startDate, endDate);
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("Weekly report generation task completed in {}ms, processedUsers={}", duration, processed);
        } catch (Exception e) {
            log.error("Error in weekly report generation task", e);
        }
    }

    /**
     * Monthly report generation task (1st of month 09:00 UTC)
     * - Generate monthly evolution reports
     * - Analyze monthly trends and performance
     * - Archive old data if needed
     */
    @Scheduled(cron = "0 0 9 1 * *", zone = "UTC")
    public void monthlyReportGenerationTask() {
        if (!knowledgeEvolutionEnabled) {
            log.debug("Knowledge evolution scheduler disabled, skipping monthly report generation task");
            return;
        }
        log.info("Starting monthly report generation task");
        long startTime = System.currentTimeMillis();

        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusMonths(1);
            List<Long> userIds = resolveTargetUserIds();
            int processed = processUsers("monthly-report", userIds, userId -> {
                evolutionService.generateEvolutionReport(userId, "MONTHLY", startDate, endDate);
                log.info("Monthly evolution report generated for userId={}, period={}~{}",
                        userId, startDate, endDate);
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("Monthly report generation task completed in {}ms, processedUsers={}", duration, processed);
        } catch (Exception e) {
            log.error("Error in monthly report generation task", e);
        }
    }

    /**
     * Quality score recalculation task (06:00 UTC daily)
     * - Recalculate quality scores from latest monitoring data
     * - Update consecutive low score counters
     * - Identify new candidates for archival
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "UTC")
    public void qualityScoreRecalculationTask() {
        if (!knowledgeEvolutionEnabled) {
            log.debug("Knowledge evolution scheduler disabled, skipping quality score recalculation task");
            return;
        }
        log.info("Starting quality score recalculation task");
        long startTime = System.currentTimeMillis();

        try {
            List<Long> userIds = resolveTargetUserIds();
            int processed = processUsers("quality-recalc", userIds, userId -> {
                Integer recalculated = evolutionService.recalculateQualityScores(userId, qualityPeriodDays);
                log.info("Quality score recalculation processed userId={}, recalculated={}", userId, recalculated);
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("Quality score recalculation task completed in {}ms, processedUsers={}", duration, processed);
        } catch (Exception e) {
            log.error("Error in quality score recalculation task", e);
        }
    }

    /**
     * Low-performance knowledge archival task (12:00 UTC daily)
     * - Find scripts with consistent low performance
     * - Auto-archive if configured
     * - Send notifications to users
     */
    @Scheduled(cron = "0 0 12 * * *", zone = "UTC")
    public void lowPerformanceArchivalTask() {
        if (!knowledgeEvolutionEnabled) {
            log.debug("Knowledge evolution scheduler disabled, skipping low-performance archival task");
            return;
        }
        log.info("Starting low-performance archival task");
        long startTime = System.currentTimeMillis();

        try {
            List<Long> userIds = resolveTargetUserIds();
            int processed = processUsers("low-performance-archival", userIds, userId -> {
                List<Long> archivalCandidates = ruleEngineService.evaluateArchivalRule(userId);
                if (archivalCandidates.isEmpty()) {
                    log.debug("No low-performance archival candidates for userId={}", userId);
                    return;
                }
                Integer archived = ruleEngineService.executeArchivalRule(userId, archivalCandidates);
                log.info("Low-performance archival processed userId={}, candidates={}, archived={}",
                        userId, archivalCandidates.size(), archived);
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("Low-performance archival task completed in {}ms, processedUsers={}", duration, processed);
        } catch (Exception e) {
            log.error("Error in low-performance archival task", e);
        }
    }

    // ─── Private Helper Methods ───

    private List<Long> resolveTargetUserIds() {
        List<Long> userIds = knowledgeBaseRepository.findDistinctUserIds();
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        int limit = Math.max(1, maxUsersPerRun);
        List<Long> normalized = new ArrayList<>();
        for (Long userId : userIds) {
            if (userId == null || userId <= 0 || normalized.contains(userId)) {
                continue;
            }
            normalized.add(userId);
            if (normalized.size() >= limit) {
                break;
            }
        }
        return normalized;
    }

    private int processUsers(String taskName, List<Long> userIds, Consumer<Long> action) {
        if (userIds == null || userIds.isEmpty()) {
            log.debug("No target users for scheduler task {}", taskName);
            return 0;
        }
        int processed = 0;
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            try {
                action.accept(userId);
                BigDecimalOrNull qualityScore = calculateLibraryQualityScoreSafely(userId);
                if (qualityScore.value() != null) {
                    log.debug("Scheduler task {} finished userId={}, libraryQuality={}",
                            taskName, userId, qualityScore.value());
                }
                processed++;
            } catch (Exception e) {
                log.warn("Scheduler task {} failed for userId={}: {}", taskName, userId, e.getMessage());
            }
        }
        return processed;
    }

    private BigDecimalOrNull calculateLibraryQualityScoreSafely(Long userId) {
        try {
            return new BigDecimalOrNull(qualityScoringService.calculateLibraryQualityScore(userId));
        } catch (Exception e) {
            log.debug("Failed to calculate library quality score for userId={}: {}", userId, e.getMessage());
            return new BigDecimalOrNull(null);
        }
    }

    private record BigDecimalOrNull(java.math.BigDecimal value) {
    }
}
