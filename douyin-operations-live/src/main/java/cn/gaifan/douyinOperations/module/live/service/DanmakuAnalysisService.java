package cn.gaifan.douyinOperations.module.live.service;

import java.util.List;
import java.util.Map;

public interface DanmakuAnalysisService {
    Map<String, Object> analyzeIntents(Long sessionId, List<String> danmakuTexts);
    List<String> suggestScriptAdjustments(Long sessionId, Map<String, Object> intents);
    Map<String, Object> analyzeBatchIntents(Long sessionId, List<String> danmakuTexts);
}
