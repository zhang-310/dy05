package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 知识图谱实体抽取服务（Phase 5 P0-3）
 * 从文档内容中抽取实体和关系，写入 ai_graph_node / ai_graph_edge
 */
public interface GraphExtractorService {

    /**
     * 从文档内容中抽取实体和关系（异步执行）
     *
     * @param docId   文档 ID
     * @param content 文档内容
     * @param ownerId 所有者 ID（知识库所属用户）
     */
    void extractFromDocument(Long docId, String content, Long ownerId);
}
