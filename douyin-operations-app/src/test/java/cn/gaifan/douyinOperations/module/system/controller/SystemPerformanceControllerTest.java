package cn.gaifan.douyinOperations.module.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("SystemPerformanceController 集成测试")
class SystemPerformanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("获取当前指标 - 应返回 200")
    void getMetricsCurrent_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/metrics/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.timestamp").exists());
    }

    @Test
    @DisplayName("搜索指标 - 应返回 200")
    void getMetricsSearch_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("page", 0);
        request.put("rows", 10);

        mockMvc.perform(post("/api/v1/system/performance/metrics/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取 API 时序数据 - 应返回 200")
    void getApiTimeseries_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/api/timeseries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取查询分析 - 应返回 200")
    void getQueryAnalysis_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/query/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取慢查询 - 应返回 200")
    void getSlowQueries_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/query/slow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取 N+1 查询 - 应返回 200")
    void getNPlusOneQueries_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/query/n-plus-one")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取索引建议 - 应返回 200")
    void getIndexSuggestions_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/index/suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取缓存统计 - 应返回 200")
    void getCacheStatistics_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/cache/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.source").value("redis_info"))
                .andExpect(jsonPath("$.data.hitCount").exists())
                .andExpect(jsonPath("$.data.missCount").exists());
    }

    @Test
    @DisplayName("获取缓存热键 - 应返回 200")
    void getCacheHotKeys_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/cache/hot-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取缓存趋势 - 应返回 200")
    void getCacheTrend_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/cache/trend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("清除缓存 - 应返回 200")
    void clearCache_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/cache/clear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.clearedCount").value(0));
    }

    @Test
    @DisplayName("重建缓存 - 应返回 200")
    void rebuildCache_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/cache/rebuild")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("导出性能数据 - 应返回 200")
    void exportPerformance_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("运行基准测试 - 应返回 200")
    void runBenchmark_shouldReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/system/performance/benchmark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("ready"))
                .andExpect(jsonPath("$.data.source").value("current_runtime_snapshot"));
    }
}
