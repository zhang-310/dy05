package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.service.DanmakuAnalysisService;
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

/**
 * DanmakuAnalysisController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DanmakuAnalysisController 集成测试")
class DanmakuAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DanmakuAnalysisService danmakuAnalysisService;

    @Test
    @DisplayName("分析弹幕意图 - 应返回 200")
    void analyze_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("texts", List.of("这个产品多少钱", "有优惠吗", "效果怎么样"));

        Map<String, Object> analysisResult = new HashMap<>();
        analysisResult.put("priceInquiry", 2);
        analysisResult.put("effectInquiry", 1);
        analysisResult.put("totalCount", 3);
        analysisResult.put("dominantIntent", "priceInquiry");

        when(danmakuAnalysisService.analyzeIntents(eq(100L), anyList()))
                .thenReturn(analysisResult);

        mockMvc.perform(post("/api/v1/live/danmaku/analyze")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.priceInquiry").value(2))
                .andExpect(jsonPath("$.data.dominantIntent").value("priceInquiry"));
    }

    @Test
    @DisplayName("分析弹幕意图（空文本列表）- 应返回 200")
    void analyze_withEmptyTexts_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("texts", List.of());

        Map<String, Object> analysisResult = new HashMap<>();
        analysisResult.put("totalCount", 0);

        when(danmakuAnalysisService.analyzeIntents(eq(100L), anyList()))
                .thenReturn(analysisResult);

        mockMvc.perform(post("/api/v1/live/danmaku/analyze")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    @DisplayName("分析弹幕意图（默认空列表）- 应返回 200")
    void analyze_withoutTexts_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        Map<String, Object> analysisResult = new HashMap<>();
        analysisResult.put("totalCount", 0);

        when(danmakuAnalysisService.analyzeIntents(eq(100L), anyList()))
                .thenReturn(analysisResult);

        mockMvc.perform(post("/api/v1/live/danmaku/analyze")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("推荐话术调整 - 应返回 200")
    void suggest_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        Map<String, Object> intents = new HashMap<>();
        intents.put("priceInquiry", 5);
        intents.put("effectInquiry", 3);
        body.put("intents", intents);

        List<String> suggestions = List.of(
                "增加价格说明环节",
                "强调产品效果和案例",
                "提供限时优惠信息"
        );

        when(danmakuAnalysisService.suggestScriptAdjustments(eq(100L), anyMap()))
                .thenReturn(suggestions);

        mockMvc.perform(post("/api/v1/live/danmaku/suggest")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0]").value("增加价格说明环节"))
                .andExpect(jsonPath("$.data[2]").value("提供限时优惠信息"));
    }

    @Test
    @DisplayName("推荐话术调整（空意图）- 应返回 200")
    void suggest_withEmptyIntents_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("intents", Map.of());

        List<String> suggestions = List.of("保持当前话术节奏");

        when(danmakuAnalysisService.suggestScriptAdjustments(eq(100L), anyMap()))
                .thenReturn(suggestions);

        mockMvc.perform(post("/api/v1/live/danmaku/suggest")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0]").value("保持当前话术节奏"));
    }

    @Test
    @DisplayName("分析弹幕缺少 sessionId - 应返回 1001")
    void analyze_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("texts", List.of("测试"));

        mockMvc.perform(post("/api/v1/live/danmaku/analyze")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("sessionId 必填"));
    }

    @Test
    @DisplayName("推荐话术缺少 sessionId - 应返回 1001")
    void suggest_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("intents", Map.of("priceInquiry", 5));

        mockMvc.perform(post("/api/v1/live/danmaku/suggest")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("sessionId 必填"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        mockMvc.perform(post("/api/v1/live/danmaku/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
