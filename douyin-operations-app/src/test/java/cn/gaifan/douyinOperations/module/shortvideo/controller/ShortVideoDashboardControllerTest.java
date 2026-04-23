package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoDashboardService;
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
 * ShortVideoDashboardController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoDashboardController 集成测试")
class ShortVideoDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "shortVideoDashboardServiceImpl")
    private ShortVideoDashboardService dashboardService;

    @Test
    @DisplayName("数据概览 - 应返回 200")
    void stats_shouldReturn200() throws Exception {
        Map<String, Object> stats = Map.of(
                "totalVideos", 100,
                "totalViews", 50000,
                "avgViews", 500,
                "totalProjects", 10
        );

        when(dashboardService.getStats(eq(1L))).thenReturn(stats);

        mockMvc.perform(post("/api/v1/short-video/dashboard/stats")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalVideos").value(100))
                .andExpect(jsonPath("$.data.totalViews").value(50000));
    }

    @Test
    @DisplayName("播放量趋势 - 应返回 200")
    void trend_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 7);

        List<Map<String, Object>> trend = List.of(
                Map.of("date", "2026-04-01", "views", 1000),
                Map.of("date", "2026-04-02", "views", 1200)
        );

        when(dashboardService.getTrend(eq(1L), eq(7))).thenReturn(trend);

        mockMvc.perform(post("/api/v1/short-video/dashboard/trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].date").value("2026-04-01"));
    }

    @Test
    @DisplayName("我的项目 - 应返回 200")
    void projects_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "in_progress");
        body.put("page", 0);
        body.put("rows", 10);

        List<Map<String, Object>> projects = List.of(
                Map.of("id", 1L, "name", "项目1", "progress", 0.5),
                Map.of("id", 2L, "name", "项目2", "progress", 0.8)
        );

        when(dashboardService.getProjectsWithProgress(eq(1L), eq("in_progress"), eq(0), eq(10)))
                .thenReturn(projects);

        mockMvc.perform(post("/api/v1/short-video/dashboard/projects")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("成本分解 - 应返回 200")
    void costBreakdown_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);

        Map<String, Object> cost = Map.of(
                "totalCost", 1000.0,
                "imageCost", 300.0,
                "videoCost", 500.0,
                "audioCost", 200.0
        );

        when(dashboardService.getCostBreakdown(eq(1L), eq(1L))).thenReturn(cost);

        mockMvc.perform(post("/api/v1/short-video/dashboard/cost-breakdown")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalCost").value(1000.0));
    }

    @Test
    @DisplayName("数据概览（未登录）- 应返回 2001")
    void stats_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
