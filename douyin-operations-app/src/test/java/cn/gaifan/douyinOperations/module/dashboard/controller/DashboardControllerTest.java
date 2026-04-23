package cn.gaifan.douyinOperations.module.dashboard.controller;

import cn.gaifan.douyinOperations.module.dashboard.service.DashboardGmvService;
import cn.gaifan.douyinOperations.module.dashboard.service.DashboardService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DashboardController 集成测试")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private DashboardGmvService dashboardGmvService;

    @Test
    @DisplayName("获取管理员统计 - 应返回 200")
    void getAdminStats_shouldReturn200() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 100);

        when(dashboardService.getAdminStats())
                .thenReturn(stats);

        mockMvc.perform(post("/api/v1/dashboard/admin/stats")
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取机构统计 - 应返回 200")
    void getOrgStats_shouldReturn200() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLives", 50);

        when(dashboardService.getOrgStats(anyLong()))
                .thenReturn(stats);

        mockMvc.perform(post("/api/v1/dashboard/org/stats")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取机构统计（未登录）- 应返回 2001")
    void getOrgStats_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/org/stats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("统一 KPI 综合指标 - 应返回 200")
    void getUnifiedKpi_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("lookbackDays", 30);

        Map<String, Object> kpi = new HashMap<>();
        kpi.put("totalGmv", 1000000);

        when(dashboardGmvService.getUnifiedKpi(anyLong(), anyInt()))
                .thenReturn(kpi);

        mockMvc.perform(post("/api/v1/dashboard/kpi-unified")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("直播形式 GMV 分布 - 应返回 200")
    void getLiveFormatGmv_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("lookbackDays", 30);

        Map<String, Object> gmv = new HashMap<>();
        gmv.put("formats", Map.of());

        when(dashboardGmvService.getLiveFormatGmv(anyLong(), anyInt()))
                .thenReturn(gmv);

        mockMvc.perform(post("/api/v1/dashboard/live-format-gmv")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("商品 GMV 汇总 - 应返回 200")
    void getProductGmvSummary_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("lookbackDays", 30);

        Map<String, Object> summary = new HashMap<>();
        summary.put("products", Map.of());

        when(dashboardGmvService.getProductGmvSummary(anyLong(), anyInt()))
                .thenReturn(summary);

        mockMvc.perform(post("/api/v1/dashboard/product-gmv-summary")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("驾驶舱场次预览 - 应返回 200")
    void getCockpitPreview_shouldReturn200() throws Exception {
        Map<String, Object> preview = new HashMap<>();
        preview.put("sessions", Map.of());

        when(dashboardGmvService.getCockpitPreview(anyLong(), any()))
                .thenReturn(preview);

        mockMvc.perform(post("/api/v1/dashboard/cockpit-preview")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("利润矩阵预览 - 应返回 200")
    void getProfitMatrixPreview_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("lookbackDays", 30);

        Map<String, Object> matrix = new HashMap<>();
        matrix.put("data", Map.of());

        when(dashboardGmvService.getProfitMatrixPreview(anyLong(), anyInt()))
                .thenReturn(matrix);

        mockMvc.perform(post("/api/v1/dashboard/profit-matrix-preview")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("转化漏斗 - 应返回 200")
    void getConversionFunnel_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("lookbackDays", 30);

        Map<String, Object> funnel = new HashMap<>();
        funnel.put("stages", Map.of());

        when(dashboardGmvService.getConversionFunnel(anyLong(), anyInt()))
                .thenReturn(funnel);

        mockMvc.perform(post("/api/v1/dashboard/conversion-funnel")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("驾驶舱 CSV 导出 - 应返回 200")
    void exportCockpitCsv_shouldReturn200() throws Exception {
        Map<String, Object> export = new HashMap<>();
        export.put("url", "http://example.com/export.csv");

        when(dashboardGmvService.exportCockpitCsv(anyLong(), any()))
                .thenReturn(export);

        mockMvc.perform(post("/api/v1/dashboard/cockpit-export")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
