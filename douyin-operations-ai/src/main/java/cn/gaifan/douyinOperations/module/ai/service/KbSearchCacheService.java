package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService.SearchResult;

import java.util.List;

/**
 * 知识库检索缓存服务
 */
public interface KbSearchCacheService {

    /**
     * 构建缓存 key
     */
    String buildKey(Long kbId, String query, int topK);

    /**
     * 构建缓存 key（含个性化用户维度，避免与默认排序混用同一 key）
     */
    String buildKey(Long kbId, String query, int topK, Long personalizeUserId);

    /** S-4：缓存键需区分 {@code queryIntent}（如 GENERAL vs DEFINITION）。 */
    String buildKey(Long kbId, String query, int topK, Long personalizeUserId, String queryIntentTag);

    /**
     * 从缓存读取，未命中返回 null
     */
    List<SearchResult> get(String key);

    /**
     * 写入缓存
     */
    void put(String key, List<SearchResult> results, long ttlSeconds);

    /**
     * 按知识库清除缓存（导入/上传后使新文档可被检索）
     */
    void invalidateByKbId(Long kbId);

    /**
     * 统计打点：hit / miss
     */
    void incrementStat(String type);
}
