package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.config.LiveBusinessConfig;
import cn.gaifan.douyinOperations.module.live.config.LiveDanmakuSentimentProperties;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveDanmakuRecordRepository;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveDanmakuRecord;
import cn.gaifan.douyinOperations.module.live.service.DanmakuAnalysisService;
import cn.gaifan.douyinOperations.module.live.service.DanmakuSentimentService;
import cn.gaifan.douyinOperations.module.live.service.LiveRealtimeSuggestionService;
import cn.gaifan.douyinOperations.module.live.vo.DanmakuSentimentSnapshotVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionRealtimeDataVO;
import cn.gaifan.douyinOperations.module.live.vo.RealtimeSuggestionVO;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 直播实时建议服务实现（P2-1）
 * 根据实时数据计算指标并生成建议
 */
@Service
public class LiveRealtimeSuggestionServiceImpl implements LiveRealtimeSuggestionService {

    @Autowired(required = false)
    private LiveBusinessConfig liveBusinessConfig;

    @Autowired
    private DanmakuSentimentService danmakuSentimentService;

    @Autowired(required = false)
    private DanmakuAnalysisService danmakuAnalysisService;

    @Autowired
    private LiveDanmakuSentimentProperties danmakuSentimentProperties;

    @Resource
    private LiveScriptRepository liveScriptRepository;

    @Resource
    private LiveDanmakuRecordRepository liveDanmakuRecordRepository;

    @Resource
    private DyProductRepository dyProductRepository;

