package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.ai.service.AiMusicProvider;
import cn.gaifan.douyinOperations.module.ai.service.AiMusicService;
import cn.gaifan.douyinOperations.module.ai.service.SfxGenerationService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvGenerationLog;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvGenerationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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

    @MockBean
    private SvGenerationLogRepository generationLogRepository;

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

        ArgumentCaptor<SvGenerationLog> captor = ArgumentCaptor.forClass(SvGenerationLog.class);
        verify(generationLogRepository).save(captor.capture());
        SvGenerationLog log = captor.getValue();
        assertThat(log.getOwnerId()).isEqualTo(1L);
        assertThat(log.getContentType()).isEqualTo("bgm");
        assertThat(log.getPrompt()).isEqualTo("cinematic background music");
        assertThat(log.getVideoUrl()).isEqualTo("https://example.com/music.mp3");
        assertThat(log.getAiProvider()).isEqualTo("suno");
        assertThat(log.getGenerationTimeMs()).isEqualTo(30000L);
        assertThat(log.getHasAudio()).isTrue();
        assertThat(log.getSuccess()).isTrue();
    }

    @Test
    @DisplayName("AI 生成 BGM（未登录）- 应返回 2001")
    void generateBgm_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("styleDescription", "cinematic background music");

        mockMvc.perform(post("/api/v1/short-video/music/generate-bgm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
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
    @DisplayName("查询 AI 音乐历史 - 应返回落库记录")
    void musicHistory_shouldReturnPersistedGenerationLogs() throws Exception {
        SvGenerationLog log = new SvGenerationLog();
        log.setId(99L);
        log.setOwnerId(1L);
        log.setContentType("bgm");
        log.setPrompt("lofi bgm");
        log.setVideoUrl("https://example.com/lofi.mp3");
        log.setAiProvider("suno");
        log.setGenerationTimeMs(45000L);
        log.setCreateTime(new Timestamp(1710000000000L));
        when(generationLogRepository.findByOwnerIdAndContentTypeInOrderByCreateTimeDesc(
                eq(1L), eq(List.of("bgm", "sfx")), any(Pageable.class)))
                .thenReturn(List.of(log));

        mockMvc.perform(post("/api/v1/short-video/music/history")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rows", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value(99))
                .andExpect(jsonPath("$.data[0].name").value("lofi bgm"))
                .andExpect(jsonPath("$.data[0].url").value("https://example.com/lofi.mp3"))
                .andExpect(jsonPath("$.data[0].source").value("sv_generation_log"))
                .andExpect(jsonPath("$.data[0].degraded").value(false));
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
