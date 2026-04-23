package cn.gaifan.douyinOperations.module.script.service;

import java.util.List;

/**
 * 向量嵌入服务接口
 */
public interface VectorEmbeddingService {

    /**
     * 生成单个向量嵌入
     */
    byte[] generateEmbedding(String text);

    /**
     * 批量生成向量嵌入
     */
    List<byte[]> batchGenerateEmbeddings(List<String> texts);

    /**
     * 更新话术向量
     */
    void updateEmbedding(Long scriptId, String newText, Long userId);

    /**
     * 索引向量到 Milvus
     */
    void indexToMilvus(Long scriptVectorEmbeddingId);

    /**
     * 批量索引向量到 Milvus
     */
    void batchIndexToMilvus(List<Long> scriptVectorEmbeddingIds);

    /**
     * 检查向量维度
     */
    void validateVectorDimension(byte[] vector);

    /**
     * 获取未索引的向量嵌入
     */
    List<Long> getUnindexedEmbeddingIds(int limit);
}
