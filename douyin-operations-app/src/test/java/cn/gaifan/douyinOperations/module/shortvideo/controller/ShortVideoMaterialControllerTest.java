package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.ai.service.CinematicKnowledgeService;
import cn.gaifan.douyinOperations.module.ai.service.VideoQualityScoreService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoMaterialService;
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
 * ShortVideoMaterialController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoMaterialController 集成测试")
class ShortVideoMaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShortVideoMaterialService materialService;

    @MockBean
    private CinematicKnowledgeService cinematicKnowledgeService;

    @MockBean
    private VideoQualityScoreService videoQualityScoreService;

    @Test
    @DisplayName("批量生成关键帧 - 应返回 200")
    void generateKeyframes_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);
        body.put("shotListId", 1L);
        body.put("shots", List.of(
                Map.of("shotId", 1L, "shotNumber", 1, "sceneDescription", "场景描述")
        ));

        ShortVideoMaterialService.KeyframeResult result = new ShortVideoMaterialService.KeyframeResult(
                1L, 1, "https://example.com/image.jpg", "bos-key", "prompt", null, null
        );

        when(materialService.generateKeyframes(eq(1L), eq(1L), anyList(), eq(1L)))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/api/v1/short-video/material/generate-keyframes")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.keyframes").isArray());
    }

    @Test
    @DisplayName("批量生成配音 - 应返回 200")
    void generateVoiceBatch_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);
        body.put("shotListId", 1L);
        body.put("shots", List.of(
                Map.of("shotId", 1L, "shotNumber", 1, "dialogue", "对白内容")
        ));

        ShortVideoMaterialService.VoiceResult result = new ShortVideoMaterialService.VoiceResult(
                1L, 1, "https://example.com/audio.mp3", "bos-key", 5.0
        );

        when(materialService.generateVoiceBatch(eq(1L), eq(1L), anyList(), eq(1L)))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/api/v1/short-video/material/generate-voice-batch")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.voices").isArray());
    }

    @Test
    @DisplayName("图生视频批量 - 应返回 200")
    void img2videoBatch_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);
        body.put("shotListId", 1L);
        body.put("keyframes", List.of(
                Map.of("shotId", 1L, "shotNumber", 1, "imageUrl", "https://example.com/image.jpg")
        ));

        ShortVideoMaterialService.VideoResult result = new ShortVideoMaterialService.VideoResult(
                1L, 1, "https://example.com/video.mp4", "bos-key", 5
        );

        when(materialService.img2videoBatch(eq(1L), eq(1L), anyList(), eq(1L)))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/api/v1/short-video/material/img2video-batch")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videos").isArray());
    }

    @Test
    @DisplayName("AI 推荐运镜 - 应返回 200")
    void recommendCamera_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sceneDescription", "室内场景");

        CinematicKnowledgeService.CameraRecommendation recommendation =
                new CinematicKnowledgeService.CameraRecommendation(
                        cn.gaifan.douyinOperations.module.ai.domain.CameraType.ZOOM_IN, 0.9, "适合展示细节", "kling", 85.0
                );

        when(cinematicKnowledgeService.recommendCamera(eq("室内场景")))
                .thenReturn(List.of(recommendation));

        mockMvc.perform(post("/api/v1/short-video/material/recommend-camera")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.recommendations").isArray())
                .andExpect(jsonPath("$.data.primary").exists());
    }

    @Test
    @DisplayName("视频质量评分 - 应返回 200")
    void evaluateVideoQuality_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://example.com/video.mp4");

        VideoQualityScoreService.QualityReport report = new VideoQualityScoreService.QualityReport(
                85.5, 90.0, 80.0, 85.0, 88.0, 82.0, "B", List.of(), List.of("建议提升清晰度")
        );

        when(videoQualityScoreService.evaluateVideoFromUrl(eq("https://example.com/video.mp4")))
                .thenReturn(report);

        mockMvc.perform(post("/api/v1/short-video/material/evaluate-video-quality")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.overallScore").value(85.5))
                .andExpect(jsonPath("$.data.grade").value("B"));
    }

    @Test
    @DisplayName("重试单个关键帧生成 - 应返回 200")
    void retryKeyframe_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);
        body.put("shotListId", 1L);
        body.put("shot", Map.of("shotId", 1L, "shotNumber", 1, "sceneDescription", "场景描述"));

        ShortVideoMaterialService.KeyframeResult result = new ShortVideoMaterialService.KeyframeResult(
                1L, 1, "https://example.com/image.jpg", "bos-key", "prompt", null, null
        );

        when(materialService.generateKeyframes(eq(1L), eq(1L), anyList(), eq(1L)))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/api/v1/short-video/material/retry-keyframe")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.keyframe").exists());
    }

    @Test
    @DisplayName("批量生成关键帧（缺少 shots）- 应返回 1001")
    void generateKeyframes_missingShots_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);

        mockMvc.perform(post("/api/v1/short-video/material/generate-keyframes")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("视频质量评分（缺少 videoUrl）- 应返回 1001")
    void evaluateVideoQuality_missingUrl_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/material/evaluate-video-quality")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("批量生成关键帧（未登录）- 应返回 2001")
    void generateKeyframes_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("shots", List.of(Map.of("shotId", 1L)));

        mockMvc.perform(post("/api/v1/short-video/material/generate-keyframes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