    @Override
    public List<RealtimeSuggestionVO> evaluateSuggestions(LiveSessionRealtimeDataVO data, Long liveSessionId) {
        List<RealtimeSuggestionVO> list = new ArrayList<>();
        if (data == null || liveBusinessConfig == null) {
            return list;
        }
        if (liveBusinessConfig.getRealtimeSuggestion().isEnabled()) {
        LiveBusinessConfig.RealtimeSuggestion cfg = liveBusinessConfig.getRealtimeSuggestion();
        int viewerCount = data.getViewerCount() != null ? data.getViewerCount() : 0;
        int watchedCount = data.getWatchedCount() != null ? data.getWatchedCount() : 0;
        int likeCount = data.getLikeCount() != null ? data.getLikeCount() : 0;
        int commentCount = data.getCommentCount() != null ? data.getCommentCount() : 0;
        int productPurchaseCount = data.getProductPurchaseCount() != null ? data.getProductPurchaseCount() : 0;

        double retentionRate = viewerCount > 0 ? (watchedCount * 100.0 / viewerCount) : 0;
        double interactionRate = viewerCount > 0 ? ((likeCount + commentCount) * 100.0 / viewerCount) : 0;
        double conversionRate = viewerCount > 0 ? (productPurchaseCount * 100.0 / viewerCount) : 0;

        if (retentionRate > 0 && retentionRate < cfg.getMinRetentionRate()) {
            RealtimeSuggestionVO s = new RealtimeSuggestionVO();
            s.setType("switch_script");
            s.setReason(String.format("停留率下降至 %.0f%%，低于阈值 %d%%", retentionRate, cfg.getMinRetentionRate()));
            s.setSuggestion("建议切换到互动话术，提问观众使用体验");
            s.setUrgency(4);
            s.setActionType("next_slot");
            s.setActionPayload(Map.of());
            list.add(s);
        }
        if (interactionRate > 0 && interactionRate < cfg.getMinInteractionRate()) {
            RealtimeSuggestionVO s = new RealtimeSuggestionVO();
            s.setType("inject_interaction");
            s.setReason(String.format("互动率 %.0f%% 低于阈值 %d%%", interactionRate, cfg.getMinInteractionRate()));
            s.setSuggestion("建议注入互动话术，引导点赞评论");
            s.setUrgency(3);
            s.setActionType("inject_interaction");
            s.setActionPayload(Map.of());
            list.add(s);
        }
        if (conversionRate > 0 && conversionRate < cfg.getMinConversionRate()) {
            RealtimeSuggestionVO s = new RealtimeSuggestionVO();
            s.setType("switch_script");
            s.setReason(String.format("转化率 %.0f%% 较低，产品讲解已超 %d 分钟", conversionRate, cfg.getMaxExplainMinutes()));
            s.setSuggestion("建议切换下一个产品");
            s.setUrgency(3);
            s.setActionType("next_slot");
            s.setActionPayload(Map.of());
            list.add(s);
        }
        if (danmakuSentimentProperties.isEnabled()
                && liveSessionId != null) {
            DanmakuSentimentSnapshotVO snap = danmakuSentimentService.getSnapshot(liveSessionId);
            if (snap.getNegativeCount() >= danmakuSentimentProperties.getSuggestWhenNegativeCountGte()) {
                RealtimeSuggestionVO s = new RealtimeSuggestionVO();
                s.setType("danmaku_sentiment");
                s.setReason(String.format(
                        "近 %d 秒窗口内负向弹幕 %d 条（阈值 %d）",
                        snap.getWindowSeconds(),
                        snap.getNegativeCount(),
                        danmakuSentimentProperties.getSuggestWhenNegativeCountGte()));
                s.setSuggestion("建议主动互动安抚情绪，或对质疑点简明澄清产品与售后政策");
                s.setUrgency(4);
                s.setActionType("inject_interaction");
                s.setActionPayload(Map.of("hint", "calm_audience"));
                list.add(s);

                // 负面弹幕超阈值 + 转化率低 → 建议切换商品
                if (conversionRate > 0 && conversionRate < cfg.getMinConversionRate()) {
                    RealtimeSuggestionVO cp = new RealtimeSuggestionVO();
                    cp.setType("change_product");
                    cp.setReason(String.format(
                            "负向弹幕 %d 条且转化率仅 %.0f%%，当前商品讲解效果不佳",
                            snap.getNegativeCount(), conversionRate));
                    cp.setSuggestion("建议切换到下一个商品，当前商品可稍后再讲或调整话术角度");
                    cp.setUrgency(5);
                    cp.setActionType("next_slot");
                    cp.setActionPayload(Map.of("hint", "change_product"));
                    list.add(cp);
                }
            }

            // P2-1: 关键词驱动话术切换建议
            Map<String, Integer> kwFreq = danmakuSentimentService.getKeywordFrequency(liveSessionId);
            if (!kwFreq.isEmpty()) {
                int priceCnt = kwFreq.getOrDefault("太贵了", 0) + kwFreq.getOrDefault("好贵", 0)
                        + kwFreq.getOrDefault("贵", 0) + kwFreq.getOrDefault("买不起", 0);
                int linkCnt = kwFreq.getOrDefault("链接在哪", 0) + kwFreq.getOrDefault("怎么买", 0)
                        + kwFreq.getOrDefault("在哪里买", 0) + kwFreq.getOrDefault("链接", 0);
                int qualityCnt = kwFreq.getOrDefault("质量怎么样", 0) + kwFreq.getOrDefault("质量好吗", 0)
                        + kwFreq.getOrDefault("会不会假", 0);

                if (priceCnt >= 3) {
                    RealtimeSuggestionVO s = new RealtimeSuggestionVO();
                    s.setType("keyword_price_objection");
                    s.setReason(String.format("窗口内 %d 条「价格疑虑」弹幕", priceCnt));
                    s.setSuggestion("建议切换「价值塑造话术」：强调成分/功效/性价比，与同类产品对比，展示真实用感");
                    s.setUrgency(4);
                    s.setActionType("suggest_script_type");
                    s.setActionPayload(Map.of("scriptType", "pain_point", "hint", "price_objection"));
                    list.add(s);
                }
                if (linkCnt >= 3) {
                    RealtimeSuggestionVO s = new RealtimeSuggestionVO();
                    s.setType("keyword_purchase_intent");
                    s.setReason(String.format("窗口内 %d 条「求购买链接」弹幕，购买意向强烈", linkCnt));
                    s.setSuggestion("建议插入「下单引导话术」：告知链接位置、优惠截止时间、库存提醒");
                    s.setUrgency(5);
                    s.setActionType("suggest_script_type");
                    s.setActionPayload(Map.of("scriptType", "closing_deal", "hint", "purchase_intent"));
                    list.add(s);
                }
                if (qualityCnt >= 3) {
                    RealtimeSuggestionVO s = new RealtimeSuggestionVO();
                    s.setType("keyword_quality_doubt");
                    s.setReason(String.format("窗口内 %d 条「质量疑虑」弹幕", qualityCnt));
                    s.setSuggestion("建议切换「信任背书话术」：展示资质证书/用户评价/成分检测报告");
                    s.setUrgency(4);
                    s.setActionType("suggest_script_type");
                    s.setActionPayload(Map.of("scriptType", "testimony", "hint", "quality_doubt"));
                    list.add(s);
                }
            }
        }

        // Intent-aware suggestions: high purchase intent → accelerate product push
        // Only analyze when a real recent danmaku window exists, avoiding empty-context intent calls.
        if (danmakuAnalysisService != null && liveSessionId != null) {
            try {
                List<String> recentDanmakuTexts = loadRecentDanmakuTexts(liveSessionId);
                if (recentDanmakuTexts.isEmpty()) {
                    return list;
                }
                var intentResult = danmakuAnalysisService.analyzeBatchIntents(liveSessionId, recentDanmakuTexts);
                int purchaseIntentCount = ((Number) intentResult.getOrDefault("purchase_intent", 0)).intValue();
                int totalIntentCount = ((Number) intentResult.getOrDefault("total", 0)).intValue();
                if (totalIntentCount > 0 && purchaseIntentCount > totalIntentCount * 0.4) {
                    RealtimeSuggestionVO s = new RealtimeSuggestionVO();
                    s.setType("purchase_intent_high");
                    s.setReason(String.format("购买意向弹幕占比 %.0f%%，观众购买欲望强烈",
                            purchaseIntentCount * 100.0 / totalIntentCount));
                    s.setSuggestion("建议立即推出优惠活动或限时折扣，趁热打铁促进转化");
                    s.setUrgency(5);
                    s.setActionType("inject_interaction");
                    s.setActionPayload(Map.of("hint", "accelerate_conversion"));
                    list.add(s);
                }
            } catch (Exception ignored) {
                // Intent analysis failure should not block other suggestions
            }
        }
        }
        if (liveBusinessConfig.getLiveInventory() != null
                && liveBusinessConfig.getLiveInventory().isRealtimeSuggestionEnabled()
                && liveSessionId != null
                && data.getCurrentSlotIndex() != null) {
            java.util.List<LiveScript> ord = liveScriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(liveSessionId, 0);
            int idx = data.getCurrentSlotIndex();
            if (idx >= 0 && idx < ord.size()) {
                Long pid = ord.get(idx).getProductId();
                if (pid != null) {
                    dyProductRepository.findById(pid).ifPresent(p -> {
                        Long inv = p.getInventory();
                        if (inv == null) {
                            return;
                        }
                        var li = liveBusinessConfig.getLiveInventory();
                        if (inv > li.getLowStockThreshold()) {
                            return;
                        }
                        RealtimeSuggestionVO s = new RealtimeSuggestionVO();
                        s.setType("inventory_low");
                        s.setReason(String.format("当前槽位关联商品「%s」系统库存 %d（低库存阈值 %d）",
                                p.getProductName() != null ? p.getProductName() : ("ID " + pid),
                                inv, li.getLowStockThreshold()));
                        s.setSuggestion(inv <= li.getCriticalStockThreshold()
                                ? "库存极低：话术需强调限量与即时下单，避免承诺具体可售件数。"
                                : "库存偏紧：可适当营造稀缺感，促单时注意与运营实盘一致。");
                        s.setUrgency(inv <= li.getCriticalStockThreshold() ? 5 : 3);
                        s.setActionType("inject_interaction");
                        s.setActionPayload(Map.of("hint", "scarcity_inventory"));
                        list.add(s);
                    });
                }
            }
        }
        return list;
    }

    private List<String> loadRecentDanmakuTexts(Long liveSessionId) {
        if (liveDanmakuRecordRepository == null || liveSessionId == null) {
            return List.of();
        }
        int lookbackSeconds = Math.max(danmakuSentimentProperties.getWindowSeconds() * 2, 300);
        Timestamp after = Timestamp.from(Instant.now().minusSeconds(lookbackSeconds));
        List<LiveDanmakuRecord> recentDanmaku = liveDanmakuRecordRepository
                .findBySessionIdAndDanmakuTimeAfterAndDeleted(liveSessionId, after, 0);
        return recentDanmaku.stream()
                .map(LiveDanmakuRecord::getContent)
                .filter(text -> text != null && !text.isBlank())
                .distinct()
                .limit(100)
                .collect(Collectors.toList());
    }
}
