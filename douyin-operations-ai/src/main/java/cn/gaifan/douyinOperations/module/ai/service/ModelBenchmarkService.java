package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;

import java.util.List;
import java.util.Map;

public interface ModelBenchmarkService {
    void recordBenchmark(Long modelId, String taskCode, long latencyMs, int tokensUsed, boolean success);

    /** 从真实 AI 调用日志回流模型基准样本；无法识别任务/模型时跳过。 */
    void recordFromCallLog(AiCallLog log);

    List<Map<String, Object>> getModelComparison(String taskCode);
    Long selectBestModel(String taskCode, String priority);

    /** 推荐最优模型（含展示名），供管理端展示 */
    Map<String, Object> recommendBestModel(String taskCode, String priority);
}
