package cn.gaifan.douyinOperations.module.live.service;

import java.util.List;
import java.util.Map;

public interface LiveSuggestionService {
    Map<String, Object> getSuggestions(Long sessionId, Long userId);
    List<Map<String, Object>> getRealtimeSuggestions(Long sessionId, Map<String, Object> currentMetrics);
}
