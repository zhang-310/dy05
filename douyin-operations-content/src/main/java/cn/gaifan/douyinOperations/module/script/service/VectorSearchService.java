package cn.gaifan.douyinOperations.module.script.service;

import cn.gaifan.douyinOperations.module.script.vo.HybridSearchRequestVO;
import cn.gaifan.douyinOperations.module.script.vo.HybridSearchResultVO;
import cn.gaifan.douyinOperations.module.script.vo.SearchSuggestionVO;
import cn.gaifan.douyinOperations.module.script.vo.SearchAnalyticsVO;

import java.util.List;

/**
 * 向量搜索服务接口
 */
public interface VectorSearchService {

    /**
     * 混合搜索（向量 + BM25 + RRF 融合）
     */
    HybridSearchResultVO hybridSearch(HybridSearchRequestVO request, Long userId);

    /**
     * 纯向量搜索
     */
    HybridSearchResultVO semanticSearch(String query, Integer topK, Long userId, Boolean withCrossEncoder);

    /**
     * 纯 BM25 关键词搜索
     */
    HybridSearchResultVO lexicalSearch(String query, Integer topK, Long userId);

    /**
     * 获取搜索建议
     */
    SearchSuggestionVO getSearchSuggestions(String prefix, Long userId, Integer limit);

    /**
     * 记录搜索结果
     */
    void recordSearchResult(String query, String searchType, Integer totalResults, Long topResultId,
                          Long executionTimeMs, String userId, String ipAddress);

    /**
     * 记录点击反馈
     */
    void recordClickFeedback(Long searchResultId, Long clickedScriptId, Boolean isSatisfied);

    /**
     * 获取搜索分析数据
     */
    List<SearchAnalyticsVO> getSearchAnalytics(Long userId, String startDate, String endDate);

    /**
     * 生成向量嵌入
     */
    byte[] generateEmbedding(String text);

    /**
     * 批量生成向量嵌入
     */
    List<byte[]> batchGenerateEmbeddings(List<String> texts);

    /**
     * 更新话术向量
     */
    void updateScriptVector(Long scriptId, String newTitle, String newContent, Long userId);
}
