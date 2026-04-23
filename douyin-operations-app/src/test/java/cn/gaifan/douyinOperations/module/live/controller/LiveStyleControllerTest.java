package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.controller.LiveStyleController.StyleRecommendRequest;
import cn.gaifan.douyinOperations.module.live.service.StyleRecommendService;
import cn.gaifan.douyinOperations.module.live.vo.StyleRecommendationVO;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveStyleController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveStyleController 集成测试")
class LiveStyleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StyleRecommendService styleRecommendService;

    @Test
    @DisplayName("智能风格推荐 - 应返回 200")
    void recommend_shouldReturn200() throws Exception {
        StyleRecommendRequest request = new StyleRecommendRequest();
        request.setProductId(100L);
        request.setLimit(5);

        StyleRecommendationVO style1 = new StyleRecommendationVO(
                "professional", new BigDecimal("8.5"), 50L, 10);
        StyleRecommendationVO style2 = new StyleRecommendationVO(
                "friendly", new BigDecimal("7.8"), 30L, 8);

        when(styleRecommendService.recommendStyles(eq(1L), eq(100L), eq(5)))
                .thenReturn(List.of(style1, style2));

        mockMvc.perform(post("/api/v1/live/style/recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].styleCode").value("professional"))
                .andExpect(jsonPath("$.data[0].avgEffectiveness").value(8.5))
                .andExpect(jsonPath("$.data[0].usageCount").value(50))
                .andExpect(jsonPath("$.data[1].styleCode").value("friendly"));
    }

    @Test
    @DisplayName("智能风格推荐（无商品 ID）- 应返回 200")
    void recommend_withoutProductId_shouldReturn200() throws Exception {
        StyleRecommendRequest request = new StyleRecommendRequest();
        request.setLimit(10);

        StyleRecommendationVO style = new StyleRecommendationVO(
                "casual", new BigDecimal("7.5"), 20L, 5);

        when(styleRecommendService.recommendStyles(eq(1L), isNull(), eq(10)))
                .thenReturn(List.of(style));

        mockMvc.perform(post("/api/v1/live/style/recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].styleCode").value("casual"));
    }

    @Test
    @DisplayName("智能风格推荐（默认 limit）- 应返回 200")
    void recommend_withDefaultLimit_shouldReturn200() throws Exception {
        StyleRecommendRequest request = new StyleRecommendRequest();
        request.setProductId(100L);

        StyleRecommendationVO style = new StyleRecommendationVO(
                "energetic", new BigDecimal("9.0"), 100L, 20);

        when(styleRecommendService.recommendStyles(eq(1L), eq(100L), eq(10)))
                .thenReturn(List.of(style));

        mockMvc.perform(post("/api/v1/live/style/recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].avgEffectiveness").value(9.0));
    }

    @Test
    @DisplayName("智能风格推荐（空请求体）- 应返回 200")
    void recommend_withEmptyBody_shouldReturn200() throws Exception {
        StyleRecommendationVO style = new StyleRecommendationVO(
                "default", new BigDecimal("6.5"), 10L, 3);

        when(styleRecommendService.recommendStyles(eq(1L), isNull(), eq(10)))
                .thenReturn(List.of(style));

        mockMvc.perform(post("/api/v1/live/style/recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].styleCode").value("default"));
    }

    @Test
    @DisplayName("智能风格推荐（无效 limit）- 应使用默认值 10")
    void recommend_withInvalidLimit_shouldUseDefault() throws Exception {
        StyleRecommendRequest request = new StyleRecommendRequest();
        request.setLimit(-5);

        StyleRecommendationVO style = new StyleRecommendationVO(
                "test", new BigDecimal("7.0"), 15L, 4);

        when(styleRecommendService.recommendStyles(eq(1L), isNull(), eq(10)))
                .thenReturn(List.of(style));

        mockMvc.perform(post("/api/v1/live/style/recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void recommend_withoutAuth_shouldReturn2001() throws Exception {
        StyleRecommendRequest request = new StyleRecommendRequest();

        mockMvc.perform(post("/api/v1/live/style/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
