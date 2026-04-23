package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.service.DigitalHumanProvider;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DigitalHumanController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DigitalHumanController 集成测试")
class DigitalHumanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DigitalHumanProvider digitalHumanProvider;

    @Test
    @DisplayName("数字人服务状态（已配置）- 应返回 200")
    void status_configured_shouldReturn200() throws Exception {
        when(digitalHumanProvider.isConfigured()).thenReturn(true);
        when(digitalHumanProvider.name()).thenReturn("TestProvider");

        mockMvc.perform(post("/api/v1/ai/digital-human/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.provider").value("TestProvider"));
    }

    @Test
    @DisplayName("数字人服务状态（未配置）- 应返回 200")
    void status_notConfigured_shouldReturn200() throws Exception {
        when(digitalHumanProvider.isConfigured()).thenReturn(false);

        mockMvc.perform(post("/api/v1/ai/digital-human/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.provider").value("none"));
    }

    @Test
    @DisplayName("生成数字人视频 - 应返回 200")
    void generate_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("avatarId", "avatar_001");
        body.put("scriptText", "Hello, this is a test script");
        body.put("voiceId", "zh-CN-XiaoxiaoNeural");

        when(digitalHumanProvider.isConfigured()).thenReturn(true);
        when(digitalHumanProvider.name()).thenReturn("TestProvider");
        when(digitalHumanProvider.generateTalkingHead(eq("avatar_001"), eq("Hello, this is a test script"), eq("zh-CN-XiaoxiaoNeural")))
                .thenReturn("https://example.com/video.mp4");

        mockMvc.perform(post("/api/v1/ai/digital-human/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoUrl").value("https://example.com/video.mp4"))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.provider").value("TestProvider"));
    }

    @Test
    @DisplayName("生成数字人视频（使用默认参数）- 应返回 200")
    void generate_withDefaults_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("scriptText", "Test script");

        when(digitalHumanProvider.isConfigured()).thenReturn(true);
        when(digitalHumanProvider.name()).thenReturn("TestProvider");
        when(digitalHumanProvider.generateTalkingHead(eq("default"), eq("Test script"), eq("zh-CN-XiaoxiaoNeural")))
                .thenReturn("https://example.com/video2.mp4");

        mockMvc.perform(post("/api/v1/ai/digital-human/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoUrl").value("https://example.com/video2.mp4"))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @DisplayName("生成数字人视频（缺少 scriptText）- 应返回 1001")
    void generate_missingScriptText_shouldReturn1001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("avatarId", "avatar_001");

        when(digitalHumanProvider.isConfigured()).thenReturn(true);

        mockMvc.perform(post("/api/v1/ai/digital-human/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("scriptText 不能为空"));
    }

    @Test
    @DisplayName("生成数字人视频（scriptText 为空）- 应返回 1001")
    void generate_emptyScriptText_shouldReturn1001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("scriptText", "");

        when(digitalHumanProvider.isConfigured()).thenReturn(true);

        mockMvc.perform(post("/api/v1/ai/digital-human/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("scriptText 不能为空"));
    }

    @Test
    @DisplayName("生成数字人视频（服务未配置）- 应返回 1002")
    void generate_notConfigured_shouldReturn1002() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("scriptText", "Test script");

        when(digitalHumanProvider.isConfigured()).thenReturn(false);

        mockMvc.perform(post("/api/v1/ai/digital-human/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1002))
                .andExpect(jsonPath("$.message").value("数字人服务未配置"));
    }

    @Test
    @DisplayName("生成数字人视频（生成失败）- 应返回 200")
    void generate_failed_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("scriptText", "Test script");

        when(digitalHumanProvider.isConfigured()).thenReturn(true);
        when(digitalHumanProvider.name()).thenReturn("TestProvider");
        when(digitalHumanProvider.generateTalkingHead(anyString(), anyString(), anyString()))
                .thenReturn("");

        mockMvc.perform(post("/api/v1/ai/digital-human/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.message").value("生成失败"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/ai/digital-human/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
