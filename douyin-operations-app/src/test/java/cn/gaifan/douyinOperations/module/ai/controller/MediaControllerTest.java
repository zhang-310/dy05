package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.service.*;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MediaController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("MediaController 集成测试")
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ImageGenerationService imageGenerationService;

    @MockBean
    private TtsService ttsService;

    @MockBean
    private VideoEditService videoEditService;

    @MockBean
    private VideoGenerationService videoGenerationService;

    @MockBean
    private AiQuotaService aiQuotaService;

    @MockBean
    private AiCallLogService aiCallLogService;

    @Test
    @DisplayName("文生图 - 应返回 200")
    void textToImage_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", "a beautiful sunset");
        body.put("width", 512);
        body.put("height", 512);

        ImageGenerationService.ImageResult result = new ImageGenerationService.ImageResult(
                "https://example.com/image.png", "a beautiful sunset", Map.of("width", 512, "height", 512), 1000L
        );

        when(imageGenerationService.textToImage(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/image/text2img")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/image.png"))
                .andExpect(jsonPath("$.data.prompt").value("a beautiful sunset"));
    }

    @Test
    @DisplayName("图生图 - 应返回 200")
    void imageToImage_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("imageUrl", "https://example.com/input.png");
        body.put("prompt", "make it more colorful");

        ImageGenerationService.ImageResult result = new ImageGenerationService.ImageResult(
                "https://example.com/output.png", "make it more colorful", Map.of(), 1200L
        );

        when(imageGenerationService.imageToImage(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/image/img2img")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/output.png"));
    }

    @Test
    @DisplayName("图像编辑 - 应返回 200")
    void editImage_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("imageUrl", "https://example.com/input.png");
        body.put("prompt", "remove background");

        ImageGenerationService.ImageResult result = new ImageGenerationService.ImageResult(
                "https://example.com/edited.png", "remove background", Map.of(), 1500L
        );

        when(imageGenerationService.editImage(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/image/edit")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/edited.png"));
    }

    @Test
    @DisplayName("获取图像生成历史 - 应返回 200")
    void getImageHistory_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("size", 20);

        ImageGenerationService.ImageGenerationHistory history = new ImageGenerationService.ImageGenerationHistory(
                1L, "https://example.com/image.png", "a beautiful sunset", "text2img", Map.of(), 1000000000L
        );

        when(imageGenerationService.getHistory(eq(1L), eq(0), eq(20)))
                .thenReturn(List.of(history));

        mockMvc.perform(post("/api/v1/ai/media/image/history")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].type").value("text2img"))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://example.com/image.png"));
    }

    @Test
    @DisplayName("文本转语音 - 应返回 200")
    void textToSpeech_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("text", "Hello world");
        body.put("voiceId", "voice_001");

        TtsService.AudioResult result = new TtsService.AudioResult(
                "https://example.com/audio.mp3", "Hello world", 5000L, 102400L
        );

        when(ttsService.textToSpeech(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/tts/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.audioUrl").value("https://example.com/audio.mp3"))
                .andExpect(jsonPath("$.data.duration").value(5000));
    }

    @Test
    @DisplayName("获取可用音色 - 应返回 200")
    void getVoices_shouldReturn200() throws Exception {
        TtsService.VoiceInfo voice = new TtsService.VoiceInfo(
                "voice_001", "Female Voice", "en-US", "female", "A natural female voice"
        );

        when(ttsService.getAvailableVoices()).thenReturn(List.of(voice));

        mockMvc.perform(post("/api/v1/ai/media/tts/voices")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value("voice_001"))
                .andExpect(jsonPath("$.data[0].name").value("Female Voice"));
    }

    @Test
    @DisplayName("获取语音合成历史 - 应返回 200")
    void getTtsHistory_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("size", 20);

        TtsService.TtsHistory history = new TtsService.TtsHistory(
                1L, "https://example.com/audio.mp3", "Hello world", "voice_001", 1000000000L
        );

        when(ttsService.getHistory(eq(1L), eq(0), eq(20)))
                .thenReturn(List.of(history));

        mockMvc.perform(post("/api/v1/ai/media/tts/history")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].text").value("Hello world"))
                .andExpect(jsonPath("$.data[0].audioUrl").value("https://example.com/audio.mp3"));
    }

    @Test
    @DisplayName("视频剪辑 - 应返回 200")
    void trimVideo_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://example.com/video.mp4");
        body.put("startSec", 10);
        body.put("endSec", 30);

        VideoEditService.VideoResult result = new VideoEditService.VideoResult(
                "https://example.com/trimmed.mp4", 20000L, 2048000L, "mp4"
        );

        when(videoEditService.trimVideo(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/video/trim")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoUrl").value("https://example.com/trimmed.mp4"))
                .andExpect(jsonPath("$.data.duration").value(20000));
    }

    @Test
    @DisplayName("视频合并 - 应返回 200")
    void mergeVideos_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrls", List.of("https://example.com/v1.mp4", "https://example.com/v2.mp4"));

        VideoEditService.VideoResult result = new VideoEditService.VideoResult(
                "https://example.com/merged.mp4", 60000L, 6144000L, "mp4"
        );

        when(videoEditService.mergeVideos(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/video/merge")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoUrl").value("https://example.com/merged.mp4"));
    }

    @Test
    @DisplayName("添加字幕 - 应返回 200")
    void addSubtitles_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://example.com/video.mp4");
        body.put("subtitleText", "Hello world");

        VideoEditService.VideoResult result = new VideoEditService.VideoResult(
                "https://example.com/subtitled.mp4", 30000L, 3072000L, "mp4"
        );

        when(videoEditService.addSubtitles(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/video/subtitle")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoUrl").value("https://example.com/subtitled.mp4"));
    }

    @Test
    @DisplayName("添加背景音乐 - 应返回 200")
    void addBackgroundMusic_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://example.com/video.mp4");
        body.put("musicUrl", "https://example.com/music.mp3");

        VideoEditService.VideoResult result = new VideoEditService.VideoResult(
                "https://example.com/with_music.mp4", 30000L, 3072000L, "mp4"
        );

        when(videoEditService.addBackgroundMusic(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/video/music")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoUrl").value("https://example.com/with_music.mp4"));
    }

    @Test
    @DisplayName("视频转码 - 应返回 200")
    void transcodeVideo_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://example.com/video.mp4");
        body.put("format", "webm");

        VideoEditService.VideoResult result = new VideoEditService.VideoResult(
                "https://example.com/video.webm", 30000L, 2560000L, "webm"
        );

        when(videoEditService.transcodeVideo(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/video/transcode")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoUrl").value("https://example.com/video.webm"));
    }

    @Test
    @DisplayName("首尾帧生成视频 - 应返回 200")
    void generateFromFrames_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("startFrameUrl", "https://example.com/start.png");
        body.put("endFrameUrl", "https://example.com/end.png");
        body.put("durationSec", 5);

        VideoEditService.VideoResult result = new VideoEditService.VideoResult(
                "https://example.com/generated.mp4", 5000L, 1024000L, "mp4"
        );

        when(videoGenerationService.generateFromFrames(anyString(), anyString(), eq(5), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/video/generate-from-frames")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoUrl").value("https://example.com/generated.mp4"));
    }

    @Test
    @DisplayName("自动成片 - 应返回 200")
    void autoCompose_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("clips", List.of("https://example.com/clip1.mp4", "https://example.com/clip2.mp4"));
        body.put("style", "dynamic");

        VideoEditService.VideoResult result = new VideoEditService.VideoResult(
                "https://example.com/composed.mp4", 60000L, 6144000L, "mp4"
        );

        when(videoEditService.autoCompose(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/media/video/auto-compose")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoUrl").value("https://example.com/composed.mp4"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", "test");

        mockMvc.perform(post("/api/v1/ai/media/image/text2img")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
