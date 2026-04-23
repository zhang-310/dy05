package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkScriptSimilarityVO;

import java.util.List;

/**
 * 质量脚本语义相似度服务
 */
public interface BenchmarkScriptSimilarityService {

    /**
     * 为质量脚本生成向量嵌入
     *
     * @param scriptId 质量脚本 ID
     * @param ownerId 用户 ID
     */
    void generateEmbedding(Long scriptId, Long ownerId);

    /**
     * 批量生成向量嵌入
     *
     * @param scriptIds 质量脚本 ID 列表
     * @param ownerId 用户 ID
     * @return 成功生成的数量
     */
    Integer batchGenerateEmbeddings(List<Long> scriptIds, Long ownerId);

    /**
     * 索引向量到 Milvus
     *
     * @param scriptId 质量脚本 ID
     * @param ownerId 用户 ID
     */
    void indexToMilvus(Long scriptId, Long ownerId);

    /**
     * 批量索引向量到 Milvus
     *
     * @param scriptIds 质量脚本 ID 列表
     * @param ownerId 用户 ID
     * @return 成功索引的数量
     */
    Integer batchIndexToMilvus(List<Long> scriptIds, Long ownerId);

    /**
     * 查找相似脚本
     *
     * @param scriptId 质量脚本 ID
     * @param ownerId 用户 ID
     * @param topK 返回前 K 个最相似的脚本
     * @param minScore 最小相似度阈值（0-1）
     * @return 相似脚本列表
     */
    List<BenchmarkScriptSimilarityVO> findSimilarScripts(Long scriptId, Long ownerId, Integer topK, Double minScore);

    /**
     * 根据文本查找相似脚本
     *
     * @param text 文本内容
     * @param ownerId 用户 ID
     * @param topK 返回前 K 个最相似的脚本
     * @param minScore 最小相似度阈值（0-1）
     * @return 相似脚本列表
     */
    List<BenchmarkScriptSimilarityVO> findSimilarScriptsByText(String text, Long ownerId, Integer topK, Double minScore);

    /**
     * 计算两个脚本的相似度
     *
     * @param scriptId1 脚本 1 ID
     * @param scriptId2 脚本 2 ID
     * @param ownerId 用户 ID
     * @return 相似度分数（0-1）
     */
    Double calculateSimilarity(Long scriptId1, Long scriptId2, Long ownerId);

    /**
     * 获取未生成向量的脚本 ID 列表
     *
     * @param ownerId 用户 ID
     * @param limit 限制数量
     * @return 脚本 ID 列表
     */
    List<Long> getUnembeddedScriptIds(Long ownerId, Integer limit);

    /**
     * 获取未索引到 Milvus 的脚本 ID 列表
     *
     * @param ownerId 用户 ID
     * @param limit 限制数量
     * @return 脚本 ID 列表
     */
    List<Long> getUnindexedScriptIds(Long ownerId, Integer limit);
}
