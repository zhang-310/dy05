package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptAttributionService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 话术效果归因实现
 * 根据 actual_execution_time 与 LiveMonitor 时序，计算执行后 30s 的 viewer_delta、interaction_delta、conversion_delta，
 * 生成 effectiveness_score = 停留×W1 + 互动×W2 + 转化×W3
 */
@Service
public class LiveScriptAttributionServiceImpl implements LiveScriptAttributionService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptAttributionServiceImpl.class);
    private static final int WINDOW_SECONDS = 30;
    /** 多维度权重：停留、互动、转化 */
    private static final double W_VIEWER = 0.35;
    private static final double W_INTERACTION = 0.35;
    private static final double W_CONVERSION = 0.30;

    @Resource
    private LiveScriptRepository scriptRepository;
    @Resource
    private LiveMonitorRepository monitorRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int runAttribution(Long sessionId) {
        if (sessionId == null || sessionId <= 0) return 0;

        List<LiveScript> scripts = scriptRepository.findBySessionIdAndExecutedAndDeleted(sessionId, 1, 0);
        List<LiveMonitor> monitors = monitorRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        if (monitors.isEmpty()) {
            log.debug("场次 {} 无 LiveMonitor 数据，跳过归因", sessionId);
            return 0;
        }

        AtomicInteger count = new AtomicInteger(0);
        for (LiveScript script : scripts) {
            Timestamp execTime = script.getActualExecutionTime();
            if (execTime == null) continue;

            long execMs = execTime.getTime();
            long afterMs = execMs + WINDOW_SECONDS * 1000L;

            MonitorPoint before = findNearest(monitors, execMs);
            MonitorPoint after = findNearest(monitors, afterMs);

            if (before == null || after == null) continue;

            int viewerDelta = after.viewers - before.viewers;
            long interactionDelta = (after.likes - before.likes) + (after.comments - before.comments) + (after.shares - before.shares);
            int conversionDelta = (after.orders - before.orders);

            script.setViewerDelta(viewerDelta);
            script.setInteractionDelta((int) Math.min(interactionDelta, Integer.MAX_VALUE));
            script.setConversionDelta(conversionDelta);
            script.setEffectivenessScore(computeScore(viewerDelta, (int) interactionDelta, conversionDelta));
            scriptRepository.save(script);
            count.incrementAndGet();
        }
        return count.get();
    }

    private MonitorPoint findNearest(List<LiveMonitor> monitors, long targetMs) {
        if (monitors.isEmpty()) return null;
        MonitorPoint best = null;
        long bestDiff = Long.MAX_VALUE;
        for (LiveMonitor m : monitors) {
            if (m.getTimestamp() == null) continue;
            long diff = Math.abs(m.getTimestamp().getTime() - targetMs);
            if (diff < bestDiff) {
                bestDiff = diff;
                int v = m.getViewers() != null ? m.getViewers() : 0;
                long l = m.getLikes() != null ? m.getLikes() : 0L;
                int c = m.getComments() != null ? m.getComments() : 0;
                int s = m.getShares() != null ? m.getShares() : 0;
                int orders = m.getOrders() != null ? m.getOrders() : 0;
                best = new MonitorPoint(v, l, c, s, orders);
            }
        }
        return best;
    }

    /** 多维度评分：停留×W1 + 互动×W2 + 转化×W3，归一化到 0-100 */
    private BigDecimal computeScore(int viewerDelta, int interactionDelta, int conversionDelta) {
        double viewerNorm = Math.min(25, Math.max(-25, viewerDelta / 2.0));
        double interactionNorm = Math.min(25, Math.max(-25, interactionDelta / 20.0));
        double conversionNorm = Math.min(25, Math.max(-25, conversionDelta * 5.0));
        double score = 50.0;
        score += W_VIEWER * viewerNorm;
        score += W_INTERACTION * interactionNorm;
        score += W_CONVERSION * conversionNorm;
        score = Math.max(0, Math.min(100, score));
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private record MonitorPoint(int viewers, long likes, int comments, int shares, int orders) {}
}
