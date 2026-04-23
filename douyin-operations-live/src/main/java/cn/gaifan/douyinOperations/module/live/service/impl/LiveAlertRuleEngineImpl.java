package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.service.LiveAlertRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LiveAlertRuleEngineImpl implements LiveAlertRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(LiveAlertRuleEngineImpl.class);

    private static final double VIEWER_DROP_THRESHOLD = 0.30;
    private static final double INTERACTION_RATE_THRESHOLD = 0.01;
    private static final double MAX_DURATION_HOURS = 4.0;

    @Override
    public List<Map<String, Object>> evaluate(Long sessionId, Map<String, Object> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> alerts = new ArrayList<>();

        checkViewerDrop(sessionId, metrics, alerts);
        checkInteractionRate(sessionId, metrics, alerts);
        checkDuration(sessionId, metrics, alerts);

        if (!alerts.isEmpty()) {
            log.info("直播预警: sessionId={}, 触发{}条规则", sessionId, alerts.size());
        }

        return alerts;
    }

    private void checkViewerDrop(Long sessionId, Map<String, Object> metrics, List<Map<String, Object>> alerts) {
        Number currentViewers = (Number) metrics.get("currentViewers");
        Number peakViewers = (Number) metrics.get("peakViewers");

        if (currentViewers == null || peakViewers == null || peakViewers.doubleValue() <= 0) {
            return;
        }

        double dropRate = 1.0 - (currentViewers.doubleValue() / peakViewers.doubleValue());
        if (dropRate > VIEWER_DROP_THRESHOLD) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("ruleId", "viewer_drop");
            alert.put("level", "warning");
            alert.put("sessionId", sessionId);
            alert.put("message", String.format("观众骤降 %.0f%%（当前 %d / 峰值 %d），建议调整话术节奏或发放福利",
                    dropRate * 100, currentViewers.intValue(), peakViewers.intValue()));
            alert.put("threshold", VIEWER_DROP_THRESHOLD);
            alert.put("actualValue", dropRate);
            alert.put("timestamp", System.currentTimeMillis());
            alerts.add(alert);
        }
    }

    private void checkInteractionRate(Long sessionId, Map<String, Object> metrics, List<Map<String, Object>> alerts) {
        Number interactionRate = (Number) metrics.get("interactionRate");

        if (interactionRate == null) {
            return;
        }

        if (interactionRate.doubleValue() < INTERACTION_RATE_THRESHOLD) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("ruleId", "low_interaction");
            alert.put("level", "warning");
            alert.put("sessionId", sessionId);
            alert.put("message", String.format("互动率过低 %.2f%%（阈值 %.0f%%），建议增加互动引导话术",
                    interactionRate.doubleValue() * 100, INTERACTION_RATE_THRESHOLD * 100));
            alert.put("threshold", INTERACTION_RATE_THRESHOLD);
            alert.put("actualValue", interactionRate.doubleValue());
            alert.put("timestamp", System.currentTimeMillis());
            alerts.add(alert);
        }
    }

    private void checkDuration(Long sessionId, Map<String, Object> metrics, List<Map<String, Object>> alerts) {
        Number durationMinutes = (Number) metrics.get("durationMinutes");

        if (durationMinutes == null) {
            return;
        }

        double durationHours = durationMinutes.doubleValue() / 60.0;
        if (durationHours > MAX_DURATION_HOURS) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("ruleId", "long_duration");
            alert.put("level", "info");
            alert.put("sessionId", sessionId);
            alert.put("message", String.format("直播时长已超 %.1f 小时（阈值 %.0f 小时），注意主播状态和内容质量",
                    durationHours, MAX_DURATION_HOURS));
            alert.put("threshold", MAX_DURATION_HOURS);
            alert.put("actualValue", durationHours);
            alert.put("timestamp", System.currentTimeMillis());
            alerts.add(alert);
        }
    }
}
