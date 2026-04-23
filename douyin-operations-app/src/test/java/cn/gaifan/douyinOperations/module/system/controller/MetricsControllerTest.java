package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.module.system.service.MetricsCollectorService;
import cn.gaifan.douyinOperations.module.system.vo.MetricsVO;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("MetricsController 集成测试")
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MetricsCollectorService metricsCollectorService;

    @Test
    @DisplayName("Prometheus 指标 - 应返回文本格式")
    void prometheusMetrics_shouldReturnText() throws Exception {
        MetricsVO metric = MetricsVO.builder()
                .name("cpu_usage")
                .value("45.5/100")
                .unit("percent")
                .timestamp(LocalDateTime.now())
                .build();

        when(metricsCollectorService.collectAllMetrics())
                .thenReturn(List.of(metric));

        mockMvc.perform(get("/api/v1/system/metrics/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cpu_usage")));
    }

    @Test
    @DisplayName("获取所有指标 - 应返回 200")
    void getAllMetrics_shouldReturn200() throws Exception {
        MetricsVO metric = MetricsVO.builder()
                .name("cpu_usage")
                .value("45.5")
                .unit("percent")
                .timestamp(LocalDateTime.now())
                .build();

        when(metricsCollectorService.collectAllMetrics())
                .thenReturn(List.of(metric));

        mockMvc.perform(get("/api/v1/system/metrics/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].name").value("cpu_usage"));
    }

    @Test
    @DisplayName("获取指定指标 - 应返回 200")
    void getMetric_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", "cpu_usage");

        MetricsVO metric = MetricsVO.builder()
                .name("cpu_usage")
                .value("45.5")
                .unit("percent")
                .timestamp(LocalDateTime.now())
                .build();

        when(metricsCollectorService.getMetricsByName(eq("cpu_usage")))
                .thenReturn(metric);

        mockMvc.perform(post("/api/v1/system/metrics/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("cpu_usage"));
    }

    @Test
    @DisplayName("获取 CPU 指标 - 应返回 200")
    void getCpuMetrics_shouldReturn200() throws Exception {
        MetricsVO cpuMetrics = MetricsVO.builder()
                .name("cpu_usage")
                .value("45.5")
                .unit("percent")
                .status("normal")
                .timestamp(LocalDateTime.now())
                .build();

        when(metricsCollectorService.collectCpuMetrics())
                .thenReturn(cpuMetrics);

        mockMvc.perform(get("/api/v1/system/metrics/cpu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("cpu_usage"));
    }

    @Test
    @DisplayName("获取内存指标 - 应返回 200")
    void getMemoryMetrics_shouldReturn200() throws Exception {
        MetricsVO memoryMetrics = MetricsVO.builder()
                .name("memory_usage")
                .value("4096/8192")
                .unit("MB")
                .status("normal")
                .timestamp(LocalDateTime.now())
                .build();

        when(metricsCollectorService.collectMemoryMetrics())
                .thenReturn(memoryMetrics);

        mockMvc.perform(get("/api/v1/system/metrics/memory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("memory_usage"));
    }

    @Test
    @DisplayName("获取磁盘指标 - 应返回 200")
    void getDiskMetrics_shouldReturn200() throws Exception {
        MetricsVO diskMetrics = MetricsVO.builder()
                .name("disk_usage")
                .value("100000/500000")
                .unit("MB")
                .status("normal")
                .timestamp(LocalDateTime.now())
                .build();

        when(metricsCollectorService.collectDiskMetrics())
                .thenReturn(diskMetrics);

        mockMvc.perform(get("/api/v1/system/metrics/disk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("disk_usage"));
    }

    @Test
    @DisplayName("获取 JVM 指标 - 应返回 200")
    void getJvmMetrics_shouldReturn200() throws Exception {
        MetricsVO jvmMetrics = MetricsVO.builder()
                .name("jvm_heap_usage")
                .value("512/2048")
                .unit("MB")
                .status("normal")
                .timestamp(LocalDateTime.now())
                .build();

        when(metricsCollectorService.collectJvmMetrics())
                .thenReturn(jvmMetrics);

        mockMvc.perform(get("/api/v1/system/metrics/jvm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("jvm_heap_usage"));
    }

    @Test
    @DisplayName("获取数据库指标 - 应返回 200")
    void getDatabaseMetrics_shouldReturn200() throws Exception {
        MetricsVO dbMetrics = MetricsVO.builder()
                .name("db_connections")
                .value("10/40")
                .unit("connections")
                .status("normal")
                .timestamp(LocalDateTime.now())
                .build();

        when(metricsCollectorService.collectDatabaseMetrics())
                .thenReturn(dbMetrics);

        mockMvc.perform(get("/api/v1/system/metrics/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("db_connections"));
    }
}
