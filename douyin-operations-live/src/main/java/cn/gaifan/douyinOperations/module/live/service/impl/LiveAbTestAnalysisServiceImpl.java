package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.event.AbTestWinnerEvent;
import cn.gaifan.douyinOperations.module.live.entity.LiveAbTestResult;
import cn.gaifan.douyinOperations.module.live.repository.LiveAbTestResultRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveAbTestAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A/B 测试闭环反馈分析服务实现
 */
@Service
public class LiveAbTestAnalysisServiceImpl implements LiveAbTestAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(LiveAbTestAnalysisServiceImpl.class);

    @Resource
    private LiveAbTestResultRepository abTestResultRepository;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void recordResult(Long sessionId, String experimentKey, String variant, String style,
                             BigDecimal effectivenessScore, BigDecimal conversionRate,
                             BigDecimal interactionRate, Integer sampleSize,
                             BigDecimal confidence, Long ownerId) {
        LiveAbTestResult result = new LiveAbTestResult();
        result.setSessionId(sessionId);
        result.setExperimentKey(experimentKey);
        result.setVariant(variant);
        result.setStyle(style);
        result.setEffectivenessScore(effectivenessScore);
        result.setConversionRate(conversionRate);
        result.setInteractionRate(interactionRate);
        result.setSampleSize(sampleSize != null ? sampleSize : 0);
        result.setConfidence(confidence);
        result.setOwnerId(ownerId);

        abTestResultRepository.save(result);
        log.info("A/B test result recorded: experiment={}, variant={}, score={}",
                experimentKey, variant, effectivenessScore);
    }

    @Override
    public Map<String, Object> getRecommendedStyle(Long ownerId) {
        // 查询该用户所有 A/B 测试结果，按效果评分降序
        List<LiveAbTestResult> results = abTestResultRepository
                .findByOwnerIdAndDeletedOrderByCreateTimeDesc(ownerId, 0);

        if (results.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("style", null);
            empty.put("confidence", BigDecimal.ZERO);
            empty.put("sampleSize", 0);
            return empty;
        }

        // 按风格聚合：取平均效果评分，选最优风格
        Map<String, List<LiveAbTestResult>> byStyle = results.stream()
                .filter(r -> r.getStyle() != null)
                .collect(Collectors.groupingBy(LiveAbTestResult::getStyle));

        String bestStyle = null;
        BigDecimal bestAvgScore = BigDecimal.ZERO;
        BigDecimal bestConfidence = BigDecimal.ZERO;
        int bestSampleSize = 0;

        for (Map.Entry<String, List<LiveAbTestResult>> entry : byStyle.entrySet()) {
            List<LiveAbTestResult> styleResults = entry.getValue();
            BigDecimal avgScore = styleResults.stream()
                    .filter(r -> r.getEffectivenessScore() != null)
                    .map(LiveAbTestResult::getEffectivenessScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long count = styleResults.stream()
                    .filter(r -> r.getEffectivenessScore() != null)
                    .count();
            if (count > 0) {
                avgScore = avgScore.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
            }

            int totalSamples = styleResults.stream()
                    .mapToInt(r -> r.getSampleSize() != null ? r.getSampleSize() : 0)
                    .sum();

            if (avgScore.compareTo(bestAvgScore) > 0) {
                bestStyle = entry.getKey();
                bestAvgScore = avgScore;
                bestSampleSize = totalSamples;
                // 取该风格下最高置信度
                bestConfidence = styleResults.stream()
                        .filter(r -> r.getConfidence() != null)
                        .map(LiveAbTestResult::getConfidence)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
            }
        }

        Map<String, Object> recommendation = new LinkedHashMap<>();
        recommendation.put("style", bestStyle);
        recommendation.put("confidence", bestConfidence);
        recommendation.put("sampleSize", bestSampleSize);
        return recommendation;
    }

    @Override
    public List<Map<String, Object>> getExperimentSummary(String experimentKey) {
        List<LiveAbTestResult> results = abTestResultRepository
                .findByExperimentKeyAndDeletedOrderByEffectivenessScoreDesc(experimentKey, 0);

        return results.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("variant", r.getVariant());
            item.put("style", r.getStyle());
            item.put("effectivenessScore", r.getEffectivenessScore());
            item.put("conversionRate", r.getConversionRate());
            item.put("interactionRate", r.getInteractionRate());
            item.put("sampleSize", r.getSampleSize());
            item.put("confidence", r.getConfidence());
            item.put("createTime", r.getCreateTime());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> calculateSignificance(String experimentKey) {
        // Get all results for experiment
        var results = getExperimentSummary(experimentKey);
        if (results == null || results.size() < 2) {
            return Map.of("significant", false, "reason", "需要至少两个变体数据", "confidenceLevel", "insufficient_data");
        }

        // Get top 2 variants
        var variant1 = results.get(0);
        var variant2 = results.get(1);

        double p1 = ((Number) variant1.getOrDefault("conversionRate", 0.0)).doubleValue();
        double p2 = ((Number) variant2.getOrDefault("conversionRate", 0.0)).doubleValue();
        int n1 = ((Number) variant1.getOrDefault("sampleSize", 0)).intValue();
        int n2 = ((Number) variant2.getOrDefault("sampleSize", 0)).intValue();

        if (n1 < 30 || n2 < 30) {
            return Map.of(
                    "significant", false,
                    "reason", "样本量不足（每组至少需要30个样本）",
                    "confidenceLevel", "insufficient_data",
                    "variant1", variant1.getOrDefault("variant", "A"),
                    "variant2", variant2.getOrDefault("variant", "B"),
                    "n1", n1, "n2", n2
            );
        }

        // Z-test for two proportions
        double pPooled = (p1 * n1 + p2 * n2) / (n1 + n2);
        double se = Math.sqrt(pPooled * (1 - pPooled) * (1.0 / n1 + 1.0 / n2));
        double zScore = se > 0 ? Math.abs(p1 - p2) / se : 0;

        // Approximate p-value (two-tailed)
        double pValue = 2 * (1 - normalCdf(zScore));
        boolean significant = pValue < 0.05;
        String confidenceLevel = pValue < 0.01 ? "99%" : pValue < 0.05 ? "95%" : pValue < 0.10 ? "90%" : "不显著";
        String winner = p1 > p2 ? String.valueOf(variant1.getOrDefault("variant", "A"))
                : String.valueOf(variant2.getOrDefault("variant", "B"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("significant", significant);
        result.put("pValue", Math.round(pValue * 10000) / 10000.0);
        result.put("zScore", Math.round(zScore * 1000) / 1000.0);
        result.put("winner", significant ? winner : "待定");
        result.put("confidenceLevel", confidenceLevel);
        result.put("variant1", variant1);
        result.put("variant2", variant2);

        // A/B 显著胜出时发布事件 → 驱动进化引擎创建高优先级主题
        if (significant) {
            double improvementPct = Math.abs(p1 - p2) / Math.max(Math.min(p1, p2), 0.001) * 100;
            try {
                applicationEventPublisher.publishEvent(
                        new AbTestWinnerEvent(experimentKey, winner, "conversionRate", improvementPct));
                log.info("[AbTest] 胜出事件已发布: experiment={}, winner={}, improvement={}%",
                        experimentKey, winner, String.format("%.1f", improvementPct));
            } catch (Exception e) {
                log.warn("[AbTest] 发布胜出事件失败: {}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * Standard normal CDF approximation (Abramowitz and Stegun)
     */
    private double normalCdf(double z) {
        if (z < -8) return 0;
        if (z > 8) return 1;
        double sum = 0, term = z;
        for (int i = 3; sum + term != sum; i += 2) {
            sum += term;
            term = term * z * z / i;
        }
        return 0.5 + sum * Math.exp(-z * z / 2 - 0.91893853320467274178);
    }
}
