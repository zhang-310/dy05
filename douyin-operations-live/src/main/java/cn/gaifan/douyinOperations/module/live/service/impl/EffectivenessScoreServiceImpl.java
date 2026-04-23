package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.*;
import cn.gaifan.douyinOperations.module.live.repository.*;
import org.springframework.data.jpa.domain.Specification;
import cn.gaifan.douyinOperations.module.live.service.EffectivenessScoreService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 直播话术效果评分服务实现
 * W-04: 效果评分系统
 */
@Service("liveEffectivenessScoreServiceImpl")
public class EffectivenessScoreServiceImpl implements EffectivenessScoreService {

    private static final Logger log = LoggerFactory.getLogger(EffectivenessScoreServiceImpl.class);

    @Resource
    private LiveScriptRepository liveScriptRepository;

    @Resource
    private LiveMonitorRepository liveMonitorRepository;

    @Resource
    private LiveScriptEffectivenessRepository effectivenessRepository;

    @Resource
    private LiveSessionRepository liveSessionRepository;

    @Resource
    private LiveSessionDataRepository liveSessionDataRepository;

    @Resource
    private LiveProductDataRepository liveProductDataRepository;

    @Resource
    private LiveEffectivenessConfigRepository liveEffectivenessConfigRepository;

    private static final BigDecimal SCORE_SCALE = new BigDecimal("10"); // 评分满分 10 分
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Override
    public Map<String, Object> calculateScore(Long scriptId, Long sessionId) {
        if (scriptId == null || scriptId <= 0 || sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "脚本ID和场次ID不能为空");
        }

