package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.QualityDashboardService;
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
 * QualityDashboardController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("QualityDashboardController 集成测试")
class QualityDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QualityDashboardService qualityDashboardService;

    @Test
    @DisplayName("概览 - 应返回 200")
    void overview_shouldReturn200() throws Exception {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalVideos", 100);
        overview.put("avgQualityScore", 85.5);
        overview.put("passRate", 0.92);

        when(qualityDashboardService.getOverview(eq(1L))).thenReturn(overview);

        mockMvc.perform(post("/api/v1/short-video/quality-dashboard/overview")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalVideos").value(100))
                .andExpect(jsonPath("$.data.avgQualityScore").value(85.5));
    }

    @Test
    @DisplayName("概览（未登录）- 应返回 2001")
    void overview_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/quality-dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("质量趋势 - 应返回 200")
    void trend_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 30);

        Map<String, Object> trend1 = new HashMap<>();
        trend1.put("date", "2026-04-01");
        trend1.put("avgScore", 85.0);

        Map<String, Object> trend2 = new HashMap<>();
        trend2.put("date", "2026-04-02");
        trend2.put("avgScore", 87.0);

        when(qualityDashboardService.getQualityTrend(eq(1L), eq(30)))
                .thenReturn(List.of(trend1, trend2));

        mockMvc.perform(post("/api/v1/short-video/quality-dashboard/trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].avgScore").value(85.0));
    }

    @Test
    @DisplayName("质量趋势（默认天数）- 应返回 200")
    void trend_defaultDays_shouldReturn200() throws Exception {
        when(qualityDashboardService.getQualityTrend(eq(1L), eq(30)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/short-video/quality-dashboard/trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("质量趋势（天数超限）- 应返回 200 并使用默认值")
    void trend_invalidDays_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 100);

        when(qualityDashboardService.getQualityTrend(eq(1L), eq(30)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/short-video/quality-dashboard/trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("模型排名 - 应返回 200")
    void modelRanking_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 30);

        Map<String, Object> model1 = new HashMap<>();
        model1.put("modelName", "kling");
        model1.put("avgScore", 90.0);
        model1.put("count", 50);

        Map<String, Object> model2 = new HashMap<>();
        model2.put("modelName", "runway");
        model2.put("avgScore", 88.0);
        model2.put("count", 45);

        when(qualityDashboardService.getModelRanking(eq(1L), eq(30)))
                .thenReturn(List.of(model1, model2));

        mockMvc.perform(post("/api/v1/short-video/quality-dashboard/model-ranking")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].modelName").value("kling"))
                .andExpect(jsonPath("$.data[0].avgScore").value(90.0));
    }

    @Test
    @DisplayName("运镜排名 - 应返回 200")
    void cameraRanking_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 30);

        Map<String, Object> camera1 = new HashMap<>();
        camera1.put("cameraType", "push_in");
        camera1.put("avgScore", 92.0);
        camera1.put("count", 30);

        Map<String, Object> camera2 = new HashMap<>();
        camera2.put("cameraType", "pan");
        camera2.put("avgScore", 89.0);
        camera2.put("count", 25);

        when(qualityDashboardService.getCameraRanking(eq(1L), eq(30)))
                .thenReturn(List.of(camera1, camera2));

        mockMvc.perform(post("/api/v1/short-video/quality-dashboard/camera-ranking")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].cameraType").value("push_in"))
                .andExpect(jsonPath("$.data[0].avgScore").value(92.0));
    }

    @Test
    @DisplayName("AI 反思 - 应返回 200")
    void aiReflections_shouldReturn200() throws Exception {
        List<String> reflections = List.of(
                "模型 kling 在人物特写场景表现优异",
                "运镜 push_in 配合产品展示效果最佳",
                "建议增加场景转换的流畅度"
        );

        when(qualityDashboardService.getAiReflections(eq(1L))).thenReturn(reflections);

        mockMvc.perform(post("/api/v1/short-video/quality-dashboard/ai-reflections")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("模型 kling 在人物特写场景表现优异"));
    }

    @Test
    @DisplayName("AI 反思（未登录）- 应返回 2001")
    void aiReflections_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/quality-dashboard/ai-reflections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
