package cn.gaifan.douyinOperations.module.copy.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CopyAiController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CopyAiController 集成测试")
class CopyAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShortVideoAiService shortVideoAiService;

    @Test
    @DisplayName("AI 生成文案 - 应返回 200")
    void generate_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("topic", "护肤品推广");
        body.put("style", "专业");
        body.put("keywords", "保湿,美白");
        body.put("length", 200);

        String generatedContent = "这是一款专业的护肤品，具有保湿和美白功效...";

        when(shortVideoAiService.generateCopy(any(), eq(1L), eq("copy_processing")))
                .thenReturn(generatedContent);

        mockMvc.perform(post("/api/v1/copy/ai/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(generatedContent));
    }

    @Test
    @DisplayName("AI 生成文案（使用 category 作为 topic）- 应返回 200")
    void generate_withCategory_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("category", "美妆");
        body.put("style", "时尚");

        String generatedContent = "时尚美妆文案内容...";

        when(shortVideoAiService.generateCopy(any(), eq(1L), eq("copy_processing")))
                .thenReturn(generatedContent);

        mockMvc.perform(post("/api/v1/copy/ai/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(generatedContent));
    }

    @Test
    @DisplayName("AI 生成文案（默认 topic）- 应返回 200")
    void generate_withDefaultTopic_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("style", "通用");

        String generatedContent = "通用文案内容...";

        when(shortVideoAiService.generateCopy(any(), eq(1L), eq("copy_processing")))
                .thenReturn(generatedContent);

        mockMvc.perform(post("/api/v1/copy/ai/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(generatedContent));
    }

    @Test
    @DisplayName("AI 生成文案（带 personaId）- 应返回 200")
    void generate_withPersonaId_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("topic", "护肤");
        body.put("personaId", 100L);
        body.put("length", 150);

        String generatedContent = "基于人设的护肤文案...";

        when(shortVideoAiService.generateCopy(any(), eq(1L), eq("copy_processing")))
                .thenReturn(generatedContent);

        mockMvc.perform(post("/api/v1/copy/ai/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(generatedContent));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void generate_withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("topic", "测试");

        mockMvc.perform(post("/api/v1/copy/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
