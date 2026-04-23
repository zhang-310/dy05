package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.ai.entity.AiPromptTemplate;
import cn.gaifan.douyinOperations.module.ai.service.PromptTemplateService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptRecommendService;
import cn.gaifan.douyinOperations.module.live.vo.ScriptRecommendRequestVO;
import cn.gaifan.douyinOperations.module.live.vo.ScriptRecommendVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveScriptRecommendController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptRecommendController 集成测试")
class LiveScriptRecommendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveScriptRecommendService liveScriptRecommendService;

    @MockBean
    private PromptTemplateService promptTemplateService;

    @Test
    @DisplayName("多场景话术推荐 - 应返回 200")
    void recommendScripts_shouldReturn200() throws Exception {
        ScriptRecommendRequestVO requestVO = new ScriptRecommendRequestVO();
        requestVO.setSessionId(100L);
        requestVO.setProductId(200L);
        requestVO.setCurrentSlot(2);
        requestVO.setViewerCount(500L);
        requestVO.setTimeElapsed(1800L);
        requestVO.setTopN(5);

        ScriptRecommendVO recommend1 = ScriptRecommendVO.builder()
                .scriptId(1L)
                .sourceType("live_script")
                .scriptType("product")
                .reason("历史效果好，适合当前时段")
                .contentPreview("欢迎来到直播间...")
                .effectivenessScore(new BigDecimal("85.5"))
                .useCount(10)
                .recommendScore(0.85)
                .build();

        ScriptRecommendVO recommend2 = ScriptRecommendVO.builder()
                .scriptId(2L)
                .sourceType("script_library")
                .scriptType("interaction")
                .reason("互动效果佳")
                .contentPreview("感谢老铁们的支持...")
                .effectivenessScore(new BigDecimal("78.0"))
                .useCount(5)
                .recommendScore(0.78)
                .build();

        when(liveScriptRecommendService.recommend(any(ScriptRecommendRequestVO.class), eq(1L)))
                .thenReturn(List.of(recommend1, recommend2));

        mockMvc.perform(post("/api/v1/live/ai/recommend-scripts")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].scriptId").value(1))
                .andExpect(jsonPath("$.data[0].reason").value("历史效果好，适合当前时段"))
                .andExpect(jsonPath("$.data[1].scriptId").value(2));
    }

    @Test
    @DisplayName("2小时聊天策略（预设策略）- 应返回 200")
    void chat2hStrategy_preset_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("personaCode", "local_flavor");
        body.put("productCount", 3);

        when(promptTemplateService.getActiveTemplate(eq("live_chat_2h_strategy"), eq("local_flavor"), eq(1L)))
                .thenReturn(null);

        mockMvc.perform(post("/api/v1/live/ai/chat-2h-strategy")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.liveFormat").value("chat_2h"))
                .andExpect(jsonPath("$.data.totalDurationMin").value(120))
                .andExpect(jsonPath("$.data.strategyType").value("preset"))
                .andExpect(jsonPath("$.data.phases[0].phase").value(1))
                .andExpect(jsonPath("$.data.phases[0].theme").value("暖场聊天建立信任"));
    }

    @Test
    @DisplayName("2小时聊天策略（模板覆盖）- 应返回 200")
    void chat2hStrategy_template_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("personaCode", "local_flavor");
        body.put("productCount", 3);

        AiPromptTemplate template = new AiPromptTemplate();
        template.setId(100L);
        template.setTemplateCode("live_chat_2h_strategy");
        template.setTemplateContent("自定义策略内容：东北风格聊天式直播...");

        when(promptTemplateService.getActiveTemplate(eq("live_chat_2h_strategy"), eq("local_flavor"), eq(1L)))
                .thenReturn(template);

        mockMvc.perform(post("/api/v1/live/ai/chat-2h-strategy")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.strategyType").value("template"))
                .andExpect(jsonPath("$.data.templateId").value(100))
                .andExpect(jsonPath("$.data.strategyNote").value("自定义策略内容：东北风格聊天式直播..."));
    }

    @Test
    @DisplayName("2小时聊天策略（无请求体）- 应返回 200")
    void chat2hStrategy_noBody_shouldReturn200() throws Exception {
        when(promptTemplateService.getActiveTemplate(anyString(), isNull(), eq(1L)))
                .thenReturn(null);

        mockMvc.perform(post("/api/v1/live/ai/chat-2h-strategy")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.liveFormat").value("chat_2h"))
                .andExpect(jsonPath("$.data.recommendedPersona").value("local_flavor"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        ScriptRecommendRequestVO requestVO = new ScriptRecommendRequestVO();
        requestVO.setSessionId(100L);

        mockMvc.perform(post("/api/v1/live/ai/recommend-scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
