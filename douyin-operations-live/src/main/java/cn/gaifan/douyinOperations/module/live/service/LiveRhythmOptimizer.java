package cn.gaifan.douyinOperations.module.live.service;

import java.util.Map;

public interface LiveRhythmOptimizer {
    Map<String, Object> optimizeSchedule(Long userId, Long sessionId);
}
