package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.ContrastVideoTemplateService;
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
 * ContrastVideoTemplateController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ContrastVideoTemplateController 集成测试")
class ContrastVideoTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContrastVideoTemplateService contrastVideoTemplateService;

    @Test
    @DisplayName("获取镜头模板 - 应返回 200")
    void getShotTemplate_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contrastType", "social_identity_contrast");
        body.put("duration", 25);

        Map<String, Object> template = Map.of(
                "shots", List.of(
                        Map.of("duration", 5, "description", "开场镜头"),
                        Map.of("duration", 10, "description", "对比镜头")
                ),
                "totalDuration", 25
        );

        when(contrastVideoTemplateService.getShotTemplate(eq("social_identity_contrast"), eq(25)))
                .thenReturn(template);

        mockMvc.perform(post("/api/v1/short-video/contrast-template/shot-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalDuration").value(25));
    }

    @Test
    @DisplayName("获取喜剧配置 - 应返回 200")
    void getComedyConfig_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contrastType", "social_identity_contrast");

        Map<String, Object> config = Map.of(
                "comedyStyle", "exaggeration",
                "punchlinePosition", "end",
                "humorLevel", 8
        );

        when(contrastVideoTemplateService.getComedyConfig(eq("social_identity_contrast")))
                .thenReturn(config);

        mockMvc.perform(post("/api/v1/short-video/contrast-template/comedy-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.comedyStyle").value("exaggeration"));
    }

    @Test
    @DisplayName("获取 BGM 策略 - 应返回 200")
    void getBgmStrategy_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contrastType", "social_identity_contrast");

        Map<String, Object> strategy = Map.of(
                "bgmType", "upbeat",
                "tempo", 120,
                "mood", "energetic"
        );

        when(contrastVideoTemplateService.getBgmStrategy(eq("social_identity_contrast")))
                .thenReturn(strategy);

        mockMvc.perform(post("/api/v1/short-video/contrast-template/bgm-strategy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.bgmType").value("upbeat"));
    }

    @Test
    @DisplayName("模板列表 - 应返回 200")
    void listTemplates_shouldReturn200() throws Exception {
        when(contrastVideoTemplateService.listTemplates()).thenReturn(List.of(
                Map.of("type", "social_identity_contrast", "name", "社会身份对比"),
                Map.of("type", "expectation_reality", "name", "期望与现实")
        ));

        mockMvc.perform(post("/api/v1/short-video/contrast-template/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].type").value("social_identity_contrast"));
    }

    @Test
    @DisplayName("获取预设模板 - 应返回 200")
    void getPresetTemplate_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contrastType", "social_identity_contrast");
        body.put("preset", "25s");

        Map<String, Object> preset = Map.of(
                "duration", 25,
                "shots", List.of(
                        Map.of("duration", 5, "description", "开场"),
                        Map.of("duration", 15, "description", "对比"),
                        Map.of("duration", 5, "description", "结尾")
                )
        );

        when(contrastVideoTemplateService.getPresetTemplate(eq("social_identity_contrast"), eq("25s")))
                .thenReturn(preset);

        mockMvc.perform(post("/api/v1/short-video/contrast-template/preset-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.duration").value(25));
    }
}
