package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.script.service.VectorSearchService;
import cn.gaifan.douyinOperations.module.script.service.VectorEmbeddingService;
import cn.gaifan.douyinOperations.module.script.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 混合搜索控制器
 * 提供向量搜索、BM25 搜索、混合搜索、搜索建议等 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/script/search")
public class HybridSearchController {

    @Resource
    private VectorSearchService vectorSearchService;

    @Resource
    private VectorEmbeddingService vectorEmbeddingService;

    /**
     * 混合搜索（向量 + BM25 + RRF 融合）
     *
     * POST /api/v1/script/search/hybrid
     *
     * @param request 搜索请求
     * @param httpRequest HTTP 请求
     * @return 混合搜索结果
     */
    @PostMapping("/hybrid")
    public RESTResult<HybridSearchResultVO> hybridSearch(
            @Valid @RequestBody HybridSearchRequestVO request,
            HttpServletRequest httpRequest) {
        log.info("混合搜索请求: query={}, page={}, rows={}",
                request.getQuery(), request.getPage(), request.getRows());

        try {
            // 从 HTTP 请求获取用户 ID
            Long userId = extractUserId(httpRequest);

            HybridSearchResultVO result = vectorSearchService.hybridSearch(request, userId);
            return RESTResult.success(result);
        } catch (Exception e) {
            log.error("混合搜索异常", e);
            return RESTResult.fail(400, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 语义搜索（纯向量搜索）
     *
     * POST /api/v1/script/search/semantic
     *
     * @param request 搜索请求
     * @param httpRequest HTTP 请求
     * @return 搜索结果
     */
    @PostMapping("/semantic")
    public RESTResult<HybridSearchResultVO> semanticSearch(
            @Valid @RequestBody SemanticSearchRequestVO request,
            HttpServletRequest httpRequest) {
        log.info("语义搜索请求: query={}", request.getQuery());

        try {
            Long userId = extractUserId(httpRequest);
            HybridSearchResultVO result = vectorSearchService.semanticSearch(
                    request.getQuery(),
                    request.getTopK(),
                    userId,
                    request.getWithCrossEncoder()
            );
            return RESTResult.success(result);
        } catch (Exception e) {
            log.error("语义搜索异常", e);
            return RESTResult.fail(400, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * BM25 关键词搜索
     *
     * POST /api/v1/script/search/lexical
     *
     * @param request 搜索请求
     * @param httpRequest HTTP 请求
     * @return 搜索结果
     */
    @PostMapping("/lexical")
    public RESTResult<HybridSearchResultVO> lexicalSearch(
            @Valid @RequestBody LexicalSearchRequestVO request,
            HttpServletRequest httpRequest) {
        log.info("BM25 搜索请求: query={}", request.getQuery());

        try {
            Long userId = extractUserId(httpRequest);
            HybridSearchResultVO result = vectorSearchService.lexicalSearch(
                    request.getQuery(),
                    request.getTopK(),
                    userId
            );
            return RESTResult.success(result);
        } catch (Exception e) {
            log.error("BM25 搜索异常", e);
            return RESTResult.fail(400, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 搜索建议（自动补全）
     *
     * POST /api/v1/script/search/suggest
     *
     * @param request 建议请求
     * @param httpRequest HTTP 请求
     * @return 建议列表
     */
    @PostMapping("/suggest")
    public RESTResult<SearchSuggestionVO> getSearchSuggestions(
            @Valid @RequestBody SearchSuggestRequestVO request,
            HttpServletRequest httpRequest) {
        log.info("搜索建议请求: prefix={}", request.getPrefix());

        try {
            Long userId = extractUserId(httpRequest);
            SearchSuggestionVO result = vectorSearchService.getSearchSuggestions(
                    request.getPrefix(),
                    userId,
                    request.getLimit()
            );
            return RESTResult.success(result);
        } catch (Exception e) {
            log.error("搜索建议异常", e);
            return RESTResult.fail(400, "获取建议失败: " + e.getMessage());
        }
    }

    /**
     * 搜索分析
     *
     * POST /api/v1/script/search/analytics
     *
     * @param request 分析请求
     * @param httpRequest HTTP 请求
     * @return 分析数据
     */
    @PostMapping("/analytics")
    public RESTResult<List<SearchAnalyticsVO>> getSearchAnalytics(
            @Valid @RequestBody SearchAnalyticsRequestVO request,
            HttpServletRequest httpRequest) {
        log.info("搜索分析请求: startDate={}, endDate={}", request.getStartDate(), request.getEndDate());

        try {
            Long userId = extractUserId(httpRequest);
            List<SearchAnalyticsVO> result = vectorSearchService.getSearchAnalytics(
                    userId,
                    request.getStartDate(),
                    request.getEndDate()
            );
            return RESTResult.success(result);
        } catch (Exception e) {
            log.error("搜索分析异常", e);
            return RESTResult.fail(400, "分析失败: " + e.getMessage());
        }
    }

    /**
     * 记录搜索点击反馈
     *
     * POST /api/v1/script/search/feedback
     *
     * @param request 反馈请求
     * @return 响应
     */
    @PostMapping("/feedback")
    public RESTResult<Void> recordClickFeedback(
            @Valid @RequestBody ClickFeedbackRequestVO request) {
        log.info("记录点击反馈: searchResultId={}, clickedScriptId={}",
                request.getSearchResultId(), request.getClickedScriptId());

        try {
            vectorSearchService.recordClickFeedback(
                    request.getSearchResultId(),
                    request.getClickedScriptId(),
                    request.getIsSatisfied()
            );
            return RESTResult.success(null);
        } catch (Exception e) {
            log.error("记录点击反馈异常", e);
            return RESTResult.fail(400, "反馈失败: " + e.getMessage());
        }
    }

    // ========== 请求 VO ==========

    /**
     * 语义搜索请求 VO
     */
    public static class SemanticSearchRequestVO {
        private String query;
        private Integer topK = 20;
        private Boolean withCrossEncoder = false;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getTopK() { return topK; }
        public void setTopK(Integer topK) { this.topK = topK; }
        public Boolean getWithCrossEncoder() { return withCrossEncoder; }
        public void setWithCrossEncoder(Boolean withCrossEncoder) { this.withCrossEncoder = withCrossEncoder; }
    }

    /**
     * BM25 搜索请求 VO
     */
    public static class LexicalSearchRequestVO {
        private String query;
        private Integer topK = 20;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getTopK() { return topK; }
        public void setTopK(Integer topK) { this.topK = topK; }
    }

    /**
     * 搜索建议请求 VO
     */
    public static class SearchSuggestRequestVO {
        private String prefix;
        private Integer limit = 10;
        private Boolean includeHotTopics = true;

        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
        public Boolean getIncludeHotTopics() { return includeHotTopics; }
        public void setIncludeHotTopics(Boolean includeHotTopics) { this.includeHotTopics = includeHotTopics; }
    }

    /**
     * 搜索分析请求 VO
     */
    public static class SearchAnalyticsRequestVO {
        private String startDate;
        private String endDate;

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
    }

    /**
     * 点击反馈请求 VO
     */
    public static class ClickFeedbackRequestVO {
        private Long searchResultId;
        private Long clickedScriptId;
        private Boolean isSatisfied;

        public Long getSearchResultId() { return searchResultId; }
        public void setSearchResultId(Long searchResultId) { this.searchResultId = searchResultId; }
        public Long getClickedScriptId() { return clickedScriptId; }
        public void setClickedScriptId(Long clickedScriptId) { this.clickedScriptId = clickedScriptId; }
        public Boolean getIsSatisfied() { return isSatisfied; }
        public void setIsSatisfied(Boolean isSatisfied) { this.isSatisfied = isSatisfied; }
    }

    // ========== 工具方法 ==========

    /**
     * 从 HTTP 请求提取用户 ID
     * 实际生产应从 SecurityContext 获取
     */
    private Long extractUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isBlank()) {
            userIdStr = "1"; // 测试默认值
        }
        return Long.parseLong(userIdStr);
    }
}
