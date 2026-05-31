package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * 检索日志服务：记录检索命中，支撑质量评分 usageCount、进化规则
 */
public interface SearchLogService {

    /**
     * 异步记录检索日志
     */
    void logSearchAsync(Long ownerId, String queryText, Long kbId, List<Long> hitDocIds,
                        Double top1Score, String searchType, int latencyMs);

    /**
     * 统计某文档最近 days 天内被检索命中次数
     */
    long getUsageCountForDoc(Long ownerId, Long docId, int days);

    /**
     * 更新检索日志的用户反馈（helpful / not_helpful），并可同步调整 Top 命中文档 boost
     */
    void updateFeedback(Long logId, String feedback, Long ownerId);

    /**
     * 基于近期检索日志的 Top-K 查询建议（搜索框补全）
     */
    List<String> suggestPopularQueries(Long ownerId, String prefix, int limit);
}
