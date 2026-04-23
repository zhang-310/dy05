package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.system.service.AlertEngineService;
import cn.gaifan.douyinOperations.module.system.service.DashboardDataService;
import cn.gaifan.douyinOperations.module.system.vo.AlertRecordVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("MonitoringController 集成测试")
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertEngineService alertEngineService;

    @MockBean
    private DashboardDataService dashboardDataService;

    @MockBean
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("获取实时指标 - 应返回 200")
    void getRealtimeMetrics_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/monitoring/metrics/realtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("搜索告警规则 - 应返回 200")
    void searchAlertRules_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        PageResultVO<AlertRuleVO> result = new PageResultVO<>();
        result.setTotal(1L);
        result.setList(List.of());

        when(alertEngineService.listAlertRules(anyInt(), anyInt()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/monitoring/alert-rules/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取告警规则详情 - 应返回 200")
    void getAlertRuleDetail_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ruleId", 1);

        AlertRuleVO rule = AlertRuleVO.builder()
                .id(1L)
                .name("测试规则")
                .build();

        when(alertEngineService.getAlertRule(anyLong()))
                .thenReturn(rule);

        mockMvc.perform(post("/api/v1/monitoring/alert-rules/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("创建告警规则 - 应返回 200")
    void createAlertRule_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "测试规则");
        body.put("metricName", "cpu_usage");
        body.put("threshold", 80.0);

        when(alertEngineService.createAlertRule(any()))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/monitoring/alert-rules/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("搜索告警记录 - 应返回 200")
    void searchAlerts_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        PageResultVO<AlertRecordVO> result = new PageResultVO<>();
        result.setTotal(1L);
        result.setList(List.of());

        when(alertEngineService.listAlertRecords(anyInt(), anyInt()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/monitoring/alerts/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取活跃告警 - 应返回 200")
    void getActiveAlerts_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("limit", 100);

        PageResultVO<AlertRecordVO> result = new PageResultVO<>();
        result.setTotal(0L);
        result.setList(List.of());

        when(alertEngineService.listAlertRecords(anyInt(), anyInt()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/monitoring/alerts/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取健康状态 - 应返回 200")
    void getHealthStatus_shouldReturn200() throws Exception {
        cn.gaifan.douyinOperations.module.system.vo.DashboardDataVO overview =
                new cn.gaifan.douyinOperations.module.system.vo.DashboardDataVO();

        when(dashboardDataService.getSystemOverview())
                .thenReturn(overview);

        mockMvc.perform(post("/api/v1/monitoring/health/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取仪表板数据 - 应返回 200")
    void getDashboardData_shouldReturn200() throws Exception {
        cn.gaifan.douyinOperations.module.system.vo.DashboardDataVO overview =
                new cn.gaifan.douyinOperations.module.system.vo.DashboardDataVO();
        cn.gaifan.douyinOperations.module.system.vo.DashboardDataVO alerts =
                new cn.gaifan.douyinOperations.module.system.vo.DashboardDataVO();

        when(dashboardDataService.getSystemOverview())
                .thenReturn(overview);
        when(dashboardDataService.getRealtimeAlerts())
                .thenReturn(alerts);

        mockMvc.perform(post("/api/v1/monitoring/dashboard/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取监控统计 - 应返回 200")
    void getMonitoringStatistics_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/monitoring/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("搜索日志 - 应返回 200")
    void searchLogs_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        mockMvc.perform(post("/api/v1/monitoring/logs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
