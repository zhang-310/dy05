package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.repository.LiveDanmakuRecordRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.service.SmartProductScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Service
public class SmartProductSchedulerImpl implements SmartProductScheduler {

    @Autowired
    private LiveProductRepository productRepository;

    @Autowired(required = false)
    private LiveDanmakuRecordRepository danmakuRecordRepository;

    @Override
    public List<Map<String, Object>> calculateProductScores(Long sessionId) {
        List<LiveProduct> products = productRepository.findBySessionIdAndDeleted(sessionId, 0);
        if (products.isEmpty()) return List.of();

        // Get recent danmaku for heat index calculation
        Map<Long, Integer> productMentions = new HashMap<>();
        int totalDanmaku = 0;
        if (danmakuRecordRepository != null) {
            Timestamp tenMinAgo = new Timestamp(System.currentTimeMillis() - 10 * 60 * 1000);
            var recentDanmaku = danmakuRecordRepository.findBySessionIdAndDanmakuTimeAfterAndDeleted(sessionId, tenMinAgo, 0);
            totalDanmaku = recentDanmaku.size();
            for (var d : recentDanmaku) {
                for (LiveProduct p : products) {
                    if (p.getProductName() != null && d.getContent() != null
                            && d.getContent().contains(p.getProductName())) {
                        productMentions.merge(p.getProductId(), 1, Integer::sum);
                    }
                }
            }
        }

        List<Map<String, Object>> scoredProducts = new ArrayList<>();
        for (LiveProduct p : products) {
            double convRate = 0;
            double marginPct = 0;
            double heatIndex = 0;

            if (p.getRevenue() != null && p.getSaleQuantity() != null && p.getSaleQuantity() > 0) {
                convRate = Math.min(1.0, p.getSaleQuantity() / 100.0); // Simplified conversion
            }
            // Use revenue as proxy for margin
            if (p.getRevenue() != null) {
                marginPct = Math.min(1.0, p.getRevenue().doubleValue() / 10000.0);
            }
            if (totalDanmaku > 0) {
                heatIndex = productMentions.getOrDefault(p.getProductId(), 0) / (double) totalDanmaku;
            }

            double aiScore = 0.4 * convRate + 0.3 * marginPct + 0.3 * heatIndex;
            aiScore = Math.round(aiScore * 10000) / 100.0; // percentage with 2 decimals

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("productId", p.getProductId());
            entry.put("productName", p.getProductName());
            entry.put("currentPosition", p.getPosition());
            entry.put("aiScore", aiScore);
            entry.put("convRate", convRate);
            entry.put("marginPct", marginPct);
            entry.put("heatIndex", heatIndex);
            scoredProducts.add(entry);
        }

        scoredProducts.sort((a, b) -> Double.compare((double) b.get("aiScore"), (double) a.get("aiScore")));
        return scoredProducts;
    }

    @Override
    public Map<String, Object> generateReorderSuggestion(Long sessionId) {
        List<Map<String, Object>> scored = calculateProductScores(sessionId);
        if (scored.isEmpty()) return Map.of("hasReorder", false);

        // Check deviation from current order
        boolean needsReorder = false;
        List<Map<String, Object>> suggestions = new ArrayList<>();

        for (int i = 0; i < scored.size(); i++) {
            int currentPos = ((Number) scored.get(i).getOrDefault("currentPosition", i)).intValue();
            int suggestedPos = i + 1;
            double deviation = Math.abs(currentPos - suggestedPos) / (double) Math.max(1, scored.size());

            if (deviation > 0.3) {
                needsReorder = true;
                Map<String, Object> suggestion = new LinkedHashMap<>();
                suggestion.put("productName", scored.get(i).get("productName"));
                suggestion.put("currentPosition", currentPos);
                suggestion.put("suggestedPosition", suggestedPos);
                suggestion.put("aiScore", scored.get(i).get("aiScore"));
                suggestion.put("reason", "AI评分排名变化，建议调整位置");
                suggestions.add(suggestion);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasReorder", needsReorder);
        result.put("suggestions", suggestions);
        result.put("scoredProducts", scored);
        return result;
    }
}
