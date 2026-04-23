package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.module.shortvideo.service.PublishFeedbackService;
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
 * ShortVideoFeedbackController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoFeedbackController 集成测试")
class ShortVideoFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PublishFeedbackService publishFeedbackService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @Test
    @DisplayName("分析视频发布效果 - 应返回 200")
    void analyzePerformance_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoId", 1L);

        PublishFeedbackService.ContentScore score = new PublishFeedbackService.ContentScore(
                85.5, 0.75, 0.12, 0.05, 1.5, 0.95, "good", List.of("完播率高于平均", "互动率良好")
        );

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(publishFeedbackService.analyzePerformance(eq(1L), anyList())).thenReturn(score);

        mockMvc.perform(post("/api/v1/short-video/feedback/analyze-performance")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.overallScore").value(85.5))
                .andExpect(jsonPath("$.data.completionRate").value(0.75))
                .andExpect(jsonPath("$.data.performance").value("good"));
    }

    @Test
    @DisplayName("生成视频反思报告 - 应返回 200")
    void reflectionReport_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoId", 1L);

        Map<String, Object> report = Map.of(
                "videoId", 1L,
                "summary", "本视频表现良好",
                "strengths", List.of("标题吸引人", "内容质量高"),
                "weaknesses", List.of("发布时间不够理想"),
                "suggestions", List.of("建议在晚上8点发布")
        );

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(publishFeedbackService.generateReflectionReport(eq(1L), anyList())).thenReturn(report);

        mockMvc.perform(post("/api/v1/short-video/feedback/reflection-report")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoId").value(1))
                .andExpect(jsonPath("$.data.summary").value("本视频表现良好"));
    }

    @Test
    @DisplayName("生成本周发布周报 - 应返回 200")
    void weeklyReport_shouldReturn200() throws Exception {
        Map<String, Object> report = Map.of(
                "weekStart", "2026-04-01",
                "weekEnd", "2026-04-07",
                "totalVideos", 10,
                "avgViews", 5000,
                "topVideo", Map.of("id", 1L, "title", "最佳视频")
        );

        when(publishFeedbackService.generateWeeklyReport(eq(1L))).thenReturn(report);

        mockMvc.perform(post("/api/v1/short-video/feedback/weekly-report")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalVideos").value(10))
                .andExpect(jsonPath("$.data.avgViews").value(5000));
    }

    @Test
    @DisplayName("分析视频发布效果（缺少 videoId）- 应返回 1001")
    void analyzePerformance_missingVideoId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/feedback/analyze-performance")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("生成视频反思报告（未登录）- 应返回 2001")
    void reflectionReport_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoId", 1L);

        mockMvc.perform(post("/api/v1/short-video/feedback/reflection-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
