package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Map;

/**
 * 搜索个性化权重服务：根据用户历史搜索行为调整 BM25/Vector 权重
 */
public interface SearchPersonalizationService {

    /**
     * 获取用户个性化搜索权重
     *
     * @param userId 用户 ID
     * @return {bm25Weight, vectorWeight, personalized}
     */
    Map<String, Object> getPersonalizedWeights(Long userId);

    /**
     * 记录搜索点击行为
     *
     * @param userId     用户 ID
     * @param query      搜索查询
     * @param hitSource  命中来源（bm25 / vector / hybrid）
     * @param documentId 点击的文档 ID
     */
    void recordSearchClick(Long userId, String query, String hitSource, Long documentId);
}
