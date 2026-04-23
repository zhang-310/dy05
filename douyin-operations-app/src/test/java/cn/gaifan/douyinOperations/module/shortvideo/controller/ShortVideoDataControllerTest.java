package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.ViralCollectVO;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ShortVideoDataController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoDataController 集成测试")
class ShortVideoDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ViralVideoService viralVideoService;

    @MockBean
    private SvScriptService scriptService;

    @Test
    @DisplayName("采集热门视频 - 应返回 200")
    void collectHotVideos_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://douyin.com/video/123");
        body.put("title", "热门视频");
        body.put("viewCount", 100000);

        when(viralVideoService.collectViralVideo(any(ViralCollectVO.class), eq(1L))).thenReturn(1L);
        doNothing().when(viralVideoService).triggerAnalysis(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/data/collect-hot-videos")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("爆款分析 - 应返回 200")
    void analyzeViral_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoUrl", "https://douyin.com/video/123");
        body.put("extractLevel", "detailed");

        when(scriptService.analyzeViral(eq("https://douyin.com/video/123"), eq("detailed"), eq(1L)))
                .thenReturn("分析结果");

        mockMvc.perform(post("/api/v1/short-video/data/analyze-viral")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("分析结果"));
    }

    @Test
    @DisplayName("采集热门视频（缺少 videoUrl）- 应返回 1001")
    void collectHotVideos_missingUrl_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/data/collect-hot-videos")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("爆款分析（缺少 viralVideoUrl）- 应返回 1001")
    void analyzeViral_missingUrl_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/data/analyze-viral")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("采集热门视频（未登录）- 应返回 2001")
    void collectHotVideos_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoUrl", "https://douyin.com/video/123");

        mockMvc.perform(post("/api/v1/short-video/data/collect-hot-videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
