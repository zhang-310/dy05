package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRealtimeDataRepository;
import cn.gaifan.douyinOperations.module.live.service.DanmakuSentimentService;
import cn.gaifan.douyinOperations.module.live.service.LiveRealtimeStrategyService;
import cn.gaifan.douyinOperations.module.live.vo.DanmakuSentimentSnapshotVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionRealtimeDataVO;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 实时话术策略引擎实现
 * <p>
 * 规则引擎（非 LLM），基于以下 6 条信号生成建议：
 * <ol>
 *   <li>在线人数趋势（上升/下降/稳定）</li>
 *   <li>弹幕情绪分布（正面/负面/中性主导）</li>
 *   <li>互动率（点赞+评论/在线）</li>
 *   <li>当前话术类型及连续执行段数</li>
 *   <li>弹幕高频关键词（如「太贵了」「不买了」）</li>
 *   <li>人气相对基线（是否处于波峰/波谷）</li>
 * </ol>
 */
@Service
public class LiveRealtimeStrategyServiceImpl implements LiveRealtimeStrategyService {

    private static final Logger log = LoggerFactory.getLogger(LiveRealtimeStrategyServiceImpl.class);

    @Resource
    private DanmakuSentimentService danmakuSentimentService;

    @Autowired(required = false)
    private LiveSessionRealtimeDataRepository realtimeDataRepository;

    @Autowired(required = false)
    private cn.gaifan.douyinOperations.module.live.service.DouyinRealtimeMetricsService douyinRealtimeMetricsService;

    /** 连续同类型话术超过此段数，建议切换 */
    @Value("${app.live.realtime-strategy.max-consecutive-same-type:3}")
    private int maxConsecutiveSameType;

    /** 互动率（点赞+评论/在线）低于此阈值，建议增加互动话术 */
    @Value("${app.live.realtime-strategy.low-interaction-rate:0.03}")
    private double lowInteractionRateThreshold;

    /** 负面弹幕比例高于此值，建议切换安抚/价值观话术 */
    @Value("${app.live.realtime-strategy.negative-sentiment-threshold:0.30}")
    private double negativeSentimentThreshold;

    /** 在线人数下降幅度高于此比例（相对上一分钟），触发留人建议 */
    @Value("${app.live.realtime-strategy.viewer-drop-threshold:0.10}")
    private double viewerDropThreshold;

    // ─── 核心策略计算 ─────────────────────────────────────────────────────────

    @Override
    public List<StrategyRecommendation> computeStrategy(
            Long sessionId,
            LiveSessionRealtimeDataVO realtimeData,
            DanmakuSentimentSnapshotVO sentimentSnapshot,
            String currentScriptType,
            int consecutiveSameTypeCount) {

        List<StrategyRecommendation> recommendations = new ArrayList<>();

        if (realtimeData == null) {
            return recommendations;
        }

        long viewerCount = realtimeData.getViewerCount() != null ? realtimeData.getViewerCount().longValue() : 0L;
        long likeCount = realtimeData.getLikeCount() != null ? realtimeData.getLikeCount().longValue() : 0L;
        long commentCount = realtimeData.getCommentCount() != null ? realtimeData.getCommentCount().longValue() : 0L;

        // ── 信号1：弹幕情绪分析 ────────────────────────────────────────────────
        if (sentimentSnapshot != null) {
            analyzesentiment(sentimentSnapshot, recommendations, viewerCount);
        }

        // ── 信号2：互动率检测 ──────────────────────────────────────────────────
        if (viewerCount > 50) {
            double interactionRate = (double) (likeCount + commentCount) / Math.max(viewerCount, 1);
            if (interactionRate < lowInteractionRateThreshold) {
                recommendations.add(new StrategyRecommendation(
                        "interaction",
                        "互动率偏低",
                        String.format("当前互动率 %.1f%%，低于阈值 %.0f%%，建议插入互动话术提升公屏活跃",
                                interactionRate * 100, lowInteractionRateThreshold * 100),
                        "warning",
                        String.format("点赞+评论=%d，在线=%d，互动率=%.1f%%", likeCount + commentCount, viewerCount, interactionRate * 100)
                ));
            }
        }

        // ── 信号3：连续同类型话术过久 ────────────────────────────────────────
        if (currentScriptType != null && consecutiveSameTypeCount >= maxConsecutiveSameType) {
            String suggested = suggestTypeSwitch(currentScriptType);
            if (suggested != null) {
                recommendations.add(new StrategyRecommendation(
                        suggested,
                        "话术类型切换建议",
                        String.format("已连续 %d 段「%s」话术，建议切换到「%s」避免观众审美疲劳",
                                consecutiveSameTypeCount, currentScriptType, suggested),
                        "normal",
                        String.format("连续%s达%d段", currentScriptType, consecutiveSameTypeCount)
                ));
            }
        }

        // ── 信号4：人气波峰检测（需要历史数据基线） ──────────────────────────
        if (isTrafficPeak(sessionId, realtimeData) && isProductTypeSuitable(currentScriptType)) {
            recommendations.add(new StrategyRecommendation(
                    "product",
                    "人气波峰，推荐主推品",
                    "当前在线人数处于近期高峰，是推介高转化/高利润商品的最佳时机",
                    "urgent",
                    String.format("当前在线 %d，处于波峰区间", viewerCount)
            ));
        }

        // ── 信号5：弹幕高频负面词检测 ────────────────────────────────────────
        if (sessionId != null) {
            analyzeKeywords(sessionId, recommendations);
        }

        // 按紧急程度排序：urgent > warning > normal
        recommendations.sort((a, b) -> urgencyOrder(b.urgency()) - urgencyOrder(a.urgency()));

        if (!recommendations.isEmpty()) {
            log.debug("[Strategy] session={} 生成 {} 条话术策略建议", sessionId, recommendations.size());
        }
        return recommendations;
    }

