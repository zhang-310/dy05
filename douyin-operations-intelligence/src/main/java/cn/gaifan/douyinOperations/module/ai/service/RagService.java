package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.vo.RagRetrieveItemVO;

import java.util.List;

public interface RagService {

    List<RagRetrieveItemVO> retrieve(Long userId, String query, int topK, String scope);

    String buildRagContext(Long userId, String query, int topK, String scope);

    /**
     * 双轨检索：根据 IP 类型路由到不同的检索策略
     * @param userId 用户 ID
     * @param query 检索关键词
     * @param topK 返回条数
     * @param ipType IP类型：phenomenal（热点优先）/ top（深度优先）
     * @return 检索结果
     */
    List<RagRetrieveItemVO> retrieveByIpType(Long userId, String query, int topK, String ipType);

    /**
     * 热点追踪检索：优先从热点相关知识库检索
     */
    List<RagRetrieveItemVO> retrieveHotspot(Long userId, String query, int topK);

    /**
     * 行业深度检索：优先从行业专业知识库检索
     */
    List<RagRetrieveItemVO> retrieveIndustryKnowledge(Long userId, String query, int topK);
}
