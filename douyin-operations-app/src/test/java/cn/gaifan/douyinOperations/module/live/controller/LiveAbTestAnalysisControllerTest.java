package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.service.LiveAbTestAnalysisService;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveAbTestAnalysisController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveAbTestAnalysisController 集成测试")
class LiveAbTestAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveAbTestAnalysisService abTestAnalysisService;

    @Test
    @DisplayName("记录 A/B 测试结果 - 应返回 200")
    void record_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("experimentKey", "style_test_001");
        body.put("sessionId", 100L);
        body.put("variant", "A");
        body.put("style", "专业");
        body.put("effectivenessScore", 85.5);
        body.put("conversionRate", 12.3);
        body.put("interactionRate", 45.6);
        body.put("sampleSize", 1000);
        body.put("confidence", 95.0);

        doNothing().when(abTestAnalysisService).recordResult(
                eq(100L),
                eq("style_test_001"),
                eq("A"),
                eq("专业"),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                eq(1000),
                any(BigDecimal.class),
                eq(1L)
        );

        mockMvc.perform(post("/api/v1/live/ab-analysis/record")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("记录 A/B 测试结果（缺少 experimentKey）- 应返回 1001")
    void record_withoutExperimentKey_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        mockMvc.perform(post("/api/v1/live/ab-analysis/record")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 experimentKey"));
    }

    @Test
    @DisplayName("记录 A/B 测试结果（缺少 sessionId）- 应返回 1001")
    void record_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("experimentKey", "style_test_001");

        mockMvc.perform(post("/api/v1/live/ab-analysis/record")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 sessionId"));
    }

    @Test
    @DisplayName("获取推荐话术风格 - 应返回 200")
    void recommend_shouldReturn200() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("recommendedStyle", "专业");
        result.put("confidence", 95.0);
        result.put("avgEffectiveness", 85.5);

        when(abTestAnalysisService.getRecommendedStyle(eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/live/ab-analysis/recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.recommendedStyle").value("专业"))
                .andExpect(jsonPath("$.data.confidence").value(95.0));
    }

    @Test
    @DisplayName("获取实验摘要 - 应返回 200")
    void summary_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("experimentKey", "style_test_001");

        Map<String, Object> summaryItem = new HashMap<>();
        summaryItem.put("variant", "A");
        summaryItem.put("style", "专业");
        summaryItem.put("avgEffectiveness", 85.5);
        summaryItem.put("sampleSize", 1000);

        when(abTestAnalysisService.getExperimentSummary(eq("style_test_001")))
                .thenReturn(List.of(summaryItem));

        mockMvc.perform(post("/api/v1/live/ab-analysis/summary")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].variant").value("A"))
                .andExpect(jsonPath("$.data[0].style").value("专业"));
    }

    @Test
    @DisplayName("获取实验摘要（缺少 experimentKey）- 应返回 1001")
    void summary_withoutExperimentKey_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/ab-analysis/summary")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 experimentKey"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("experimentKey", "style_test_001");
        body.put("sessionId", 100L);

        mockMvc.perform(post("/api/v1/live/ab-analysis/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
