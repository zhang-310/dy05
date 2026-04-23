package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.service.EffectivenessScoreService;
import cn.gaifan.douyinOperations.module.product.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EffectivenessScoreController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("EffectivenessScoreController 集成测试")
class EffectivenessScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EffectivenessScoreService scoreService;

    @Test
    @DisplayName("获取排行榜 - 应返回 200")
    void getRanking_shouldReturn200() throws Exception {
        PageResultVO<ScriptRankingVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(10L);
        pageResult.setList(List.of());

        when(scoreService.getRanking(eq(1L), eq(10), eq("score"), eq(0), eq(30), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/product/script-effectiveness/ranking")
                        .param("productId", "1")
                        .param("topN", "10")
                        .param("sortBy", "score")
                        .param("page", "0")
                        .param("rows", "30")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("版本对比 - 应返回 200")
    void compareVersions_shouldReturn200() throws Exception {
        ScriptComparisonVO comparison = new ScriptComparisonVO();

        when(scoreService.compareVersions(anyList(), eq(1L))).thenReturn(comparison);

        mockMvc.perform(post("/api/v1/product/script-effectiveness/compare")
                        .param("versionIds", "1", "2", "3")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取历史趋势 - 应返回 200")
    void getTrend_shouldReturn200() throws Exception {
        ScriptTrendVO trend = new ScriptTrendVO();

        when(scoreService.getTrend(eq(1L), eq(30), eq(1L))).thenReturn(trend);

        mockMvc.perform(post("/api/v1/product/script-effectiveness/trend")
                        .param("versionId", "1")
                        .param("days", "30")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("重新计算评分 - 应返回 200")
    void recalculate_shouldReturn200() throws Exception {
        when(scoreService.recalculateAllScores(eq(1L), eq(1L))).thenReturn(5);

        mockMvc.perform(post("/api/v1/product/script-effectiveness/recalculate")
                        .param("productId", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("风格对比 - 应返回 200")
    void getStyleComparison_shouldReturn200() throws Exception {
        ScriptComparisonVO comparison = new ScriptComparisonVO();

        when(scoreService.getStyleComparison(eq(1L), eq(1L))).thenReturn(comparison);

        mockMvc.perform(post("/api/v1/product/script-effectiveness/style-comparison")
                        .param("productId", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取效果分析 - 应返回 200")
    void getAnalysis_shouldReturn200() throws Exception {
        ScriptEffectivenessAnalysisVO analysis = new ScriptEffectivenessAnalysisVO();

        when(scoreService.getAnalysis(eq(1L), eq(1L))).thenReturn(analysis);

        mockMvc.perform(post("/api/v1/product/script-effectiveness/analysis")
                        .param("versionId", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("记录快照 - 应返回 200")
    void recordSnapshot_shouldReturn200() throws Exception {
        when(scoreService.recordSnapshot(eq(1L), eq(1L))).thenReturn(true);

        mockMvc.perform(post("/api/v1/product/script-effectiveness/record-snapshot")
                        .param("versionId", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("清除缓存 - 应返回 200")
    void clearCache_shouldReturn200() throws Exception {
        when(scoreService.clearComparisonCache(eq(1L), eq(1L))).thenReturn(3);

        mockMvc.perform(post("/api/v1/product/script-effectiveness/clear-cache")
                        .param("productId", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(3));
    }
}
