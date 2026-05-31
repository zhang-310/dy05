package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.ContentEffectivenessService;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionStrategyService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Map;

/**
 * 进化策略服务实现 — 统计驱动升级版
 * <p>
 * 核心变更：
 * 1. 硬编码阈值 → 基于数据分布（均值 + 标准差）的动态阈值
 * 2. 新增季节性因子（大促期扩大内容池）
 * 3. 调整幅度 → Z 检验显著性判定（p < 0.05 → 1σ 步长）
 */
@Service
public class EvolutionStrategyServiceImpl implements EvolutionStrategyService {

    private static final Logger log = LoggerFactory.getLogger(EvolutionStrategyServiceImpl.class);

    @Resource
    private ContentEffectivenessService contentEffectivenessService;

    @Value("${app.ai.evolve.strategy.min-docs-for-adjust:20}")
    private int minDocsForAdjust;

    @Value("${app.ai.evolve.strategy.min-avg-citation:0.5}")
    private double minAvgCitation;

    @Value("${app.ai.evolve.strategy.viral-quality-step:5}")
    private int viralQualityStep;

    @Value("${app.ai.evolve.strategy.live-chars-step:50}")
    private int liveCharsStep;

    @Value("${app.ai.evolution.strategy-viral-quality-cap:90}")
    private int viralQualityCap;

    @Value("${app.ai.evolution.strategy-live-chars-cap:600}")
    private int liveCharsCap;

    /** 大促月日窗口（618、双11），策略阈值下调 */
    private static final List<MonthDay[]> PROMO_WINDOWS = List.of(
            new MonthDay[]{MonthDay.of(6, 1), MonthDay.of(6, 20)},   // 618
            new MonthDay[]{MonthDay.of(11, 1), MonthDay.of(11, 15)}, // 双11
            new MonthDay[]{MonthDay.of(12, 1), MonthDay.of(12, 15)}  // 双12
    );

    @Override
    public Integer suggestViralMinQuality(int defaultQuality) {
        if (contentEffectivenessService == null) return null;
        try {
            Map<String, Object> stats = contentEffectivenessService.getEffectivenessBySourceType("viral_analysis");
            long totalDocs = ((Number) stats.getOrDefault("totalDocs", 0L)).longValue();
            long totalCitation = ((Number) stats.getOrDefault("totalCitationCount", 0L)).longValue();
            if (totalDocs < minDocsForAdjust) return null;

            // 统计驱动：计算引用率均值和标准差
            double avgCitation = (double) totalCitation / totalDocs;
            // 估算标准差（基于泊松分布假设：σ ≈ √μ）
            double sigma = Math.sqrt(avgCitation);

            // 动态阈值：均值 + 0.5σ 以下的内容需提升
            double threshold = avgCitation + 0.5 * sigma;
            if (avgCitation >= Math.max(minAvgCitation, threshold)) return null;

            // 调整步长：基于偏离程度
            double zScore = sigma > 0 ? (minAvgCitation - avgCitation) / sigma : 1.0;
            int adjustedStep = zScore >= 1.96 ? viralQualityStep * 2 : viralQualityStep; // p<0.05 → 加倍步长

            // 季节性因子：大促期间下调 10%
            if (isPromoPeriod()) {
                adjustedStep = Math.max(1, (int) (adjustedStep * 0.9));
            }

            int suggested = Math.min(viralQualityCap, defaultQuality + adjustedStep);
            log.info("[进化策略] viral 统计驱动: totalDocs={}, avgCitation={}, σ={}, zScore={}, step={}, quality {} -> {}",
                    totalDocs, String.format("%.3f", avgCitation), String.format("%.3f", sigma),
                    String.format("%.2f", zScore), adjustedStep, defaultQuality, suggested);
            return suggested;
        } catch (Exception e) {
            log.warn("viral 策略建议计算失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Integer suggestLiveMinReportChars(int defaultChars) {
        if (contentEffectivenessService == null) return null;
        try {
            Map<String, Object> stats = contentEffectivenessService.getEffectivenessBySourceType("live_review");
            long totalDocs = ((Number) stats.getOrDefault("totalDocs", 0L)).longValue();
            long totalCitation = ((Number) stats.getOrDefault("totalCitationCount", 0L)).longValue();
            if (totalDocs < minDocsForAdjust) return null;

            double avgCitation = (double) totalCitation / totalDocs;
            double sigma = Math.sqrt(avgCitation);
            double threshold = avgCitation + 0.5 * sigma;
            if (avgCitation >= Math.max(minAvgCitation, threshold)) return null;

            double zScore = sigma > 0 ? (minAvgCitation - avgCitation) / sigma : 1.0;
            int adjustedStep = zScore >= 1.96 ? liveCharsStep * 2 : liveCharsStep;

            if (isPromoPeriod()) {
                adjustedStep = Math.max(10, (int) (adjustedStep * 0.9));
            }

            int suggested = Math.min(liveCharsCap, defaultChars + adjustedStep);
            log.info("[进化策略] live_review 统计驱动: totalDocs={}, avgCitation={}, σ={}, zScore={}, step={}, chars {} -> {}",
                    totalDocs, String.format("%.3f", avgCitation), String.format("%.3f", sigma),
                    String.format("%.2f", zScore), adjustedStep, defaultChars, suggested);
            return suggested;
        } catch (Exception e) {
            log.warn("live_review 策略建议计算失败: {}", e.getMessage());
            return null;
        }
    }

    /** 是否处于大促期 */
    static boolean isPromoPeriod() {
        MonthDay today = MonthDay.from(LocalDate.now());
        for (MonthDay[] window : PROMO_WINDOWS) {
            if (!today.isBefore(window[0]) && !today.isAfter(window[1])) {
                return true;
            }
        }
        return false;
    }
}
