package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.system.service.SystemService;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("SystemController 集成测试")
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SystemService systemService;

    @Test
    @DisplayName("API调用日志列表 - 应返回 200")
    void apiLogList_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("module", "live");
        body.put("page", 0);
        body.put("rows", 10);

        Map<String, Object> log = new HashMap<>();
        log.put("id", 1L);
        log.put("module", "live");
        log.put("apiName", "/api/v1/live/list");

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(log));

        when(systemService.searchApiLogs(eq("live"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/system/api-log/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("API调用日志列表（非管理员）- 应返回 2002")
    void apiLogList_nonAdmin_shouldReturn2002() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/api-log/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("API调用日志列表（未登录）- 应返回 2001")
    void apiLogList_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/api-log/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("API调用统计 - 应返回 200")
    void apiLogStats_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("module", "live");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCalls", 1000);
        stats.put("successRate", 0.95);

        when(systemService.getApiLogStats(eq("live"), isNull(), isNull()))
                .thenReturn(stats);

        mockMvc.perform(post("/api/v1/system/api-log/stats")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalCalls").value(1000));
    }

    @Test
    @DisplayName("API调用统计（未登录）- 应返回 2001")
    void apiLogStats_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/api-log/stats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("API调用日志详情 - 应返回 200")
    void apiLogGet_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        Map<String, Object> log = new HashMap<>();
        log.put("id", 1L);
        log.put("module", "live");
        log.put("requestBody", "{\"page\":0}");

        when(systemService.getApiLogById(eq(1L))).thenReturn(log);

        mockMvc.perform(post("/api/v1/system/api-log/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("API调用日志详情（缺少 id）- 应返回 1001")
    void apiLogGet_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/api-log/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("API调用日志详情（记录不存在）- 应返回 1005")
    void apiLogGet_notFound_shouldReturn1005() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 999L);

        when(systemService.getApiLogById(eq(999L))).thenReturn(null);

        mockMvc.perform(post("/api/v1/system/api-log/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1005));
    }

    @Test
    @DisplayName("数据同步日志列表 - 应返回 200")
    void syncLogList_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("syncType", "douyin");
        body.put("page", 0);
        body.put("rows", 10);

        Map<String, Object> log = new HashMap<>();
        log.put("id", 1L);
        log.put("syncType", "douyin");
        log.put("status", "success");

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(log));

        when(systemService.searchSyncLogs(eq("douyin"), isNull(), isNull(),
                isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/system/sync-log/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("数据同步日志列表（未登录）- 应返回 2001")
    void syncLogList_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/sync-log/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("系统健康检查 - 应返回 200")
    void health_shouldReturn200() throws Exception {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "UP");
        health.put("redis", "UP");

        when(systemService.checkHealth()).thenReturn(health);

        mockMvc.perform(post("/api/v1/system/health")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("系统健康检查（未登录）- 应返回 2001")
    void health_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("系统运行信息 - 应返回 200")
    void info_shouldReturn200() throws Exception {
        Map<String, Object> info = new HashMap<>();
        info.put("javaVersion", "17");
        info.put("uptime", 3600000L);

        when(systemService.getSystemInfo()).thenReturn(info);

        mockMvc.perform(post("/api/v1/system/info")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.javaVersion").value("17"));
    }

    @Test
    @DisplayName("系统运行信息（未登录）- 应返回 2001")
    void info_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
