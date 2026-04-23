package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.system.service.AlertEngineService;
import cn.gaifan.douyinOperations.module.system.service.DashboardDataService;
import cn.gaifan.douyinOperations.module.system.vo.AlertRecordVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleVO;
import cn.gaifan.douyinOperations.module.system.vo.DashboardDataVO;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AlertController 集成测试")
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertEngineService alertEngineService;

    @MockBean
    private DashboardDataService dashboardDataService;

    @Test
    @DisplayName("创建告警规则 - 应返回 200")
    void createAlertRule_shouldReturn200() throws Exception {
        AlertRuleVO vo = AlertRuleVO.builder()
                .name("CPU告警")
                .metricName("cpu_usage")
                .type("threshold")
                .threshold(80.0)
                .operator(">")
                .duration(300)
                .severity("high")
                .enabled(true)
                .build();

        when(alertEngineService.createAlertRule(any(AlertRuleVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/system/alert/rule/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("更新告警规则 - 应返回 200")
    void updateAlertRule_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("ruleId", 1L);
        request.put("name", "CPU告警");
        request.put("metricName", "cpu_usage");
        request.put("type", "threshold");
        request.put("threshold", 85.0);
        request.put("operator", ">");
        request.put("duration", 300);
        request.put("severity", "high");
        request.put("enabled", true);

        doNothing().when(alertEngineService).updateAlertRule(eq(1L), any(AlertRuleVO.class));

        mockMvc.perform(post("/api/v1/system/alert/rule/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除告警规则 - 应返回 200")
    void deleteAlertRule_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("ruleId", 1L);

        doNothing().when(alertEngineService).deleteAlertRule(eq(1L));

        mockMvc.perform(post("/api/v1/system/alert/rule/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取告警规则详情 - 应返回 200")
    void getAlertRule_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("ruleId", 1L);

        AlertRuleVO rule = AlertRuleVO.builder()
                .name("CPU告警")
                .metricName("cpu_usage")
                .threshold(80.0)
                .build();

        when(alertEngineService.getAlertRule(eq(1L))).thenReturn(rule);

        mockMvc.perform(post("/api/v1/system/alert/rule/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("CPU告警"));
    }

    @Test
    @DisplayName("分页查询告警规则 - 应返回 200")
    void listAlertRules_shouldReturn200() throws Exception {
        Map<String, Integer> request = new HashMap<>();
        request.put("page", 0);
        request.put("rows", 10);

        PageResultVO<AlertRuleVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of());

        when(alertEngineService.listAlertRules(eq(0), eq(10))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/system/alert/rule/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("启用告警规则 - 应返回 200")
    void enableAlertRule_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("ruleId", 1L);

        doNothing().when(alertEngineService).enableAlertRule(eq(1L));

        mockMvc.perform(post("/api/v1/system/alert/rule/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("禁用告警规则 - 应返回 200")
    void disableAlertRule_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("ruleId", 1L);

        doNothing().when(alertEngineService).disableAlertRule(eq(1L));

        mockMvc.perform(post("/api/v1/system/alert/rule/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取告警记录详情 - 应返回 200")
    void getAlertRecord_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("recordId", 1L);

        AlertRecordVO record = AlertRecordVO.builder()
                .id(1L)
                .ruleId(1L)
                .status("triggered")
                .build();

        when(alertEngineService.getAlertRecord(eq(1L))).thenReturn(record);

        mockMvc.perform(post("/api/v1/system/alert/record/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("分页查询告警记录 - 应返回 200")
    void listAlertRecords_shouldReturn200() throws Exception {
        Map<String, Integer> request = new HashMap<>();
        request.put("page", 0);
        request.put("rows", 10);

        PageResultVO<AlertRecordVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of());

        when(alertEngineService.listAlertRecords(eq(0), eq(10))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/system/alert/record/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取系统整体统计 - 应返回 200")
    void getSystemOverview_shouldReturn200() throws Exception {
        DashboardDataVO overview = DashboardDataVO.builder()
                .title("系统概览")
                .data(Map.of("totalUsers", 100, "activeUsers", 80))
                .type("overview")
                .timestamp(System.currentTimeMillis())
                .build();

        when(dashboardDataService.getSystemOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/v1/system/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.title").value("系统概览"));
    }

    @Test
    @DisplayName("获取实时告警数据 - 应返回 200")
    void getRealtimeAlerts_shouldReturn200() throws Exception {
        DashboardDataVO alerts = DashboardDataVO.builder()
                .title("实时告警")
                .data(Map.of("critical", 2, "warning", 5))
                .type("table")
                .timestamp(System.currentTimeMillis())
                .build();

        when(dashboardDataService.getRealtimeAlerts()).thenReturn(alerts);

        mockMvc.perform(get("/api/v1/system/dashboard/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.title").value("实时告警"));
    }

    @Test
    @DisplayName("获取性能指标趋势 - 应返回 200")
    void getPerformanceTrends_shouldReturn200() throws Exception {
        DashboardDataVO trends = DashboardDataVO.builder()
                .title("性能趋势")
                .data(Map.of("cpu", List.of(50, 60, 70)))
                .type("chart")
                .timestamp(System.currentTimeMillis())
                .build();

        when(dashboardDataService.getPerformanceTrends()).thenReturn(trends);

        mockMvc.perform(get("/api/v1/system/dashboard/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.title").value("性能趋势"));
    }

    @Test
    @DisplayName("获取日志聚合统计 - 应返回 200")
    void getLogStatistics_shouldReturn200() throws Exception {
        DashboardDataVO logs = DashboardDataVO.builder()
                .title("日志统计")
                .data(Map.of("totalLogs", 10000))
                .type("overview")
                .timestamp(System.currentTimeMillis())
                .build();

        when(dashboardDataService.getLogStatistics()).thenReturn(logs);

        mockMvc.perform(get("/api/v1/system/dashboard/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.title").value("日志统计"));
    }

    @Test
    @DisplayName("获取链路追踪摘要 - 应返回 200")
    void getTracesSummary_shouldReturn200() throws Exception {
        DashboardDataVO traces = DashboardDataVO.builder()
                .title("链路追踪")
                .data(Map.of("totalTraces", 5000))
                .type("overview")
                .timestamp(System.currentTimeMillis())
                .build();

        when(dashboardDataService.getTracesSummary()).thenReturn(traces);

        mockMvc.perform(get("/api/v1/system/dashboard/traces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.title").value("链路追踪"));
    }

    @Test
    @DisplayName("获取健康检查状态 - 应返回 200")
    void getHealthStatus_shouldReturn200() throws Exception {
        DashboardDataVO health = DashboardDataVO.builder()
                .title("健康检查")
                .data(Map.of("status", "UP"))
                .type("status")
                .timestamp(System.currentTimeMillis())
                .build();

        when(dashboardDataService.getHealthStatus()).thenReturn(health);

        mockMvc.perform(get("/api/v1/system/dashboard/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.title").value("健康检查"));
    }
}
