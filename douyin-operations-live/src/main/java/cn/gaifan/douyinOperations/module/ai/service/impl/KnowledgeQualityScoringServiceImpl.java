package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiLiveReview;
import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeEvolutionLog;
import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeQualityScore;
import cn.gaifan.douyinOperations.module.ai.repository.AiLiveReviewRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeEvolutionLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeQualityScoreRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeQualityScoringService;
import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptVersion;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Knowledge Quality Scoring Service Implementation
 * Calculates and tracks quality scores for scripts
 */
@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class KnowledgeQualityScoringServiceImpl implements KnowledgeQualityScoringService {

    @Resource
    private KnowledgeQualityScoreRepository qualityScoreRepository;

    @Resource
    private KnowledgeEvolutionLogRepository evolutionLogRepository;

    @jakarta.annotation.Resource
    private LiveScriptVersionRepository liveScriptVersionRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private LiveMonitorRepository liveMonitorRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AiLiveReviewRepository aiLiveReviewRepository;

    private static final BigDecimal TREND_THRESHOLD = new BigDecimal("5.0");
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    public KnowledgeQualityScore calculateAndSaveQualityScore(Long userId, Long scriptVersionId,
            LocalDate periodStart, LocalDate periodEnd) {
        log.debug("Calculating quality score for script {} in period {}-{}", scriptVersionId, periodStart, periodEnd);

        // Check if score already exists for this period
        Optional<KnowledgeQualityScore> existing = qualityScoreRepository.findByScriptVersionIdAndPeriodStartAndPeriodEndAndDeleted(
                scriptVersionId, Date.valueOf(periodStart), Date.valueOf(periodEnd), 0);
        if (existing == null) {
            existing = Optional.empty();
        }
        if (existing.isPresent()) {
            log.debug("Quality score already exists for script {} in period", scriptVersionId);
            return existing.get();
        }

        BigDecimal qualityScore = calculateQualityMetric(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal effectivenessScore = calculateEffectivenessMetric(userId, scriptVersionId, periodStart, periodEnd);
        Integer usageCount = getUsageCount(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal adoptionRate = calculateAdoptionRate(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal engagementRate = calculateEngagementRate(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal conversionRate = calculateConversionRate(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal avgSentimentScore = calculateAverageSentiment(userId, scriptVersionId, periodStart, periodEnd);

        // Determine trend
        Optional<KnowledgeQualityScore> previousScore = qualityScoreRepository
                .findFirstByScriptVersionIdAndDeletedOrderByPeriodEndDesc(scriptVersionId, 0);
        if (previousScore == null) {
            previousScore = Optional.empty();
        }
        String trend = previousScore.isPresent()
                ? determineTrend(qualityScore, previousScore.get().getQualityScore())
                : "STABLE";

        // Update consecutive low score counter
        Integer consecutiveLowScores = updateConsecutiveLowScores(userId, scriptVersionId, qualityScore, new BigDecimal("40"));

        // Create and save quality score
        KnowledgeQualityScore score = new KnowledgeQualityScore();
        score.setUserId(userId);
        score.setScriptVersionId(scriptVersionId);
        score.setPeriodStart(Date.valueOf(periodStart));
        score.setPeriodEnd(Date.valueOf(periodEnd));
        score.setQualityScore(qualityScore);
        score.setEffectivenessScore(effectivenessScore);
        score.setUsageCount(usageCount);
        score.setAdoptionRate(adoptionRate);
        score.setEngagementRate(engagementRate);
        score.setConversionRate(conversionRate);
        score.setAvgSentimentScore(avgSentimentScore);
        score.setConsecutiveLowScores(consecutiveLowScores);
        score.setTrend(trend);
        score.setNotes("derived_from=live_script_version+live_monitor+ai_live_review");

        KnowledgeQualityScore saved = qualityScoreRepository.save(score);
        log.debug("Quality score calculated and saved: id={}, score={}", saved.getId(), qualityScore);
        return saved;
    }

    @Override
    public BigDecimal calculateLibraryQualityScore(Long userId) {
        BigDecimal avgScore = qualityScoreRepository.calculateAverageQualityScore(userId);
        if (avgScore == null) {
            return BigDecimal.ZERO;
        }
        return avgScore.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<Map<String, Object>> getQualityScoreTrend(Long userId, Long scriptVersionId, int periodCount) {
        List<KnowledgeQualityScore> scores = qualityScoreRepository
                .findByScriptVersionIdAndDeletedOrderByPeriodEndDesc(scriptVersionId, 0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) (Object) scores.stream()
                .limit(periodCount)
                .sorted(Comparator.comparing(KnowledgeQualityScore::getPeriodEnd))
                .map(s -> Map.ofEntries(
                        Map.entry("periodStart", s.getPeriodStart().toString()),
                        Map.entry("periodEnd", s.getPeriodEnd().toString()),
                        Map.entry("qualityScore", s.getQualityScore()),
                        Map.entry("trend", s.getTrend()),
                        Map.entry("usageCount", s.getUsageCount())
                ))
                .collect(Collectors.toList());
        return result;
    }

    @Override
    public List<Long> findScriptsWithConsecutiveLowScores(Long userId, BigDecimal threshold, Integer consecutiveCount) {
        return qualityScoreRepository.findScriptsWithConsecutiveLowScores(userId, consecutiveCount);
    }

    @Override
    public void updateConsecutiveLowScoreCounter(Long userId, Long scriptVersionId, BigDecimal score, BigDecimal lowThreshold) {
        log.debug("Updating consecutive low score counter for script {}", scriptVersionId);

        boolean isLowScore = score.compareTo(lowThreshold) < 0;

        Optional<KnowledgeQualityScore> latest = qualityScoreRepository
                .findFirstByScriptVersionIdAndDeletedOrderByPeriodEndDesc(scriptVersionId, 0);
        if (latest == null) {
            latest = Optional.empty();
        }

        if (isLowScore && latest.isPresent()) {
            int newCount = latest.get().getConsecutiveLowScores() + 1;
            log.debug("Script {} has {} consecutive low scores", scriptVersionId, newCount);
        } else if (!isLowScore) {
            log.debug("Resetting consecutive low score counter for script {}", scriptVersionId);
        }
    }

    @Override
    public Integer batchRecalculateQualityScores(Long userId, LocalDate periodStart, LocalDate periodEnd) {
        log.info("Batch recalculating quality scores for user {} from {} to {}", userId, periodStart, periodEnd);
        List<Long> scriptIds = Optional.ofNullable(liveScriptVersionRepository.findByOwnerIdAndDeleted(userId, 0))
                .orElse(List.of())
                .stream()
                .map(LiveScriptVersion::getId)
                .distinct()
                .toList();
        if (scriptIds.isEmpty()) {
            scriptIds = Optional.ofNullable(qualityScoreRepository.findDistinctScriptVersionIdsByUserId(userId))
                    .orElse(List.of());
        }
        int count = 0;
        for (Long scriptId : scriptIds) {
            calculateAndSaveQualityScore(userId, scriptId, periodStart, periodEnd);
            count++;
        }
        return count;
    }

    @Override
    public List<KnowledgeQualityScore> getQualityScoreHistory(Long userId, Long scriptVersionId,
            LocalDate periodStart, LocalDate periodEnd) {
        log.debug("Getting quality score history for user {}, script {}, period {}-{}",
                userId, scriptVersionId, periodStart, periodEnd);

        if (scriptVersionId != null) {
            List<KnowledgeQualityScore> history = qualityScoreRepository.findByScriptVersionIdAndDeletedOrderByPeriodEndDesc(
                    scriptVersionId, 0);
            return history != null ? history : List.of();
        } else {
            List<KnowledgeQualityScore> history = qualityScoreRepository.findByUserIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualAndDeletedOrderByPeriodEndDesc(
                    userId, Date.valueOf(periodStart), Date.valueOf(periodEnd), 0);
            return history != null ? history : List.of();
        }
    }

    @Override
    public String determineTrend(BigDecimal currentScore, BigDecimal previousScore) {
        if (previousScore == null) {
            return "STABLE";
        }

        BigDecimal difference = currentScore.subtract(previousScore);
        if (difference.abs().compareTo(TREND_THRESHOLD) < 0) {
            return "STABLE";
        }
        return difference.compareTo(BigDecimal.ZERO) > 0 ? "UP" : "DOWN";
    }

    // ─── Private Helper Methods ───

    private BigDecimal calculateQualityMetric(Long userId, Long scriptVersionId,
            LocalDate periodStart, LocalDate periodEnd) {
        BigDecimal effectiveness = calculateEffectivenessMetric(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal adoption = calculateAdoptionRate(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal engagement = calculateEngagementRate(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal conversion = calculateConversionRate(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal sentiment = calculateAverageSentiment(userId, scriptVersionId, periodStart, periodEnd);

        return effectiveness.multiply(new BigDecimal("0.35"))
                .add(adoption.multiply(new BigDecimal("0.20")))
                .add(engagement.multiply(new BigDecimal("0.15")))
                .add(conversion.multiply(new BigDecimal("0.15")))
                .add(sentiment.multiply(new BigDecimal("0.15")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEffectivenessMetric(Long userId, Long scriptVersionId,
            LocalDate periodStart, LocalDate periodEnd) {
        LiveScriptVersion version = findScriptVersion(scriptVersionId);
        if (version != null && version.getEffectivenessScore() != null) {
            return clampScore(version.getEffectivenessScore());
        }
        KnowledgeQualityScore latest = latestScore(scriptVersionId);
        int usageCount = getUsageCount(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal base = latest != null
                ? firstNonNull(latest.getEffectivenessScore(), latest.getQualityScore(), BigDecimal.valueOf(60))
                : BigDecimal.valueOf(55 + Math.min(usageCount * 3, 25));
        BigDecimal activityBoost = BigDecimal.valueOf(Math.min(usageCount * 1.5, 12));
        return clampScore(base.add(activityBoost));
    }

    private Integer getUsageCount(Long userId, Long scriptVersionId, LocalDate periodStart, LocalDate periodEnd) {
        LiveScriptVersion version = findScriptVersion(scriptVersionId);
        if (version != null && version.getUsageCount() != null) {
            Timestamp start = Timestamp.valueOf(periodStart.atStartOfDay());
            if (version.getLastUsedTime() == null || !version.getLastUsedTime().before(start)) {
                return Math.max(0, version.getUsageCount());
            }
            return 0;
        }
        Timestamp start = Timestamp.valueOf(periodStart.atStartOfDay());
        Timestamp end = Timestamp.valueOf(periodEnd.plusDays(1).atStartOfDay());
        long count = evolutionLogRepository.countByUserIdAndScriptVersionIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeleted(
                userId, scriptVersionId, start, end, 0);
        if (count > 0) {
            return Math.toIntExact(Math.min(count, Integer.MAX_VALUE));
        }
        KnowledgeQualityScore latest = latestScore(scriptVersionId);
        return latest != null && latest.getUsageCount() != null ? latest.getUsageCount() : 0;
    }

    private BigDecimal calculateAdoptionRate(Long userId, Long scriptVersionId,
            LocalDate periodStart, LocalDate periodEnd) {
        List<LiveScriptVersion> versions = Optional.ofNullable(
                liveScriptVersionRepository.findByOwnerIdAndDeleted(userId, 0)
        ).orElse(List.of());
        int maxUsage = versions.stream()
                .map(LiveScriptVersion::getUsageCount)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        int usage = getUsageCount(userId, scriptVersionId, periodStart, periodEnd);
        if (maxUsage <= 0) {
            return usage > 0 ? BigDecimal.valueOf(Math.min(100, usage * 25)) : BigDecimal.ZERO;
        }
        return clampScore(BigDecimal.valueOf(usage * 100.0 / maxUsage));
    }

    private BigDecimal calculateEngagementRate(Long userId, Long scriptVersionId,
            LocalDate periodStart, LocalDate periodEnd) {
        SessionMetrics metrics = loadSessionMetrics(scriptVersionId, periodStart, periodEnd);
        if (metrics.audienceBase() > 0) {
            BigDecimal interactions = BigDecimal.valueOf(metrics.likes() + metrics.comments() + metrics.shares() + metrics.newFollowers());
            return clampScore(interactions.multiply(ONE_HUNDRED)
                    .divide(BigDecimal.valueOf(metrics.audienceBase()), 2, RoundingMode.HALF_UP));
        }
        KnowledgeQualityScore latest = latestScore(scriptVersionId);
        return clampScore(latest != null
                ? firstNonNull(latest.getEngagementRate(), latest.getEffectivenessScore(), latest.getQualityScore(), BigDecimal.valueOf(50))
                : BigDecimal.valueOf(50));
    }

    private BigDecimal calculateConversionRate(Long userId, Long scriptVersionId,
            LocalDate periodStart, LocalDate periodEnd) {
        SessionMetrics metrics = loadSessionMetrics(scriptVersionId, periodStart, periodEnd);
        if (metrics.reviewConversionRate() != null) {
            return clampScore(metrics.reviewConversionRate());
        }
        if (metrics.audienceBase() > 0 && metrics.orders() > 0) {
            return clampScore(BigDecimal.valueOf(metrics.orders()).multiply(ONE_HUNDRED)
                    .divide(BigDecimal.valueOf(metrics.audienceBase()), 2, RoundingMode.HALF_UP));
        }
        KnowledgeQualityScore latest = latestScore(scriptVersionId);
        return clampScore(latest != null
                ? firstNonNull(latest.getConversionRate(), percentageOf(latest.getEffectivenessScore(), "0.35"), BigDecimal.valueOf(10))
                : BigDecimal.valueOf(10));
    }

    private BigDecimal calculateAverageSentiment(Long userId, Long scriptVersionId,
            LocalDate periodStart, LocalDate periodEnd) {
        BigDecimal engagement = calculateEngagementRate(userId, scriptVersionId, periodStart, periodEnd);
        BigDecimal conversion = calculateConversionRate(userId, scriptVersionId, periodStart, periodEnd);
        if (engagement.compareTo(BigDecimal.ZERO) > 0 || conversion.compareTo(BigDecimal.ZERO) > 0) {
            return clampScore(BigDecimal.valueOf(40)
                    .add(engagement.multiply(new BigDecimal("0.35")))
                    .add(conversion.multiply(new BigDecimal("0.45"))));
        }
        KnowledgeQualityScore latest = latestScore(scriptVersionId);
        return clampScore(latest != null
                ? firstNonNull(latest.getAvgSentimentScore(), percentageOf(latest.getQualityScore(), "0.9"), BigDecimal.valueOf(60))
                : BigDecimal.valueOf(60));
    }

    private Integer updateConsecutiveLowScores(Long userId, Long scriptVersionId,
            BigDecimal currentScore, BigDecimal lowThreshold) {
        Optional<KnowledgeQualityScore> previous = qualityScoreRepository
                .findFirstByScriptVersionIdAndDeletedOrderByPeriodEndDesc(scriptVersionId, 0);

        if (currentScore.compareTo(lowThreshold) < 0) {
            // Current score is low
            return previous.map(p -> p.getConsecutiveLowScores() + 1).orElse(1);
        } else {
            // Current score is acceptable, reset counter
            return 0;
        }
    }

    private KnowledgeQualityScore latestScore(Long scriptVersionId) {
        Optional<KnowledgeQualityScore> latest = qualityScoreRepository
                .findFirstByScriptVersionIdAndDeletedOrderByPeriodEndDesc(scriptVersionId, 0);
        return latest != null ? latest.orElse(null) : null;
    }

    private BigDecimal clampScore(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        return score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentageOf(BigDecimal base, String ratio) {
        if (base == null) {
            return null;
        }
        return base.multiply(new BigDecimal(ratio));
    }

    private LiveScriptVersion findScriptVersion(Long scriptVersionId) {
        if (scriptVersionId == null) {
            return null;
        }
        return liveScriptVersionRepository.findById(scriptVersionId).orElse(null);
    }

    private SessionMetrics loadSessionMetrics(Long scriptVersionId, LocalDate periodStart, LocalDate periodEnd) {
        LiveScriptVersion version = findScriptVersion(scriptVersionId);
        if (version == null || version.getSessionId() == null) {
            return SessionMetrics.empty();
        }
        long audienceBase = 0;
        long likes = 0;
        long comments = 0;
        long shares = 0;
        long newFollowers = 0;
        long orders = 0;

        if (liveMonitorRepository != null) {
            Timestamp start = Timestamp.valueOf(periodStart.atStartOfDay());
            Timestamp end = Timestamp.valueOf(periodEnd.plusDays(1).atStartOfDay());
            List<LiveMonitor> monitors = liveMonitorRepository.findBySessionIdAndTimestampBetween(version.getSessionId(), start, end);
            for (LiveMonitor monitor : monitors) {
                audienceBase = Math.max(audienceBase, maxPositive(
                        monitor.getTotalViewers(),
                        monitor.getOnlineCount(),
                        monitor.getViewers()));
                likes = Math.max(likes, positiveLong(monitor.getLikes()));
                comments = Math.max(comments, positiveLong(monitor.getComments()));
                shares = Math.max(shares, positiveLong(monitor.getShares()));
                newFollowers = Math.max(newFollowers, positiveLong(monitor.getNewFollowers()));
                orders = Math.max(orders, positiveLong(monitor.getOrders()));
            }
        }

        BigDecimal reviewConversion = null;
        if (aiLiveReviewRepository != null) {
            Optional<AiLiveReview> review = aiLiveReviewRepository.findBySessionIdAndDeleted(version.getSessionId(), 0);
            if (review.isPresent()) {
                AiLiveReview liveReview = review.get();
                audienceBase = Math.max(audienceBase, positiveLong(liveReview.getTotalViewers()));
                reviewConversion = normalizeRateAsPercentage(liveReview.getConversionRate());
            }
        }

        return new SessionMetrics(audienceBase, likes, comments, shares, newFollowers, orders, reviewConversion);
    }

    private long maxPositive(Number... values) {
        long max = 0;
        if (values == null) {
            return max;
        }
        for (Number value : values) {
            max = Math.max(max, positiveLong(value));
        }
        return max;
    }

    private long positiveLong(Number value) {
        return value == null ? 0 : Math.max(0, value.longValue());
    }

    private BigDecimal normalizeRateAsPercentage(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal normalized = value.compareTo(BigDecimal.ONE) <= 0
                ? value.multiply(ONE_HUNDRED)
                : value;
        return clampScore(normalized);
    }

    private record SessionMetrics(long audienceBase, long likes, long comments, long shares,
                                  long newFollowers, long orders, BigDecimal reviewConversionRate) {
        private static SessionMetrics empty() {
            return new SessionMetrics(0, 0, 0, 0, 0, 0, null);
        }
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
