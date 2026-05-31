package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 全文搜索服务
 */
public interface SearchService {

    /**
     * 创建索引
     */
    void createIndex(String indexName);

    /**
     * 删除索引
     */
    void deleteIndex(String indexName);

    /**
     * 索引文档
     */
    void indexDocument(String indexName, String id, Map<String, Object> document);

    /**
     * 批量索引文档
     */
    void bulkIndexDocuments(String indexName, List<DocumentWithId> documents);

    /**
     * 全文搜索
     */
    List<SearchResult> search(String indexName, String query, int from, int size, Map<String, Object> filters);

    /**
     * 删除文档
     */
    void deleteDocument(String indexName, String id);

    /**
     * 批量删除文档
     */
    void bulkDeleteDocuments(String indexName, List<String> ids);

    /**
     * 聚合分析
     */
    Map<String, Object> aggregate(String indexName, String field, String aggType);

    /**
     * 文档和 ID
     */
    record DocumentWithId(String id, Map<String, Object> document) {}

    /**
     * 搜索结果
     */
    record SearchResult(String id, double score, Map<String, Object> source) {}
}
