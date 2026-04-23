package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.service.ScriptOptimizationService;
import cn.gaifan.douyinOperations.module.product.vo.OptimizationSuggestionVO;
import cn.gaifan.douyinOperations.module.product.vo.RegeneratedScriptVO;
import cn.gaifan.douyinOperations.module.product.vo.ScriptAnalysisResultVO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScriptOptimizationController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ScriptOptimizationController 集成测试")
class ScriptOptimizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScriptOptimizationService scriptOptimizationService;

    @Test
    @DisplayName("分析话术效果 - 应返回 200")
    void analyzeScript_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptVersionId", 1L);
        body.put("dataSource", "LIVE_MONITOR");
        body.put("analysisType", "COMPREHENSIVE");

        ScriptAnalysisResultVO result = new ScriptAnalysisResultVO();
        result.setId(1L);

        when(scriptOptimizationService.analyzeScript(eq(1L), eq("LIVE_MONITOR"), eq("COMPREHENSIVE"), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/product/script/analyze")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取优化建议 - 应返回 200")
    void getOptimizationSuggestions_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptVersionId", 1L);
        body.put("analysisResultId", 1L);
        body.put("topN", 10);

        when(scriptOptimizationService.getOptimizationSuggestions(eq(1L), eq(1L), eq(10), eq(1L)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/script/suggestions")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("重新生成话术 - 应返回 200")
    void regenerateOptimized_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptVersionId", 1L);
        body.put("suggestionId", 1L);
        body.put("generationStyles", List.of("FRIENDLY", "HUMOROUS"));

        when(scriptOptimizationService.regenerateScript(eq(1L), eq(1L), anyList(), eq(1L)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/script/regenerate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("查询优化历史 - 应返回 200")
    void optimizationHistory_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptVersionId", 1L);
        body.put("page", 0);
        body.put("rows", 30);

        PageResultVO<ScriptAnalysisResultVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(5L);
        pageResult.setList(List.of());

        when(scriptOptimizationService.getOptimizationHistory(eq(1L), eq(0), eq(30), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/product/script/optimization-history")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(5));
    }
}
