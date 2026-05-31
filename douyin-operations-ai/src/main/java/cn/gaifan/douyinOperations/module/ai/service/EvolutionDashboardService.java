package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Map;

/**
 * 进化仪表盘与索引队列入口（从 EvolutionService 拆分）。
 */
public interface EvolutionDashboardService {

    long enqueueIndex(String sourceType, Long sourceId, String content, Integer priority);

    long enqueueIndex(String sourceType, Long sourceId, String content, Integer priority, Long targetKbId);

    Map<String, Object> getEvolutionStats();
}
