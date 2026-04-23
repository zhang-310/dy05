package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvHotTopic;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvHotTopicRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.CrossModuleContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
 * CrossModuleContentController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CrossModuleContentController 集成测试")
class CrossModuleContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CrossModuleContentService crossModuleContentService;

    @MockBean
    private SvHotTopicRepository svHotTopicRepository;

    @Test
    @DisplayName("直播话术转短视频脚本 - 应返回 200")
    void liveToVideo_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("liveScriptId", 1L);

        when(crossModuleContentService.convertLiveScriptToVideoScript(eq(1L), eq(1L)))
                .thenReturn("转换后的短视频脚本内容...");

        mockMvc.perform(post("/api/v1/short-video/cross/live-to-video")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("转换后的短视频脚本内容..."));
    }

    @Test
    @DisplayName("直播话术转短视频脚本（缺少 liveScriptId）- 应返回 1001")
    void liveToVideo_missingLiveScriptId_shouldReturn1001() throws Exception {
        Map<String, Long> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/cross/live-to-video")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("直播话术转短视频脚本（未登录）- 应返回 2001")
    void liveToVideo_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("liveScriptId", 1L);

        mockMvc.perform(post("/api/v1/short-video/cross/live-to-video")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("查询直播关联的短视频引流效果预览 - 应返回 200")
    void videoPreviewForLive_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);

        SvHotTopic topic = new SvHotTopic();
        topic.setId(1L);
        topic.setTitle("热门话题1");
        topic.setHeatScore(95L);
        topic.setSource("douyin");
        topic.setCategory("beauty");
        topic.setStatus("active");

        when(svHotTopicRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(topic)));

        mockMvc.perform(post("/api/v1/short-video/cross/video-preview-for-live")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(1))
                .andExpect(jsonPath("$.data.relatedHotTopics").isArray())
                .andExpect(jsonPath("$.data.relatedHotTopics[0].title").value("热门话题1"));
    }

    @Test
    @DisplayName("统一热点池 - 应返回 200")
    void hotTopicPool_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("limit", 10);

        SvHotTopic topic1 = new SvHotTopic();
        topic1.setId(1L);
        topic1.setTitle("热门话题1");
        topic1.setHeatScore(95L);
        topic1.setSource("douyin");
        topic1.setCategory("beauty");
        topic1.setStatus("active");

        SvHotTopic topic2 = new SvHotTopic();
        topic2.setId(2L);
        topic2.setTitle("热门话题2");
        topic2.setHeatScore(88L);
        topic2.setSource("weibo");
        topic2.setCategory("fashion");
        topic2.setStatus("active");

        when(svHotTopicRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(topic1, topic2)));

        mockMvc.perform(post("/api/v1/short-video/cross/hot-topic-pool")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.hotTopics").isArray())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.hotTopics[0].topic").value("热门话题1"));
    }

    @Test
    @DisplayName("统一热点池（无参数）- 应返回 200")
    void hotTopicPool_noParams_shouldReturn200() throws Exception {
        when(svHotTopicRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(post("/api/v1/short-video/cross/hot-topic-pool")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.hotTopics").isArray())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    @DisplayName("统一热点池（未登录）- 应返回 2001")
    void hotTopicPool_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/cross/hot-topic-pool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
