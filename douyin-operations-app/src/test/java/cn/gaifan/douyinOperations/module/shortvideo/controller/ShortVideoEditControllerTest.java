package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvSubtitleSegment;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvSubtitleSegmentRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShotListService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotListVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotVO;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @MockBean
    private SvShotListService shotListService;

    @MockBean
    private SvScriptService scriptService;

    @MockBean
    private SvSubtitleSegmentRepository subtitleSegmentRepository;

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
    @DisplayName("自动剪辑成片（仅 projectId）- 应从项目分镜收集视频片段")
    void autoCompose_projectOnly_shouldBuildMaterialsFromProject() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);

        SvProjectVO project = new SvProjectVO();
        project.setId(1L);
        project.setTitle("项目");
        project.setProjectType("daily");
        project.setScriptId(2L);
        project.setShotListId(3L);

        SvShotVO shot = new SvShotVO();
        shot.setId(4L);
        shot.setShotNumber(1);
        shot.setVideoUrl("https://example.com/shot.mp4");
        shot.setAudioUrl("https://example.com/voice.mp3");
        SvShotListVO shotList = new SvShotListVO();
        shotList.setId(3L);
        shotList.setShots(List.of(shot));

        SvScriptVO script = new SvScriptVO();
        script.setId(2L);
        script.setContent("短视频脚本文案");

        when(projectService.get(eq(1L), eq(1L), anyList())).thenReturn(project);
        when(shotListService.get(eq(3L), eq(1L))).thenReturn(shotList);
        when(scriptService.get(eq(2L), eq(1L))).thenReturn(script);
        when(videoEditService.autoCompose(any(VideoEditService.AutoComposeRequest.class), eq(1L)))
                .thenReturn(new VideoEditService.VideoResult("https://example.com/final.mp4", 30000L, 1024L, "mp4"));

        mockMvc.perform(post("/api/v1/short-video/edit/auto-compose")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.finalVideoUrl").value("https://example.com/final.mp4"));

        ArgumentCaptor<VideoEditService.AutoComposeRequest> captor = ArgumentCaptor.forClass(VideoEditService.AutoComposeRequest.class);
        verify(videoEditService).autoCompose(captor.capture(), eq(1L));
        assertThat(captor.getValue().videoClips()).containsExactly("https://example.com/shot.mp4");
        assertThat(captor.getValue().voiceClipUrls()).containsExactly("https://example.com/voice.mp3");
        assertThat(captor.getValue().scriptText()).isEqualTo("短视频脚本文案");
    }

    @Test
    @DisplayName("查询字幕段 - 应返回持久化字幕")
    void getSubtitles_shouldReturnPersistedSegments() throws Exception {
        SvSubtitleSegment segment = subtitleSegment("seg-1", 1.0, 3.5, "第一句");
        segment.setFontSize(28);
        segment.setColor("#fff");
        when(subtitleSegmentRepository.findByOwnerIdAndVideoIdAndDeletedOrderByStartTimeAsc(1L, 10L, 0))
                .thenReturn(List.of(segment));

        mockMvc.perform(post("/api/v1/short-video/edit/subtitles/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("videoId", 10L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value("seg-1"))
                .andExpect(jsonPath("$.data[0].text").value("第一句"))
                .andExpect(jsonPath("$.data[0].source").value("sv_subtitle_segment"))
                .andExpect(jsonPath("$.data[0].degraded").value(false));
    }

    @Test
    @DisplayName("保存字幕段 - 应软删除旧字幕并保存新字幕")
    void saveSubtitles_shouldSoftDeleteExistingAndSaveNewSegments() throws Exception {
        SvSubtitleSegment existing = subtitleSegment("old-1", 0.0, 1.0, "旧字幕");
        when(subtitleSegmentRepository.findByOwnerIdAndVideoIdAndDeletedOrderByStartTimeAsc(1L, 10L, 0))
                .thenReturn(List.of(existing));
        Map<String, Object> body = Map.of(
                "videoId", 10L,
                "segments", List.of(Map.of(
                        "id", "new-1",
                        "startTime", 1.0,
                        "endTime", 3.5,
                        "text", "新字幕",
                        "fontSize", 30,
                        "color", "#fff",
                        "position", "bottom"))
        );

        mockMvc.perform(post("/api/v1/short-video/edit/subtitles/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        ArgumentCaptor<SvSubtitleSegment> captor = ArgumentCaptor.forClass(SvSubtitleSegment.class);
        verify(subtitleSegmentRepository, times(2)).save(captor.capture());
        List<SvSubtitleSegment> saved = captor.getAllValues();
        assertThat(saved.get(0).getSegmentKey()).isEqualTo("old-1");
        assertThat(saved.get(0).getDeleted()).isEqualTo(1);
        assertThat(saved.get(1).getOwnerId()).isEqualTo(1L);
        assertThat(saved.get(1).getVideoId()).isEqualTo(10L);
        assertThat(saved.get(1).getSegmentKey()).isEqualTo("new-1");
        assertThat(saved.get(1).getText()).isEqualTo("新字幕");
        assertThat(saved.get(1).getFontSize()).isEqualTo(30);
    }

    @Test
    @DisplayName("导出字幕 SRT - 应基于持久化字幕")
    void exportSubtitlesSrt_shouldUsePersistedSegments() throws Exception {
        when(subtitleSegmentRepository.findByOwnerIdAndVideoIdAndDeletedOrderByStartTimeAsc(1L, 10L, 0))
                .thenReturn(List.of(
                        subtitleSegment("seg-1", 1.0, 3.5, "第一句"),
                        subtitleSegment("seg-2", 4.0, 6.0, "第二句")));

        mockMvc.perform(post("/api/v1/short-video/edit/subtitles/export-srt")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("videoId", 10L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", containsString("00:00:01,000 --> 00:00:03,500")))
                .andExpect(jsonPath("$.data", containsString("第一句")))
                .andExpect(jsonPath("$.data", containsString("第二句")));
    }

    @Test
    @DisplayName("自动剪辑成片（缺少 materials 且项目无素材）- 应返回 1001")
    void autoCompose_missingMaterialsAndProjectAssets_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);

        when(projectService.get(eq(1L), eq(1L), anyList())).thenReturn(new SvProjectVO());

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

    private SvSubtitleSegment subtitleSegment(String key, double start, double end, String text) {
        SvSubtitleSegment segment = new SvSubtitleSegment();
        segment.setOwnerId(1L);
        segment.setVideoId(10L);
        segment.setSegmentKey(key);
        segment.setStartTime(start);
        segment.setEndTime(end);
        segment.setText(text);
        segment.setDeleted(0);
        return segment;
    }
}
