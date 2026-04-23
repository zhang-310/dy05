package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Optional;

/**
 * HyDE（Hypothetical Document Embeddings）：由 LLM 生成与检索意图对齐的假设文档片段，供向量检索使用。
 * 未启用或调用失败时返回 empty，调用方应回退为仅使用查询向量。
 */
public interface HydeExpansionService {

    /**
     * @param queryText 混合检索中的查询串（可含 GraphRAG 拼接；内部会按配置截断后送 LLM）
     */
    Optional<String> expandHypotheticalPassage(String queryText);
}
