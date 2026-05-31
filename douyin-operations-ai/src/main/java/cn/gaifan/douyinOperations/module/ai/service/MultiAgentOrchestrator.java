package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Map;

public interface MultiAgentOrchestrator {
    Map<String, Object> executeWorkflow(Long sessionId, Long userId);
}
