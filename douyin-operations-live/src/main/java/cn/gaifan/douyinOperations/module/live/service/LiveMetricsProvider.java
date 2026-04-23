package cn.gaifan.douyinOperations.module.live.service;

import java.util.Map;

public interface LiveMetricsProvider {
    Map<String, Double> getSessionMetrics(Long sessionId);
}