    @Override
    public boolean isTrafficPeak(Long sessionId, LiveSessionRealtimeDataVO realtimeData) {
        if (realtimeData == null || realtimeData.getViewerCount() == null) return false;
        long current = realtimeData.getViewerCount();
        if (current < 100) return false; // 基础流量太低，不算波峰

        // 与近期均值对比（从实时数据仓库查询，若不可用则用静态阈值）
        if (realtimeDataRepository != null && sessionId != null) {
            try {
                var recent = realtimeDataRepository.findByLiveSessionId(sessionId);
                if (recent.isPresent()) {
                    // 使用 watchedCount 作为历史基线
                    long baseline = recent.get().getWatchedCount() != null ? recent.get().getWatchedCount().longValue() / 10 : 0;
                    return baseline > 0 && current > baseline * 1.2;
                }
            } catch (Exception e) {
                log.debug("[Strategy] 波峰检测查询失败: {}", e.getMessage());
            }
        }
        return false;
    }

    @Override
    public List<StrategyRecommendation> getSessionStrategy(Long sessionId) {
        if (sessionId == null) return List.of();
        try {
            DanmakuSentimentSnapshotVO sentiment = danmakuSentimentService.getSnapshot(sessionId);

            LiveSessionRealtimeDataVO realtimeData = null;

            // 优先使用 DouyinRealtimeMetricsService 获取更精确的实时指标
            if (douyinRealtimeMetricsService != null) {
                var metrics = douyinRealtimeMetricsService.getMetrics(sessionId);
                if (metrics != null) {
                    realtimeData = new LiveSessionRealtimeDataVO();
                    realtimeData.setViewerCount(metrics.viewerCount());
                    realtimeData.setLikeCount(0); // metrics 中互动率已汇总
                    realtimeData.setCommentCount(0);

                    // 附加 GPM 相关建议
                    List<StrategyRecommendation> recommendations = new ArrayList<>(
                            computeStrategy(sessionId, realtimeData, sentiment, null, 0));

                    // GPM 驱动：波峰期推高 GPM 商品
                    if (douyinRealtimeMetricsService.isTrafficPeak(metrics)
                            && !metrics.topGpmProducts().isEmpty()) {
                        String topProductName = metrics.topGpmProducts().get(0).productName();
                        recommendations.add(0, new StrategyRecommendation(
                                "product",
                                "流量波峰，推高 GPM 商品",
                                String.format("当前在线 %d，处于流量波峰，建议立即讲解高 GPM 商品「%s」（GPM=%.1f元/千次观看）",
                                        metrics.viewerCount(), topProductName,
                                        metrics.topGpmProducts().get(0).gpm().doubleValue()),
                                "urgent",
                                String.format("viewerTrend=+%.0f%% GPM=%.1f",
                                        metrics.viewerTrend() * 100,
                                        metrics.topGpmProducts().get(0).gpm().doubleValue())
                        ));
                    }

                    // 互动率低告警
                    if (metrics.recentInteractionRate() < lowInteractionRateThreshold
                            && metrics.viewerCount() > 50) {
                        recommendations.add(new StrategyRecommendation(
                                "interaction",
                                "互动率偏低，需激活公屏",
                                String.format("当前互动率 %.1f%%（阈值 %.0f%%），建议插入提问/投票/弹幕互动话术",
                                        metrics.recentInteractionRate() * 100,
                                        lowInteractionRateThreshold * 100),
                                "warning",
                                String.format("interactionRate=%.1f%%", metrics.recentInteractionRate() * 100)
                        ));
                    }

                    recommendations.sort((a, b) -> urgencyOrder(b.urgency()) - urgencyOrder(a.urgency()));
                    return recommendations;
                }
            }

            // 降级：使用 live_session_realtime_data
            if (realtimeDataRepository != null) {
                var data = realtimeDataRepository.findByLiveSessionId(sessionId).orElse(null);
                if (data != null) {
                    realtimeData = new LiveSessionRealtimeDataVO();
                    realtimeData.setViewerCount(data.getViewerCount());
                    realtimeData.setLikeCount(data.getLikeCount());
                    realtimeData.setCommentCount(data.getCommentCount());
                }
            }

            return computeStrategy(sessionId, realtimeData, sentiment, null, 0);
        } catch (Exception e) {
            log.debug("[Strategy] getSessionStrategy 异常: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── 私有辅助方法 ──────────────────────────────────────────────────────────

    private void analyzesentiment(DanmakuSentimentSnapshotVO snapshot,
                                    List<StrategyRecommendation> recs, long viewerCount) {
        if (snapshot.getTotalInWindow() < 10) return;

        long total = snapshot.getTotalInWindow();
        long negative = snapshot.getNegativeCount();
        long positive = snapshot.getPositiveCount();

        double negativeRatio = (double) negative / total;
        double positiveRatio = (double) positive / total;

        if (negativeRatio >= negativeSentimentThreshold) {
            recs.add(new StrategyRecommendation(
                    "chat",
                    "弹幕负面情绪偏高",
                    String.format("近期弹幕负面比例 %.0f%%，建议暂停商品介绍，切换至聊天/情感话术安抚观众",
                            negativeRatio * 100),
                    "warning",
                    String.format("近%d条弹幕中负面%d条(%.0f%%)", total, negative, negativeRatio * 100)
            ));
        } else if (positiveRatio >= 0.60 && viewerCount > 50) {
            recs.add(new StrategyRecommendation(
                    "closing_deal",
                    "弹幕氛围热烈，可加速逼单",
                    String.format("近期弹幕正面比例 %.0f%%，观众情绪高涨，适合插入限时逼单话术",
                            positiveRatio * 100),
                    "normal",
                    String.format("近%d条弹幕中正面%d条(%.0f%%)", total, positive, positiveRatio * 100)
            ));
        }
    }

    private void analyzeKeywords(Long sessionId, List<StrategyRecommendation> recs) {
        try {
            Map<String, Integer> keywords = danmakuSentimentService.getKeywordFrequency(sessionId);
            if (keywords == null || keywords.isEmpty()) return;

            // 价格抗拒信号
            boolean hasPriceResistance = keywords.entrySet().stream()
                    .anyMatch(e -> (e.getKey().contains("贵") || e.getKey().contains("买不起")
                            || e.getKey().contains("太贵")) && e.getValue() >= 3);
            if (hasPriceResistance) {
                recs.add(new StrategyRecommendation(
                        "hold_back",
                        "弹幕出现价格抗拒",
                        "多条弹幕提到价格过高，建议切换憋单/蓄水话术，先建立价值感再逼单",
                        "warning",
                        "关键词: 贵/买不起 出现3次以上"
                ));
            }

            // 购买意向信号
            boolean hasBuyIntent = keywords.entrySet().stream()
                    .anyMatch(e -> (e.getKey().contains("买") || e.getKey().contains("链接")
                            || e.getKey().contains("多少钱")) && e.getValue() >= 5);
            if (hasBuyIntent) {
                recs.add(new StrategyRecommendation(
                        "product",
                        "弹幕出现购买意向",
                        "多条弹幕询价/要链接，建议立即插入报价+下单引导话术，抓住转化窗口",
                        "urgent",
                        "关键词: 买/链接/多少钱 出现5次以上"
                ));
            }
        } catch (Exception e) {
            log.debug("[Strategy] 关键词分析异常: {}", e.getMessage());
        }
    }

    /** 根据当前话术类型推荐切换目标 */
    private static String suggestTypeSwitch(String currentType) {
        return switch (currentType) {
            case "product" -> "interaction"; // 商品话术连续太久 → 互动
            case "interaction", "chat" -> "product"; // 纯互动太久 → 商品
            case "closing_deal" -> "product"; // 连续逼单 → 换个商品
            case "hold_back" -> "closing_deal"; // 蓄水完成 → 逼单
            default -> null;
        };
    }

    private static boolean isProductTypeSuitable(String currentType) {
        if (currentType == null) return true;
        return !("interaction".equals(currentType) || "chat".equals(currentType));
    }

    private static int urgencyOrder(String urgency) {
        if (urgency == null) return 0;
        return switch (urgency) {
            case "urgent" -> 3;
            case "warning" -> 2;
            case "normal" -> 1;
            default -> 0;
        };
    }
}