        LiveScript script = liveScriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        LiveSession session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));

        // 获取该话术的实际执行数据
        List<LiveMonitor> monitors = liveMonitorRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        LiveSessionData sessionData = liveSessionDataRepository.findBySessionId(sessionId).orElse(null);
        LiveProductData productData = script.getProductId() != null
                ? liveProductDataRepository.findBySessionIdAndProductId(sessionId, script.getProductId()).orElse(null)
                : null;
        ScoreWeights weights = resolveWeights(script, session);

        if ((monitors == null || monitors.isEmpty()) && sessionData == null && productData == null) {
            return createEmptyScoreResult(scriptId, sessionId);
        }

        long audienceBase = resolveAudienceBase(monitors, sessionData);
        long totalLikes = resolveTotalLikes(monitors, sessionData);
        int totalComments = resolveTotalComments(monitors, sessionData);
        int totalShares = resolveTotalShares(monitors, sessionData);
        int totalOrders = resolveTotalOrders(monitors, sessionData, productData);
        BigDecimal totalRevenue = resolveRevenue(sessionData, productData);

        int scriptInteractions = Math.max(0, script.getInteractionDelta() != null ? script.getInteractionDelta() : 0);
        int scriptConversions = Math.max(0, script.getConversionDelta() != null ? script.getConversionDelta() : 0);
        int viewerDelta = script.getViewerDelta() != null ? script.getViewerDelta() : 0;

        BigDecimal conversionRate = audienceBase > 0
                ? BigDecimal.valueOf(scriptConversions).multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(audienceBase), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal interactionRate = audienceBase > 0
                ? BigDecimal.valueOf(scriptInteractions).multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(audienceBase), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal completionRate = resolveCompletionRate(script, session, sessionData, audienceBase, viewerDelta);
        BigDecimal gmvScore = resolveGmvScore(scriptConversions, totalOrders, totalRevenue, audienceBase);

        BigDecimal weightedTotal = conversionRate.multiply(weights.conversionWeight())
                .add(interactionRate.multiply(weights.interactionWeight()))
                .add(completionRate.multiply(weights.retentionWeight()))
                .add(gmvScore.multiply(weights.gmvWeight()));

        BigDecimal totalScore = weightedTotal
                .divide(SCORE_SCALE, 2, RoundingMode.HALF_UP);

        // 确保在 0-10 范围内
        totalScore = totalScore.min(SCORE_SCALE).max(BigDecimal.ZERO);

        // 保存或更新评分记录
        Optional<LiveScriptEffectiveness> existing = effectivenessRepository
                .findByScriptIdAndDeletedOrderByCalculatedAtDesc(scriptId, 0);

        LiveScriptEffectiveness effectiveness = existing.orElseGet(() -> {
            LiveScriptEffectiveness e = new LiveScriptEffectiveness();
            e.setScriptId(scriptId);
            e.setSessionId(sessionId);
            e.setCreateTime(new Timestamp(System.currentTimeMillis()));
            return e;
        });

        effectiveness.setConversionRate(conversionRate.setScale(2, RoundingMode.HALF_UP));
        effectiveness.setLikes(totalLikes);
        effectiveness.setComments(totalComments);
        effectiveness.setCompletionRate(completionRate);
        effectiveness.setTotalScore(totalScore);
        effectiveness.setSampleSize(script.getExecuted() != null ? script.getExecuted() : Math.max(1, scriptConversions));
        effectiveness.setScoreFormula(String.format(
                "conversion(%.2f)*%.2f + interaction(%.2f)*%.2f + completion(%.2f)*%.2f + gmv(%.2f)*%.2f",
                conversionRate, weights.conversionWeight(),
                interactionRate, weights.interactionWeight(),
                completionRate, weights.retentionWeight(),
                gmvScore, weights.gmvWeight()
        ));
        effectiveness.setCalculatedAt(new Timestamp(System.currentTimeMillis()));

        effectivenessRepository.save(effectiveness);

        return Map.ofEntries(
                Map.entry("scriptId", scriptId),
                Map.entry("sessionId", sessionId),
                Map.entry("totalScore", totalScore.doubleValue()),
                Map.entry("conversionRate", conversionRate.doubleValue()),
                Map.entry("interactionRate", interactionRate.doubleValue()),
                Map.entry("gmvScore", gmvScore.doubleValue()),
                Map.entry("likes", totalLikes),
                Map.entry("comments", totalComments),
                Map.entry("shares", totalShares),
                Map.entry("orders", totalOrders),
                Map.entry("completionRate", completionRate.doubleValue()),
                Map.entry("sampleSize", script.getExecuted() != null ? script.getExecuted() : Math.max(1, scriptConversions))
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> calculateSessionRanking(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "场次ID不能为空");
        }

        // 获取该场次的所有话术
        List<LiveScript> scripts = liveScriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
        if (scripts.isEmpty()) {
            return new ArrayList<>();
        }

        // 逐个计算评分
        List<Map<String, Object>> results = new ArrayList<>();
        for (LiveScript script : scripts) {
            Map<String, Object> scoreResult = calculateScore(script.getId(), sessionId);
            results.add(scoreResult);
        }

        // 按评分降序排序，计算排名
        results.sort((a, b) -> {
            Double scoreA = (Double) a.get("totalScore");
            Double scoreB = (Double) b.get("totalScore");
            return scoreB.compareTo(scoreA);
        });

        // 更新排名和标签
        updateRankingAndTags(sessionId, results);

        return results;
    }

    @Override
    public Map<String, Object> compareVersions(Long versionA, Long versionB) {
        if (versionA == null || versionA <= 0 || versionB == null || versionB <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "版本ID不能为空");
        }

        LiveScriptEffectiveness effA = effectivenessRepository.findById(versionA)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "版本A不存在"));

        LiveScriptEffectiveness effB = effectivenessRepository.findById(versionB)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "版本B不存在"));

        BigDecimal scoreDiff = effA.getTotalScore().subtract(effB.getTotalScore());
        String trend = scoreDiff.compareTo(BigDecimal.ZERO) > 0 ? "A更优" : (scoreDiff.compareTo(BigDecimal.ZERO) < 0 ? "B更优" : "持平");

        return Map.ofEntries(
                Map.entry("versionA", versionA),
                Map.entry("versionB", versionB),
                Map.entry("scoreA", effA.getTotalScore().doubleValue()),
                Map.entry("scoreB", effB.getTotalScore().doubleValue()),
                Map.entry("scoreDiff", scoreDiff.doubleValue()),
                Map.entry("trend", trend),
                Map.entry("conversionRateA", effA.getConversionRate().doubleValue()),
                Map.entry("conversionRateB", effB.getConversionRate().doubleValue()),
                Map.entry("likesA", effA.getLikes()),
                Map.entry("likesB", effB.getLikes()),
                Map.entry("commentsA", effA.getComments()),
                Map.entry("commentsB", effB.getComments()),
                Map.entry("completionRateA", effA.getCompletionRate().doubleValue()),
                Map.entry("completionRateB", effB.getCompletionRate().doubleValue()),
                Map.entry("sampleSizeA", effA.getSampleSize()),
                Map.entry("sampleSizeB", effB.getSampleSize())
        );
    }

    @Override
    public PageResultVO<Map<String, Object>> getRanking(Long sessionId, int page, int pageSize) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "场次ID不能为空");
        }

        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100), Sort.by(Sort.Direction.ASC, "ranking"));
        Page<LiveScriptEffectiveness> pageResult = effectivenessRepository
                .findBySessionIdAndDeletedOrderByRankingAsc(sessionId, 0, pageable);

        List<Map<String, Object>> list = pageResult.getContent().stream()
                .map(this::toRankingMap)
                .collect(Collectors.toList());

        return PageResultVO.of(pageResult.getTotalElements(), list, page, pageSize);
    }

    @Override
    public List<Map<String, Object>> getTopScripts(Long sessionId, int limit) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "场次ID不能为空");
        }

        List<LiveScriptEffectiveness> topScripts = effectivenessRepository
                .findBySessionIdAndDeletedOrderByTotalScoreDesc(sessionId, 0).stream()
                .limit(Math.min(limit, 10))
                .collect(Collectors.toList());

        return topScripts.stream()
                .map(this::toRankingMap)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRecommendedScripts(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "场次ID不能为空");
        }

        List<LiveScriptEffectiveness> recommended = effectivenessRepository
                .findBySessionIdAndScoreRange(sessionId, new BigDecimal("7.0"));

        return recommended.stream()
                .map(this::toRankingMap)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getEmergedScripts(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "场次ID不能为空");
        }

        List<LiveScriptEffectiveness> emerged = effectivenessRepository
                .findBySessionIdAndTagAndDeletedOrderByTotalScoreDesc(sessionId, "emerging", 0);

        return emerged.stream()
                .map(this::toRankingMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getScriptEffectiveness(Long scriptId) {
        if (scriptId == null || scriptId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "脚本ID不能为空");
        }

        LiveScriptEffectiveness eff = effectivenessRepository
                .findByScriptIdAndDeletedOrderByCalculatedAtDesc(scriptId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "评分数据不存在"));

        return toRankingMap(eff);
    }

    @Override
    public void scheduleScoreUpdate() {
        // 获取所有有效的直播场次（数据库层过滤，避免全表扫描）
        Specification<LiveSession> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("deleted"), 0),
                root.get("status").in(Arrays.asList(1, 2))
        );
        List<LiveSession> activeSessions = liveSessionRepository.findAll(spec);

        for (LiveSession session : activeSessions) {
            try {
                calculateSessionRanking(session.getId());
                log.info("更新场次评分: sessionId={}", session.getId());
            } catch (Exception e) {
                log.error("更新场次评分失败: sessionId={}", session.getId(), e);
            }
        }
    }

    // ───────────── 内部方法 ─────────────

    private void updateRankingAndTags(Long sessionId, List<Map<String, Object>> results) {
        int ranking = 1;
        for (Map<String, Object> result : results) {
            Long scriptId = ((Number) result.get("scriptId")).longValue();
            Double score = (Double) result.get("totalScore");
            String tag = getTag(score, ranking);

            LiveScriptEffectiveness eff = effectivenessRepository
                    .findByScriptIdAndDeletedOrderByCalculatedAtDesc(scriptId, 0)
                    .orElse(null);

            if (eff != null) {
                eff.setRanking(ranking);
                eff.setTag(tag);
                effectivenessRepository.save(eff);
            }

            ranking++;
        }
    }

    private String getTag(Double score, int ranking) {
        if (ranking <= 3) return "hot";
        if (score >= 7.0) return "recommend";
        if (ranking <= 5) return "emerging";
        return null;
    }

    private Map<String, Object> toRankingMap(LiveScriptEffectiveness eff) {
        return Map.ofEntries(
                Map.entry("scriptId", eff.getScriptId()),
                Map.entry("sessionId", eff.getSessionId()),
                Map.entry("totalScore", eff.getTotalScore().doubleValue()),
                Map.entry("ranking", eff.getRanking()),
                Map.entry("tag", eff.getTag()),
                Map.entry("conversionRate", eff.getConversionRate().doubleValue()),
                Map.entry("likes", eff.getLikes()),
                Map.entry("comments", eff.getComments()),
                Map.entry("completionRate", eff.getCompletionRate().doubleValue()),
                Map.entry("sampleSize", eff.getSampleSize()),
                Map.entry("calculatedAt", eff.getCalculatedAt() != null ? eff.getCalculatedAt().getTime() : 0)
        );
    }

    private Map<String, Object> createEmptyScoreResult(Long scriptId, Long sessionId) {
        return Map.ofEntries(
                Map.entry("scriptId", scriptId),
                Map.entry("sessionId", sessionId),
                Map.entry("totalScore", 0.0),
                Map.entry("conversionRate", 0.0),
                Map.entry("interactionRate", 0.0),
                Map.entry("gmvScore", 0.0),
                Map.entry("likes", 0L),
                Map.entry("comments", 0),
                Map.entry("shares", 0),
                Map.entry("orders", 0),
                Map.entry("completionRate", 0.0),
                Map.entry("sampleSize", 0)
        );
    }

    private ScoreWeights resolveWeights(LiveScript script, LiveSession session) {
        Long ownerId = script.getUserId() != null ? script.getUserId() : session.getUserId();
        LiveEffectivenessConfig config = null;
        if (ownerId != null) {
            config = liveEffectivenessConfigRepository.findByUserIdAndIsDefaultAndDeleted(ownerId, 1, 0).orElse(null);
        }
        if (config == null) {
            config = liveEffectivenessConfigRepository.findFirstByIsDefaultAndDeleted(1, 0).orElse(null);
        }
        BigDecimal conversion = config != null ? defaultWeight(config.getConversionWeight(), new BigDecimal("0.30")) : new BigDecimal("0.30");
        BigDecimal interaction = config != null ? defaultWeight(config.getInteractionWeight(), new BigDecimal("0.25")) : new BigDecimal("0.25");
        BigDecimal retention = config != null ? defaultWeight(config.getRetentionWeight(), new BigDecimal("0.25")) : new BigDecimal("0.25");
        BigDecimal gmv = config != null ? defaultWeight(config.getGmvWeight(), new BigDecimal("0.20")) : new BigDecimal("0.20");
        BigDecimal sum = conversion.add(interaction).add(retention).add(gmv);
        if (sum.compareTo(BigDecimal.ZERO) <= 0) {
            return new ScoreWeights(new BigDecimal("0.30"), new BigDecimal("0.25"), new BigDecimal("0.25"), new BigDecimal("0.20"));
        }
        return new ScoreWeights(
                conversion.divide(sum, 4, RoundingMode.HALF_UP),
                interaction.divide(sum, 4, RoundingMode.HALF_UP),
                retention.divide(sum, 4, RoundingMode.HALF_UP),
                gmv.divide(sum, 4, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal defaultWeight(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : fallback;
    }

    private long resolveAudienceBase(List<LiveMonitor> monitors, LiveSessionData sessionData) {
        long sessionTotal = sessionData != null && sessionData.getTotalViewers() != null ? sessionData.getTotalViewers() : 0;
        long monitorTotal = monitors.stream()
                .mapToLong(m -> Math.max(
                        m.getTotalViewers() != null ? m.getTotalViewers() : 0,
                        Math.max(m.getOnlineCount() != null ? m.getOnlineCount() : 0,
                                m.getViewers() != null ? m.getViewers() : 0)))
                .max().orElse(0);
        return Math.max(1, Math.max(sessionTotal, monitorTotal));
    }

    private long resolveTotalLikes(List<LiveMonitor> monitors, LiveSessionData sessionData) {
        long sessionLikes = sessionData != null && sessionData.getTotalLikes() != null ? sessionData.getTotalLikes() : 0L;
        long monitorLikes = monitors.stream().mapToLong(m -> m.getLikes() != null ? m.getLikes() : 0L).max().orElse(0L);
        return Math.max(sessionLikes, monitorLikes);
    }

    private int resolveTotalComments(List<LiveMonitor> monitors, LiveSessionData sessionData) {
        int sessionComments = sessionData != null && sessionData.getTotalComments() != null ? sessionData.getTotalComments() : 0;
        int monitorComments = monitors.stream().mapToInt(m -> m.getComments() != null ? m.getComments() : 0).max().orElse(0);
        return Math.max(sessionComments, monitorComments);
    }

    private int resolveTotalShares(List<LiveMonitor> monitors, LiveSessionData sessionData) {
        int sessionShares = sessionData != null && sessionData.getTotalShares() != null ? sessionData.getTotalShares() : 0;
        int monitorShares = monitors.stream().mapToInt(m -> m.getShares() != null ? m.getShares() : 0).max().orElse(0);
        return Math.max(sessionShares, monitorShares);
    }

    private int resolveTotalOrders(List<LiveMonitor> monitors, LiveSessionData sessionData, LiveProductData productData) {
        int productOrders = productData != null && productData.getOrders() != null ? productData.getOrders() : 0;
        if (productData != null && productOrders > 0) {
            return productOrders;
        }
        int sessionOrders = sessionData != null && sessionData.getTotalOrders() != null ? sessionData.getTotalOrders() : 0;
        int monitorOrders = monitors.stream().mapToInt(m -> m.getOrders() != null ? m.getOrders() : 0).max().orElse(0);
        return Math.max(sessionOrders, monitorOrders);
    }

    private BigDecimal resolveRevenue(LiveSessionData sessionData, LiveProductData productData) {
        if (productData != null && productData.getRevenue() != null && productData.getRevenue().compareTo(BigDecimal.ZERO) > 0) {
            return productData.getRevenue();
        }
        return sessionData != null && sessionData.getTotalRevenue() != null ? sessionData.getTotalRevenue() : BigDecimal.ZERO;
    }

    private BigDecimal resolveCompletionRate(LiveScript script, LiveSession session, LiveSessionData sessionData, long audienceBase, int viewerDelta) {
        if (script.getExecutionTime() != null && script.getExecutionTime() > 0 && sessionData != null && sessionData.getAvgStayTime() != null && sessionData.getAvgStayTime() > 0) {
            return BigDecimal.valueOf(sessionData.getAvgStayTime())
                    .divide(BigDecimal.valueOf(script.getExecutionTime()), 4, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED)
                    .min(ONE_HUNDRED)
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (audienceBase > 0 && viewerDelta != 0) {
            BigDecimal base = BigDecimal.valueOf(50)
                    .add(BigDecimal.valueOf(viewerDelta).multiply(ONE_HUNDRED)
                            .divide(BigDecimal.valueOf(audienceBase), 4, RoundingMode.HALF_UP));
            return base.max(BigDecimal.ZERO).min(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
        }
        if (session.getStartTime() != null && session.getEndTime() != null && sessionData != null && sessionData.getAvgStayTime() != null) {
            long sessionDurationSec = Math.max(1, (session.getEndTime().getTime() - session.getStartTime().getTime()) / 1000);
            return BigDecimal.valueOf(sessionData.getAvgStayTime())
                    .divide(BigDecimal.valueOf(sessionDurationSec), 4, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED)
                    .min(ONE_HUNDRED)
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveGmvScore(int scriptConversions, int totalOrders, BigDecimal totalRevenue, long audienceBase) {
        if (audienceBase <= 0 || scriptConversions <= 0 || totalOrders <= 0 || totalRevenue == null || totalRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal avgOrderValue = totalRevenue.divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_UP);
        BigDecimal attributableRevenue = avgOrderValue.multiply(BigDecimal.valueOf(scriptConversions));
        return attributableRevenue.multiply(ONE_HUNDRED)
                .divide(totalRevenue, 4, RoundingMode.HALF_UP)
                .min(ONE_HUNDRED)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private record ScoreWeights(BigDecimal conversionWeight, BigDecimal interactionWeight,
                                BigDecimal retentionWeight, BigDecimal gmvWeight) {
    }
}
