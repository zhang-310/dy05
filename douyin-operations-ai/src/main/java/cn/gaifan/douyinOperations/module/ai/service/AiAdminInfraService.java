package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;

import java.util.List;
import java.util.Map;

/**
 * AI 运维管理服务：PostgreSQL 文档、ES 文档、Milvus 向量、索引队列
 * 按知识库搜索分页
 */
public interface AiAdminInfraService {

    /**
     * 基础设施健康检查：Milvus、ES、Redis、RabbitMQ 连通性
     *
     * @return 各组件状态，ok=true 表示正常，message 为错误信息
     */
    List<InfraHealthItem> checkHealth();

    record InfraHealthItem(String component, boolean ok, String message) {}

    /**
     * PostgreSQL 文档分页（按知识库、关键词搜索）
     */
    PageResultVO<Map<String, Object>> pagePgDocuments(Long kbId, String keyword, int page, int rows);

    /**
     * Elasticsearch 文档分页（按知识库，chunk 级别）
     */
    PageResultVO<Map<String, Object>> pageEsDocuments(Long kbId, String keyword, int page, int rows);

    /**
     * Milvus 向量统计（按知识库）
     */
    Map<String, Object> getMilvusStats(Long kbId);

    /**
     * 索引队列表分页（按知识库、状态搜索）
     */
    PageResultVO<Map<String, Object>> pageIndexQueue(Long kbId, String status, String keyword, int page, int rows);

    /**
     * P2 缓存命中率统计：stats:kb:cache:hit、stats:kb:cache:miss
     */
    Map<String, Object> getCacheStats();

    /**
     * Redis 缓存诊断：区分 Redis 全局命中率、业务缓存命中率、Key 前缀分布与 TTL 分布。
     */
    Map<String, Object> getCacheDiagnostics();

    /**
     * P2 检索性能统计：P50/P99/QPS
     */
    Map<String, Object> getSearchStats();

    /**
     * 基础设施详情（PostgreSQL、Milvus、ES、Redis 的版本、统计、资源使用及建议）
     */
    Map<String, Object> getInfraDetail();
}
