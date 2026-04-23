package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.ai.service.AiMusicProvider;
import cn.gaifan.douyinOperations.module.ai.service.AiMusicService;
import cn.gaifan.douyinOperations.module.ai.service.SfxGenerationService;
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
 * AiMusicController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AiMusicController 集成测试")
class AiMusicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiMusicService aiMusicService;

    @MockBean
    private SfxGenerationService sfxGenerationService;

    @Test
    @DisplayName("AI 生成 BGM - 应返回 200")
    void generateBgm_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("styleDescription", "cinematic background music");
        body.put("durationSec", 30);
        body.put("instrumental", true);

        AiMusicProvider.MusicGenerationResult result = new AiMusicProvider.MusicGenerationResult(
                "https://example.com/music.mp3",
                "suno",
                30000,
                120
        );

        when(aiMusicService.generateBgm(anyString(), anyInt(), anyBoolean())).thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/music/generate-bgm")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.musicUrl").value("https://example.com/music.mp3"))
                .andExpect(jsonPath("$.data.provider").value("suno"))
                .andExpect(jsonPath("$.data.bpm").value(120));
    }

    @Test
    @DisplayName("获取可用的 BGM 生成 Provider - 应返回 200")
    void providers_shouldReturn200() throws Exception {
        when(aiMusicService.getAvailableProviders()).thenReturn(List.of("suno", "udio"));

        mockMvc.perform(post("/api/v1/short-video/music/providers")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("suno"));
    }

    @Test
    @DisplayName("AI 生成音效 - 应返回 200")
    void generateSfx_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sceneDescription", "footsteps on wooden floor");
        body.put("durationSec", 5.0);

        SfxGenerationService.SfxResult sfxResult = new SfxGenerationService.SfxResult(
                "footsteps on wooden floor",
                "https://example.com/sfx.mp3",
                5.0
        );

        when(sfxGenerationService.generateSfxFromScene(anyString(), anyDouble(), eq(1L)))
                .thenReturn(List.of(sfxResult));

        mockMvc.perform(post("/api/v1/short-video/music/generate-sfx")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].description").value("footsteps on wooden floor"))
                .andExpect(jsonPath("$.data[0].audioUrl").value("https://example.com/sfx.mp3"));
    }

    @Test
    @DisplayName("AI 生成音效（未登录）- 应返回 2001")
    void generateSfx_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sceneDescription", "footsteps on wooden floor");

        mockMvc.perform(post("/api/v1/short-video/music/generate-sfx")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
