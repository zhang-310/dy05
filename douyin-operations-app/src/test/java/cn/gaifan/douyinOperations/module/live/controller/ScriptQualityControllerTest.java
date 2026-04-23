package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.service.LiveScriptQualityScoringService;
import cn.gaifan.douyinOperations.module.live.service.ScriptQualityEvaluator;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScriptQualityController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ScriptQualityController 集成测试")
class ScriptQualityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScriptQualityEvaluator scriptQualityEvaluator;

    @MockBean
    private LiveScriptQualityScoringService qualityScoringService;

    @Test
    @DisplayName("多维度质量评估 - 应返回 200")
    void evaluate_shouldReturn200() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("content", "欢迎来到直播间，今天给大家带来优质产品");
        params.put("ipType", "product");

        Map<String, Object> result = new HashMap<>();
        result.put("overallScore", 8.5);
        result.put("compliance", 9.0);
        result.put("fluency", 8.0);
        result.put("attraction", 8.5);

        when(scriptQualityEvaluator.evaluate(eq("欢迎来到直播间，今天给大家带来优质产品"), eq("product"), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/script-quality/evaluate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.overallScore").value(8.5))
                .andExpect(jsonPath("$.data.compliance").value(9.0));
    }

    @Test
    @DisplayName("多维度质量评估（缺少内容）- 应返回 1001")
    void evaluate_withoutContent_shouldReturn1001() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("ipType", "product");

        mockMvc.perform(post("/api/v1/live/script-quality/evaluate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("话术内容不能为空"));
    }

    @Test
    @DisplayName("四维加权质量评分 - 应返回 200")
    void score_shouldReturn200() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("scriptId", 1L);

        Map<String, Object> result = new HashMap<>();
        result.put("scriptId", 1L);
        result.put("totalScore", 85.5);
        result.put("complianceScore", 90.0);
        result.put("fluencyScore", 85.0);
        result.put("attractionScore", 82.0);
        result.put("keywordScore", 88.0);

        when(qualityScoringService.scoreScript(eq(1L), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/script-quality/score")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalScore").value(85.5))
                .andExpect(jsonPath("$.data.complianceScore").value(90.0));
    }

    @Test
    @DisplayName("四维加权质量评分（缺少 scriptId）- 应返回 1001")
    void score_withoutScriptId_shouldReturn1001() throws Exception {
        Map<String, Object> params = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/script-quality/score")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("scriptId 不能为空"));
    }

    @Test
    @DisplayName("批量评分场次话术 - 应返回 200")
    void scoreSession_shouldReturn200() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", 100L);

        doNothing().when(qualityScoringService).scoreSession(eq(100L), eq(1L));

        mockMvc.perform(post("/api/v1/live/script-quality/score-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(100))
                .andExpect(jsonPath("$.data.status").value("ok"));
    }

    @Test
    @DisplayName("批量评分场次话术（缺少 sessionId）- 应返回 1001")
    void scoreSession_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> params = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/script-quality/score-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("sessionId 不能为空"));
    }

    @Test
    @DisplayName("TTS 试听预览 - 应返回 200")
    void ttsPreview_shouldReturn200() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("content", "欢迎来到直播间，今天给大家带来优质产品，限时优惠不容错过");

        mockMvc.perform(post("/api/v1/live/script-quality/tts-preview")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.charCount").exists())
                .andExpect(jsonPath("$.data.estimatedSec").exists())
                .andExpect(jsonPath("$.data.estimatedDuration").exists())
                .andExpect(jsonPath("$.data.ttsEnabled").value(false));
    }

    @Test
    @DisplayName("TTS 试听预览（缺少内容）- 应返回 1001")
    void ttsPreview_withoutContent_shouldReturn1001() throws Exception {
        Map<String, Object> params = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/script-quality/tts-preview")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("content 不能为空"));
    }
}
