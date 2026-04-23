package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.service.DanmakuAnalysisService;
import cn.gaifan.douyinOperations.module.live.service.LiveAlertRuleEngine;
import cn.gaifan.douyinOperations.module.live.service.LiveSuggestionService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LiveSuggestionServiceImpl implements LiveSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(LiveSuggestionServiceImpl.class);

    @Resource
    private DanmakuAnalysisService danmakuAnalysisService;
    @Resource
    private LiveAlertRuleEngine liveAlertRuleEngine;

    @Override
    public Map<String, Object> getSuggestions(Long sessionId, Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("timestamp", System.currentTimeMillis());
        result.put("suggestions", List.of(
                Map.of("type", "engagement", "message", "观众互动率偏低，建议发起一次抽奖或投票"),
                Map.of("type", "product", "message", "当前商品讲解时长已超过3分钟，建议切换下一品"),
                Map.of("type", "rhythm", "message", "连续讲解了2个商品，建议穿插一段互动话术")
        ));
        return result;
    }

    @Override
    public List<Map<String, Object>> getRealtimeSuggestions(Long sessionId, Map<String, Object> currentMetrics) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        List<Map<String, Object>> alerts = liveAlertRuleEngine.evaluate(sessionId, currentMetrics);
        for (Map<String, Object> alert : alerts) {
            suggestions.add(Map.of(
                    "type", "alert",
                    "level", alert.getOrDefault("level", "warning"),
                    "message", alert.getOrDefault("message", ""),
                    "action", alert.getOrDefault("suggestion", "")
            ));
        }
        return suggestions;
    }
}
