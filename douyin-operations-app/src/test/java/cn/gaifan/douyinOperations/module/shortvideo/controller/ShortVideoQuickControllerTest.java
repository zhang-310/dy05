package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoQuickService;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoQuickController 集成测试")
class ShortVideoQuickControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShortVideoQuickService quickService;

    @Test
    @DisplayName("一键生成 - 应返回 200")
    void generate_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("theme", "美食");
        body.put("keywords", "火锅,麻辣");
        body.put("style", "温馨");

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", 1L);
        result.put("script", "生成的脚本内容...");
        result.put("videoUrl", "https://example.com/video.mp4");

        when(quickService.quickGenerate(eq("美食"), eq("火锅,麻辣"), eq("温馨"), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/quick/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.projectId").value(1));
    }

    @Test
    @DisplayName("一键生成（默认参数）- 应返回 200")
    void generate_defaultParams_shouldReturn200() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", 1L);
        result.put("script", "默认生成的脚本内容...");

        when(quickService.quickGenerate(eq("美食"), eq(""), eq("温馨"), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/quick/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.projectId").value(1));
    }

    @Test
    @DisplayName("一键生成（未登录）- 应返回 2001")
    void generate_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("theme", "美食");

        mockMvc.perform(post("/api/v1/short-video/quick/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
