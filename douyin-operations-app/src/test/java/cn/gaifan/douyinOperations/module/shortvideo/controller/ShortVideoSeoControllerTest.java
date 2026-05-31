package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.DouyinSeoService;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoSeoController 集成测试")
class ShortVideoSeoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DouyinSeoService douyinSeoService;

    @Test
    @DisplayName("话题标签推荐 - 应返回 200")
    void suggestTags_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "护肤霜保湿效果测评");
        body.put("description", "深度测评护肤霜保湿效果");
        body.put("industry", "美妆护肤");

        List<String> tags = List.of("#护肤", "#保湿", "#测评");

        when(douyinSeoService.suggestTags(eq("护肤霜保湿效果测评"), eq("深度测评护肤霜保湿效果"), eq("美妆护肤")))
                .thenReturn(tags);

        mockMvc.perform(post("/api/v1/short-video/seo/suggest-tags")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("话题标签推荐（未登录）- 应返回 2001")
    void suggestTags_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "护肤霜测评");

        mockMvc.perform(post("/api/v1/short-video/seo/suggest-tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("发布时间推荐 - 应返回 200")
    void suggestPublishTime_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("accountId", 1L);

        List<String> times = List.of("18:00-20:00", "12:00-13:00");

        when(douyinSeoService.suggestPublishTime(eq(1L), anyList()))
                .thenReturn(times);

        mockMvc.perform(post("/api/v1/short-video/seo/suggest-publish-time")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("封面推荐 - 应返回 200")
    void suggestCover_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("frameUrls", List.of("https://example.com/frame1.jpg", "https://example.com/frame2.jpg"));

        String bestCover = "https://example.com/frame2.jpg";

        when(douyinSeoService.suggestCover(anyList()))
                .thenReturn(bestCover);

        mockMvc.perform(post("/api/v1/short-video/seo/suggest-cover")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(bestCover));
    }

    @Test
    @DisplayName("标题 A/B 测试变体 - 应返回 200")
    void suggestAbTestTitles_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("baseTitle", "护肤霜保湿效果惊人");

        List<String> variants = List.of("护肤霜保湿效果惊人！干皮救星", "保湿神器！护肤霜效果测评");

        when(douyinSeoService.suggestAbTestTitles(eq("护肤霜保湿效果惊人")))
                .thenReturn(variants);

        mockMvc.perform(post("/api/v1/short-video/seo/suggest-ab-titles")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("标题 A/B 测试变体（未登录）- 应返回 2001")
    void suggestAbTestTitles_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("baseTitle", "护肤霜测评");

        mockMvc.perform(post("/api/v1/short-video/seo/suggest-ab-titles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
