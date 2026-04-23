package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ViralVideoController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ViralVideoController 集成测试")
class ViralVideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ViralVideoService viralVideoService;

    @Test
    @DisplayName("爆款视频列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        SvViralVideo video = new SvViralVideo();
        video.setId(1L);
        video.setTitle("爆款视频");

        when(viralVideoService.listViralVideos(eq(1L), eq("my"), isNull(), eq("viralScore"), eq(0), eq(20)))
                .thenReturn(List.of(video));

        mockMvc.perform(post("/api/v1/short-video/viral/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("mode", "my")
                        .param("sortBy", "viralScore")
                        .param("page", "0")
                        .param("rows", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("采集爆款视频 - 应返回 200")
    void collect_shouldReturn200() throws Exception {
        ViralCollectVO vo = new ViralCollectVO();
        vo.setVideoUrl("https://douyin.com/video/123");
        vo.setTitle("爆款视频");

        when(viralVideoService.collectViralVideo(any(ViralCollectVO.class), eq(1L))).thenReturn(1L);
        doNothing().when(viralVideoService).triggerAnalysis(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/viral/collect")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("获取爆款视频详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        SvViralVideo video = new SvViralVideo();
        video.setId(1L);
        video.setTitle("爆款视频");

        when(viralVideoService.getViralVideo(eq(1L), eq(1L))).thenReturn(video);

        mockMvc.perform(post("/api/v1/short-video/viral/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("删除爆款视频 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        doNothing().when(viralVideoService).deleteViralVideo(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/viral/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("分析爆款视频 - 应返回 200")
    void analyze_shouldReturn200() throws Exception {
        doNothing().when(viralVideoService).triggerAnalysis(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/viral/analyze")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("复刻爆款视频 - 应返回 200")
    void replicate_shouldReturn200() throws Exception {
        when(viralVideoService.replicateViral(eq(1L), eq(1L))).thenReturn("复刻方案");

        mockMvc.perform(post("/api/v1/short-video/viral/replicate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("复刻方案"));
    }

    @Test
    @DisplayName("推荐爆款视频 - 应返回 200")
    void recommended_shouldReturn200() throws Exception {
        SvViralVideo video = new SvViralVideo();
        video.setId(1L);
        video.setTitle("推荐视频");

        when(viralVideoService.getRecommendedVirals(eq(1L), eq(10))).thenReturn(List.of(video));

        mockMvc.perform(post("/api/v1/short-video/viral/recommended")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("获取爆款视频详情（缺少 id）- 应返回 1001")
    void get_missingId_shouldReturn1001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/viral/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("爆款视频列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/viral/list")
                        .param("mode", "my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
