package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.module.script.service.VectorSearchService;
import cn.gaifan.douyinOperations.module.script.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("HybridSearchController 集成测试")
class HybridSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VectorSearchService vectorSearchService;

    @Test
    @DisplayName("混合搜索 - 应返回 200")
    void hybridSearch_shouldReturn200() throws Exception {
        HybridSearchRequestVO requestVO = new HybridSearchRequestVO();
        requestVO.setQuery("测试查询");
        requestVO.setPage(0);
        requestVO.setRows(10);

        HybridSearchResultVO resultVO = new HybridSearchResultVO();
        resultVO.setTotal(1L);

        when(vectorSearchService.hybridSearch(any(HybridSearchRequestVO.class), anyLong()))
                .thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/script/search/hybrid")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("语义搜索 - 应返回 200")
    void semanticSearch_shouldReturn200() throws Exception {
        HybridSearchController.SemanticSearchRequestVO requestVO = new HybridSearchController.SemanticSearchRequestVO();
        requestVO.setQuery("测试查询");
        requestVO.setTopK(20);

        HybridSearchResultVO resultVO = new HybridSearchResultVO();
        resultVO.setTotal(1L);

        when(vectorSearchService.semanticSearch(anyString(), anyInt(), anyLong(), anyBoolean()))
                .thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/script/search/semantic")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("BM25 关键词搜索 - 应返回 200")
    void lexicalSearch_shouldReturn200() throws Exception {
        HybridSearchController.LexicalSearchRequestVO requestVO = new HybridSearchController.LexicalSearchRequestVO();
        requestVO.setQuery("测试查询");
        requestVO.setTopK(20);

        HybridSearchResultVO resultVO = new HybridSearchResultVO();
        resultVO.setTotal(1L);

        when(vectorSearchService.lexicalSearch(anyString(), anyInt(), anyLong()))
                .thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/script/search/lexical")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("搜索建议 - 应返回 200")
    void getSearchSuggestions_shouldReturn200() throws Exception {
        HybridSearchController.SearchSuggestRequestVO requestVO = new HybridSearchController.SearchSuggestRequestVO();
        requestVO.setPrefix("测试");
        requestVO.setLimit(10);

        SearchSuggestionVO resultVO = new SearchSuggestionVO();

        when(vectorSearchService.getSearchSuggestions(anyString(), anyLong(), anyInt()))
                .thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/script/search/suggest")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("搜索分析 - 应返回 200")
    void getSearchAnalytics_shouldReturn200() throws Exception {
        HybridSearchController.SearchAnalyticsRequestVO requestVO = new HybridSearchController.SearchAnalyticsRequestVO();
        requestVO.setStartDate("2024-01-01");
        requestVO.setEndDate("2024-01-31");

        SearchAnalyticsVO analyticsVO = new SearchAnalyticsVO();

        when(vectorSearchService.getSearchAnalytics(anyLong(), anyString(), anyString()))
                .thenReturn(List.of(analyticsVO));

        mockMvc.perform(post("/api/v1/script/search/analytics")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("记录搜索点击反馈 - 应返回 200")
    void recordClickFeedback_shouldReturn200() throws Exception {
        HybridSearchController.ClickFeedbackRequestVO requestVO = new HybridSearchController.ClickFeedbackRequestVO();
        requestVO.setSearchResultId(1L);
        requestVO.setClickedScriptId(1L);
        requestVO.setIsSatisfied(true);

        doNothing().when(vectorSearchService).recordClickFeedback(anyLong(), anyLong(), anyBoolean());

        mockMvc.perform(post("/api/v1/script/search/feedback")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
