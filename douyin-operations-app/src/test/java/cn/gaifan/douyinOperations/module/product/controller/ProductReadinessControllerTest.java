package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.module.product.service.ProductReadinessService;
import cn.gaifan.douyinOperations.module.product.vo.ProductReadinessVO;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProductReadinessController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ProductReadinessController 集成测试")
class ProductReadinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductReadinessService productReadinessService;

    @Test
    @DisplayName("检测商品上播准备度 - 应返回 200")
    void checkReadiness_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);

        ProductReadinessVO readiness = new ProductReadinessVO();
        readiness.setReadyForLive(true);
        readiness.setOverallScore(85);

        when(productReadinessService.checkReadiness(eq(1L), eq(1L))).thenReturn(readiness);

        mockMvc.perform(post("/api/v1/product/readiness")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.readyForLive").value(true))
                .andExpect(jsonPath("$.data.overallScore").value(85));
    }

    @Test
    @DisplayName("检测商品上播准备度（未登录）- 应返回 2001")
    void checkReadiness_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1L);

        mockMvc.perform(post("/api/v1/product/readiness")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("检测商品上播准备度（缺少 productId）- 应返回 1001")
    void checkReadiness_missingProductId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/product/readiness")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
