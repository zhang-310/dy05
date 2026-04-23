package cn.gaifan.douyinOperations.module.live.service;

/**
 * 话术效果归因服务
 * 根据 actual_execution_time 与 LiveMonitor 时序数据，计算执行后 30s 的 viewer_delta、interaction_delta，生成 effectiveness_score
 */
public interface LiveScriptAttributionService {

    /**
     * 对指定场次执行话术效果归因
     * 仅处理 executed=1 且 actualExecutionTime 非空的话术
     */
    int runAttribution(Long sessionId);

    /**
     * 场次结束后持久化归因数据（异步调用）
     */
    default int persistAttributionForSession(Long sessionId) {
        return runAttribution(sessionId);
    }
}
