package cn.gaifan.douyinOperations.module.live.service;

import java.util.List;
import java.util.Map;

public interface SmartProductScheduler {
    List<Map<String, Object>> calculateProductScores(Long sessionId);
    Map<String, Object> generateReorderSuggestion(Long sessionId);
}
