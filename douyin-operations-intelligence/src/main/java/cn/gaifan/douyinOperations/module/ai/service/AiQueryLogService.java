package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 检索查询日志服务（P2 监控埋点）
 */
public interface AiQueryLogService {

    /**
     * 记录一次检索查询
     * @param userId 用户 ID
     * @param kbId 知识库 ID
     * @param queryText 查询文本
     * @param topK 请求 topK
     * @param hitCount 命中数
     * @param latencyMs 延迟毫秒
     * @param cacheHit 是否缓存命中 1=是 0=否
     */
    void log(Long userId, Long kbId, String queryText, int topK, int hitCount, Integer latencyMs, int cacheHit);
}
