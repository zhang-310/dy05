package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
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
 * ShortVideoEditController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoEditController 集成测试")
class ShortVideoEditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VideoEditService videoEditService;

    @MockBean
    private BosStorageService bosStorageService;

    @MockBean
    private SvProjectService projectService;

    @Test
    @DisplayName("自动剪辑成片 - 应返回 200")
    void autoCompose_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);
        Map<String, Object> materials = new HashMap<>();
        materials.put("videos", List.of(
                Map.of("videoUrl", "https://example.com/video1.mp4"),
                Map.of("videoUrl", "https://example.com/video2.mp4")
        ));
        materials.put("bgmUrl", "https://example.com/bgm.mp3");
        body.put("materials", materials);

        VideoEditService.VideoResult result = new VideoEditService.VideoResult(
                "https://example.com/final.mp4", 30000L, 1024000L, "mp4"
        );

        when(videoEditService.autoCompose(any(VideoEditService.AutoComposeRequest.class), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/short-video/edit/auto-compose")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.finalVideoUrl").exists())
                .andExpect(jsonPath("$.data.duration").value(30));
    }

    @Test
    @DisplayName("生成字幕 - 应返回 200")
    void generateSubtitles_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("script", "这是一段测试文案。用于生成字幕。");
        body.put("language", "zh");

        mockMvc.perform(post("/api/v1/short-video/edit/generate-subtitles")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.subtitles").isArray());
    }

    @Test
    @DisplayName("自动剪辑成片（缺少 materials）- 应返回 1001")
    void autoCompose_missingMaterials_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);

        mockMvc.perform(post("/api/v1/short-video/edit/auto-compose")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("自动剪辑成片（videos 为空）- 应返回 1001")
    void autoCompose_emptyVideos_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);
        Map<String, Object> materials = new HashMap<>();
        materials.put("videos", List.of());
        body.put("materials", materials);

        mockMvc.perform(post("/api/v1/short-video/edit/auto-compose")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("生成字幕（缺少 videoUrl 和 script）- 应返回 1001")
    void generateSubtitles_missingBoth_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/edit/generate-subtitles")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("自动剪辑成片（未登录）- 应返回 2001")
    void autoCompose_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> materials = new HashMap<>();
        materials.put("videos", List.of(Map.of("videoUrl", "https://example.com/video.mp4")));
        body.put("materials", materials);

        mockMvc.perform(post("/api/v1/short-video/edit/auto-compose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
