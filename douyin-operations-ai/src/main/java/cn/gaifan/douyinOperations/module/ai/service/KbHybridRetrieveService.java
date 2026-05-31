package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.search.QueryIntent;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService.SearchResult;

import java.util.List;
import java.util.Map;

/**
 * 知识库混合检索执行服务：向量 + ES 并行、RRF 融合、重排、效果加权
 */
public interface KbHybridRetrieveService {

    /**
     * 执行混合检索（不含缓存）
     *
     * @param kb            知识库
     * @param queries       查询列表（可含改写后的多查询）
     * @param topK          返回条数
     * @param metadataFilter Milvus expr 过滤
     * @param esFilters      ES 过滤条件
     * @param candidateTop   初筛候选数（用于重排）
     * @return 检索结果
     */
    List<SearchResult> retrieve(AiKnowledgeBase kb, List<String> queries, int topK,
                                String metadataFilter, Map<String, Object> esFilters, int candidateTop);

    /**
     * 混合检索（含可选用户个性化 RRF 权重，见 {@code personalizeUserId}）
     */
    List<SearchResult> retrieve(AiKnowledgeBase kb, List<String> queries, int topK,
                                String metadataFilter, Map<String, Object> esFilters, int candidateTop,
                                Long personalizeUserId);

    /**
     * S-4：混合检索，带查询意图（影响 RRF 向量/关键词权重与 HyDE 混合强度）。
     *
     * @param queryIntent 不可为 null；调用方传入 {@link QueryIntent#GENERAL} 表示不区分意图。
     */
    List<SearchResult> retrieve(AiKnowledgeBase kb, List<String> queries, int topK,
                                String metadataFilter, Map<String, Object> esFilters, int candidateTop,
                                Long personalizeUserId, QueryIntent queryIntent);
}
