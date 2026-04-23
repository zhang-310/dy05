package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveDanmakuRecordRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveMetricsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class LiveMetricsProviderImpl implements LiveMetricsProvider {

    @Autowired
    private LiveSessionRepository sessionRepository;

    @Autowired(required = false)
    private LiveDanmakuRecordRepository danmakuRecordRepository;

    @Override
    public Map<String, Double> getSessionMetrics(Long sessionId) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        var sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) return metrics;

        LiveSession session = sessionOpt.get();

        // Viewer loss rate = (peak - current) / peak
        long currentViewers = session.getViewers() != null ? session.getViewers() : 0;
        // We use a simplified peak estimation
        long peakViewers = Math.max(currentViewers, 1);
        double viewerLossRate = peakViewers > 0 ? (peakViewers - currentViewers) / (double) peakViewers : 0;
        metrics.put("viewer_loss_rate", viewerLossRate);

        // Negative danmaku ratio
        if (danmakuRecordRepository != null) {
            Timestamp fifteenMinAgo = new Timestamp(System.currentTimeMillis() - 15 * 60 * 1000);
            var recentDanmaku = danmakuRecordRepository.findBySessionIdAndDanmakuTimeAfterAndDeleted(sessionId, fifteenMinAgo, 0);
            long totalCount = recentDanmaku.size();
            long negativeCount = recentDanmaku.stream()
                    .filter(d -> "negative".equals(d.getSentiment()))
                    .count();
            metrics.put("negative_ratio", totalCount > 0 ? negativeCount / (double) totalCount : 0);
            metrics.put("total_danmaku_15min", (double) totalCount);
        }

        // Conversion rate (simplified)
        metrics.put("conversion_rate", 0.0); // Would need purchase data integration

        return metrics;
    }
}
