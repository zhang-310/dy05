package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 向量数据库服务
 */
public interface VectorService {

    /**
     * 创建 Collection
     */
    void createCollection(String collectionName, int dimension);

    /**
     * 删除 Collection
     */
    void dropCollection(String collectionName);

    /**
     * 插入向量
     */
    void insertVectors(String collectionName, List<Long> ids, List<List<Float>> vectors, List<Map<String, Object>> metadata);

    /**
     * 向量相似度搜索
     */
    List<VectorSearchResult> search(String collectionName, List<Float> queryVector, int topK, String filter);

    /**
     * 批量向量搜索（每条 query 返回 topK 条结果，用于 chunk 级批量去重）
     */
    List<List<VectorSearchResult>> batchSearch(String collectionName, List<List<Float>> queryVectors, int topKPerQuery, String filter);

    /**
     * 删除向量
     */
    void deleteVectors(String collectionName, List<Long> ids);

    /**
     * 生成文本嵌入向量
     */
    List<Float> generateEmbedding(String text);

    /**
     * 批量生成文本嵌入向量
     */
    List<List<Float>> generateEmbeddings(List<String> texts);

    /**
     * 向量搜索结果
     */
    record VectorSearchResult(Long id, float score, Map<String, Object> metadata) {}
}
