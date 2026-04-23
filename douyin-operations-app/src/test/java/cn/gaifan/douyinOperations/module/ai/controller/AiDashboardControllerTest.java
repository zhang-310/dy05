package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.service.AiDashboardService;
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
 * AiDashboardController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AiDashboardController 集成测试")
class AiDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiDashboardService aiDashboardService;

    @Test
    @DisplayName("仪表盘概览统计 - 应返回 200")
    void dashboardStats_shouldReturn200() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCalls", 10000);
        stats.put("totalUsers", 500);
        stats.put("avgLatency", 1200);

        when(aiDashboardService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(post("/api/v1/ai/admin/dashboard/stats")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalCalls").value(10000))
                .andExpect(jsonPath("$.data.totalUsers").value(500));
    }

    @Test
    @DisplayName("调用量趋势（按日）- 应返回 200")
    void callVolumeTrend_byDay_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 30);
        body.put("callType", "text_generation");

        Map<String, Object> trend1 = new HashMap<>();
        trend1.put("date", "2026-04-01");
        trend1.put("count", 100);

        when(aiDashboardService.getCallVolumeTrend(eq(30), eq("text_generation")))
                .thenReturn(List.of(trend1));

        mockMvc.perform(post("/api/v1/ai/admin/dashboard/call-volume-trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].date").value("2026-04-01"))
                .andExpect(jsonPath("$.data[0].count").value(100));
    }

    @Test
    @DisplayName("调用量趋势（按小时）- 应返回 200")
    void callVolumeTrend_byHour_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("hours", 24);

        Map<String, Object> trend1 = new HashMap<>();
        trend1.put("hour", "2026-04-07 14:00");
        trend1.put("count", 50);

        when(aiDashboardService.getCallVolumeTrendByHour(eq(24), isNull()))
                .thenReturn(List.of(trend1));

        mockMvc.perform(post("/api/v1/ai/admin/dashboard/call-volume-trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].hour").value("2026-04-07 14:00"))
                .andExpect(jsonPath("$.data[0].count").value(50));
    }

    @Test
    @DisplayName("调用量趋势（默认参数）- 应返回 200")
    void callVolumeTrend_defaults_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        when(aiDashboardService.getCallVolumeTrend(eq(30), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ai/admin/dashboard/call-volume-trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("调用量趋势（超出范围限制）- 应返回 200")
    void callVolumeTrend_outOfRange_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 200);

        when(aiDashboardService.getCallVolumeTrend(eq(90), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ai/admin/dashboard/call-volume-trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("额度使用趋势 - 应返回 200")
    void quotaTrend_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 30);

        Map<String, Object> trend1 = new HashMap<>();
        trend1.put("date", "2026-04-01");
        trend1.put("used", 500);
        trend1.put("limit", 1000);

        when(aiDashboardService.getQuotaTrend(eq(30)))
                .thenReturn(List.of(trend1));

        mockMvc.perform(post("/api/v1/ai/admin/dashboard/quota-trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].date").value("2026-04-01"))
                .andExpect(jsonPath("$.data[0].used").value(500));
    }

    @Test
    @DisplayName("额度使用趋势（默认参数）- 应返回 200")
    void quotaTrend_defaults_shouldReturn200() throws Exception {
        when(aiDashboardService.getQuotaTrend(eq(30)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ai/admin/dashboard/quota-trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("调用类型分布 - 应返回 200")
    void callTypeDistribution_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 30);

        Map<String, Object> dist1 = new HashMap<>();
        dist1.put("callType", "text_generation");
        dist1.put("count", 5000);
        dist1.put("percentage", 50.0);

        Map<String, Object> dist2 = new HashMap<>();
        dist2.put("callType", "image_generation");
        dist2.put("count", 3000);
        dist2.put("percentage", 30.0);

        when(aiDashboardService.getCallTypeDistribution(eq(30)))
                .thenReturn(List.of(dist1, dist2));

        mockMvc.perform(post("/api/v1/ai/admin/dashboard/call-type-distribution")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].callType").value("text_generation"))
                .andExpect(jsonPath("$.data[1].callType").value("image_generation"));
    }

    @Test
    @DisplayName("Token 用量拆解 - 应返回 200")
    void costBreakdown_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 14);

        Map<String, Object> row = new HashMap<>();
        row.put("callType", "kb_search");
        row.put("tokens", 12000L);
        row.put("calls", 40L);
        row.put("tokenSharePct", 100.0);

        when(aiDashboardService.getCostBreakdown(eq(14))).thenReturn(List.of(row));

        mockMvc.perform(post("/api/v1/ai/admin/dashboard/cost-breakdown")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].callType").value("kb_search"))
                .andExpect(jsonPath("$.data[0].tokens").value(12000));
    }

    @Test
    @DisplayName("非管理员访问 - 应返回 2002")
    void asUser_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/ai/admin/dashboard/stats")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2002")
    void withoutAuth_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/ai/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }
}
