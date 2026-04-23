package cn.gaifan.douyinOperations.module.live.service;

import java.util.List;
import java.util.Map;

public interface LiveAlertRuleEngine {
    List<Map<String, Object>> evaluate(Long sessionId, Map<String, Object> metrics);
}
