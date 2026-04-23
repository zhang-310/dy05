package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.service.EffectivenessScoreService;
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
    private EffectivenessScoreService effectivenessScoreService;

    @Test
    @DisplayName("计算话术效果评分 - 应返回 200")
    void calculateScore_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 1L);
        body.put("sessionId", 100L);

        Map<String, Object> result = new HashMap<>();
        result.put("scriptId", 1L);
        result.put("score", 85.5);
        result.put("conversionRate", 0.12);
        result.put("interactionCount", 150);

        when(effectivenessScoreService.calculateScore(eq(1L), eq(100L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/effectiveness/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptId").value(1))
                .andExpect(jsonPath("$.data.score").value(85.5));
    }

    @Test
    @DisplayName("计算话术效果评分（缺少参数）- 应返回 1001")
    void calculateScore_withoutParams_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 1L);

        mockMvc.perform(post("/api/v1/live/effectiveness/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("scriptId 和 sessionId 不能为空"));
    }

    @Test
    @DisplayName("计算场次排行榜 - 应返回 200")
    void calculateSessionRanking_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        Map<String, Object> script1 = new HashMap<>();
        script1.put("scriptId", 1L);
        script1.put("rank", 1);
        script1.put("score", 90.0);

        Map<String, Object> script2 = new HashMap<>();
        script2.put("scriptId", 2L);
        script2.put("rank", 2);
        script2.put("score", 85.0);

        when(effectivenessScoreService.calculateSessionRanking(eq(100L)))
                .thenReturn(List.of(script1, script2));

        mockMvc.perform(post("/api/v1/live/effectiveness/session-ranking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[1].rank").value(2));
    }

    @Test
    @DisplayName("版本对比 - 应返回 200")
    void compareVersions_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("versionA", 1L);
        body.put("versionB", 2L);

        Map<String, Object> result = new HashMap<>();
        result.put("versionA", 1L);
        result.put("versionB", 2L);
        result.put("scoreA", 80.0);
        result.put("scoreB", 85.0);
        result.put("improvement", 5.0);

        when(effectivenessScoreService.compareVersions(eq(1L), eq(2L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/effectiveness/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.improvement").value(5.0));
    }

    @Test
    @DisplayName("获取排行榜 - 应返回 200")
    void getRanking_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("page", 0);
        body.put("pageSize", 10);

        Map<String, Object> script1 = new HashMap<>();
        script1.put("scriptId", 1L);
        script1.put("score", 90.0);

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(script1));

        when(effectivenessScoreService.getRanking(eq(100L), eq(0), eq(10)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/effectiveness/ranking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("获取热门话术 - 应返回 200")
    void getTopScripts_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("limit", 3);

        Map<String, Object> script1 = new HashMap<>();
        script1.put("scriptId", 1L);
        script1.put("score", 95.0);

        when(effectivenessScoreService.getTopScripts(eq(100L), eq(3)))
                .thenReturn(List.of(script1));

        mockMvc.perform(post("/api/v1/live/effectiveness/top-scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].score").value(95.0));
    }

    @Test
    @DisplayName("获取推荐话术 - 应返回 200")
    void getRecommendedScripts_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        Map<String, Object> script1 = new HashMap<>();
        script1.put("scriptId", 1L);
        script1.put("recommended", true);

        when(effectivenessScoreService.getRecommendedScripts(eq(100L)))
                .thenReturn(List.of(script1));

        mockMvc.perform(post("/api/v1/live/effectiveness/recommended-scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].recommended").value(true));
    }

    @Test
    @DisplayName("获取新兴话术 - 应返回 200")
    void getEmergedScripts_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        Map<String, Object> script1 = new HashMap<>();
        script1.put("scriptId", 1L);
        script1.put("emerging", true);

        when(effectivenessScoreService.getEmergedScripts(eq(100L)))
                .thenReturn(List.of(script1));

        mockMvc.perform(post("/api/v1/live/effectiveness/emerged-scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].emerging").value(true));
    }

    @Test
    @DisplayName("获取话术效果详情 - 应返回 200")
    void getScriptEffectiveness_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 1L);

        Map<String, Object> result = new HashMap<>();
        result.put("scriptId", 1L);
        result.put("score", 88.0);
        result.put("usageCount", 50);

        when(effectivenessScoreService.getScriptEffectiveness(eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/effectiveness/script-effectiveness")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.score").value(88.0));
    }
}
