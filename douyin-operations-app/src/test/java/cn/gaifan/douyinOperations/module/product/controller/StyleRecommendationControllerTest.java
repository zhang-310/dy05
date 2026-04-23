package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.module.product.service.StyleRecommendationMLService;
import cn.gaifan.douyinOperations.module.product.vo.ModelMetricsVO;
import cn.gaifan.douyinOperations.module.product.vo.StyleRecommendationVO;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * StyleRecommendationController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("StyleRecommendationController 集成测试")
class StyleRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StyleRecommendationMLService mlService;

    @Test
    @DisplayName("混合推荐风格 - 应返回 200")
    void recommendHybrid_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);
        body.put("topK", 5);

        when(mlService.recommendHybrid(eq(1L), eq(1L), eq(5))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/style-recommendation/recommend-hybrid")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("ML模型推荐 - 应返回 200")
    void recommendML_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);
        body.put("topK", 5);

        when(mlService.recommendWithML(eq(1L), eq(1L), eq(5))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/product/style-recommendation/recommend-ml")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("训练推荐模型 - 应返回 200")
    void trainModel_shouldReturn200() throws Exception {
        when(mlService.trainModel(eq(1L))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/product/style-recommendation/train")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.modelId").value(1))
                .andExpect(jsonPath("$.data.message").value("模型训练成功"));
    }

    @Test
    @DisplayName("异步训练推荐模型 - 应返回 200")
    void trainModelAsync_shouldReturn200() throws Exception {
        when(mlService.trainModelAsync(eq(1L))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/product/style-recommendation/train-async")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.modelId").value(1));
    }

    @Test
    @DisplayName("获取模型性能指标 - 应返回 200")
    void getMetrics_shouldReturn200() throws Exception {
        ModelMetricsVO metrics = new ModelMetricsVO();
        metrics.setAccuracy(java.math.BigDecimal.valueOf(0.95));

        when(mlService.getModelMetrics(eq(1L))).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/product/style-recommendation/metrics")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accuracy").value(0.95));
    }

    @Test
    @DisplayName("混合推荐风格（未登录）- 应返回 2001")
    void recommendHybrid_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);

        mockMvc.perform(post("/api/v1/product/style-recommendation/recommend-hybrid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
