package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.service.ModelBenchmarkService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ModelBenchmarkController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ModelBenchmarkController 集成测试")
class ModelBenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModelBenchmarkService modelBenchmarkService;

    @Test
    @DisplayName("模型性能对比 - 应返回 200")
    void getComparison_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskCode", "short_video_script");

        Map<String, Object> model1 = new HashMap<>();
        model1.put("modelId", 1L);
        model1.put("modelName", "GPT-4");
        model1.put("avgLatency", 1500);
        model1.put("avgTokens", 500);

        Map<String, Object> model2 = new HashMap<>();
        model2.put("modelId", 2L);
        model2.put("modelName", "Claude-3");
        model2.put("avgLatency", 1200);
        model2.put("avgTokens", 450);

        when(modelBenchmarkService.getModelComparison(eq("short_video_script")))
                .thenReturn(List.of(model1, model2));

        mockMvc.perform(post("/api/v1/ai/model-benchmark/comparison")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].modelName").value("GPT-4"))
                .andExpect(jsonPath("$.data[1].modelName").value("Claude-3"));
    }

    @Test
    @DisplayName("模型性能对比（无 taskCode）- 应返回 200")
    void getComparison_withoutTaskCode_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        when(modelBenchmarkService.getModelComparison(isNull()))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ai/model-benchmark/comparison")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("推荐最优模型 - 应返回 200")
    void getBestModel_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskCode", "evolution");
        body.put("priority", "latency");

        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("modelId", 3L);
        rec.put("modelName", "gpt-test");
        rec.put("taskCode", "evolution");
        rec.put("priority", "latency");
        when(modelBenchmarkService.recommendBestModel(eq("evolution"), eq("latency")))
                .thenReturn(rec);

        mockMvc.perform(post("/api/v1/ai/model-benchmark/best-model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.modelId").value(3))
                .andExpect(jsonPath("$.data.modelName").value("gpt-test"))
                .andExpect(jsonPath("$.data.taskCode").value("evolution"))
                .andExpect(jsonPath("$.data.priority").value("latency"));
    }

    @Test
    @DisplayName("推荐最优模型（使用默认参数）- 应返回 200")
    void getBestModel_withDefaults_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("modelId", 1L);
        rec.put("modelName", "m1");
        rec.put("taskCode", "default");
        rec.put("priority", "latency");
        when(modelBenchmarkService.recommendBestModel(eq("default"), eq("latency")))
                .thenReturn(rec);

        mockMvc.perform(post("/api/v1/ai/model-benchmark/best-model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.modelId").value(1))
                .andExpect(jsonPath("$.data.taskCode").value("default"));
    }

    @Test
    @DisplayName("推荐最优模型（无结果）- 应返回 200")
    void getBestModel_noResult_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskCode", "unknown");

        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("modelId", 0L);
        rec.put("modelName", "");
        rec.put("taskCode", "unknown");
        rec.put("priority", "latency");
        when(modelBenchmarkService.recommendBestModel(eq("unknown"), eq("latency")))
                .thenReturn(rec);

        mockMvc.perform(post("/api/v1/ai/model-benchmark/best-model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.modelId").value(0));
    }

    @Test
    @DisplayName("记录基准数据 - 应返回 200")
    void recordBenchmark_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("modelId", 1L);
        body.put("taskCode", "short_video_script");
        body.put("latencyMs", 1500L);
        body.put("tokensUsed", 500);
        body.put("success", true);

        doNothing().when(modelBenchmarkService).recordBenchmark(eq(1L), eq("short_video_script"), eq(1500L), eq(500), eq(true));

        mockMvc.perform(post("/api/v1/ai/model-benchmark/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("记录成功"));
    }

    @Test
    @DisplayName("记录基准数据（使用默认值）- 应返回 200")
    void recordBenchmark_withDefaults_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("modelId", 2L);

        doNothing().when(modelBenchmarkService).recordBenchmark(eq(2L), eq("default"), eq(0L), eq(0), eq(true));

        mockMvc.perform(post("/api/v1/ai/model-benchmark/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("记录基准数据（缺少 modelId）- 应返回 400")
    void recordBenchmark_missingModelId_shouldReturn400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskCode", "test");

        mockMvc.perform(post("/api/v1/ai/model-benchmark/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("modelId 不能为空"));
    }
}
