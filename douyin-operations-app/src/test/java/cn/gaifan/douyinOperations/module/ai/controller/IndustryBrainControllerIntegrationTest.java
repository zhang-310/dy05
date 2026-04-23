package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.service.brain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 行业大脑 Controller 集成测试
 * 需登录后调用，Mock 各 brain 服务
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled("Requires full ApplicationContext; enable when test profile is configured")
@DisplayName("IndustryBrainController 集成测试")
class IndustryBrainControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IndustryKnowledgeGraphService knowledgeGraphService;

    @MockBean
    private IndustryCausalEngine causalEngine;

    @MockBean
    private TrendMonitorService trendMonitorService;

    @MockBean
    private UserCognitiveProfileService userProfileService;

    @MockBean
    private AccountDiagnosisService accountDiagnosisService;

    @MockBean
    private StrategicPlanningService strategicPlanningService;

    @MockBean
    private RiskWarningService riskWarningService;

    @MockBean
    private GrowthPathService growthPathService;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = loginAndGetToken();
        when(knowledgeGraphService.isAvailable()).thenReturn(true);
        when(causalEngine.isAvailable()).thenReturn(true);
        when(trendMonitorService.isAvailable()).thenReturn(true);
        when(userProfileService.isAvailable()).thenReturn(true);
        when(accountDiagnosisService.isAvailable()).thenReturn(true);
        when(strategicPlanningService.isAvailable()).thenReturn(true);
        when(riskWarningService.isAvailable()).thenReturn(true);
        when(growthPathService.isAvailable()).thenReturn(true);
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> json = objectMapper.readValue(body, Map.class);
        Object data = json.get("data");
        if (data instanceof Map) {
            Object t = ((Map<?, ?>) data).get("token");
            if (t != null) return t.toString();
        }
        Object t = json.get("token");
        if (t != null) return t.toString();
        throw new IllegalStateException("登录失败，无 token: " + body);
    }

    @Test
    @DisplayName("知识图谱查询 - 200")
    void knowledgeGraphQuery_shouldReturn200() throws Exception {
        when(knowledgeGraphService.queryEntities(eq("topic"), anyString(), anyInt()))
                .thenReturn(List.of(Map.of("entityId", "e1", "name", "护肤")));

        mockMvc.perform(post("/api/v1/ai/brain/knowledge-graph/query")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entityType\":\"topic\",\"keyword\":\"护肤\",\"limit\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("护肤"));
    }

    @Test
    @DisplayName("因果推理 - 200")
    void causalInfer_shouldReturn200() throws Exception {
        when(causalEngine.infer(any())).thenReturn(new IndustryCausalEngine.CausalInferenceResult(0.5, List.of("人设匹配"), List.of(), "测试"));

        mockMvc.perform(post("/api/v1/ai/brain/causal/infer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scriptType\":\"种草\",\"persona\":\"达人\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.expectedConversionRate").value(0.5));
    }

    @Test
    @DisplayName("趋势列表 - 200")
    void trendsCurrent_shouldReturn200() throws Exception {
        when(trendMonitorService.getCurrentTrends(any(), anyInt()))
                .thenReturn(List.of(new TrendMonitorService.TrendSignal("dy_1", "热搜1", "douyin", 0.9, System.currentTimeMillis(), "tianapi", "描述")));

        mockMvc.perform(post("/api/v1/ai/brain/trends/current")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limit\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("风险预警 - 200")
    void riskWarn_shouldReturn200() throws Exception {
        when(riskWarningService.warn(eq("测试内容"), anyLong())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ai/brain/risk/warn")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"这是一段测试文案\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("未登录 - 401")
    void withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/brain/trends/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
