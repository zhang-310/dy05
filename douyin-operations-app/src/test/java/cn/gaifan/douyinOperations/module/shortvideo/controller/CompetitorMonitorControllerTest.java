package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.CompetitorMonitorService;
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
 * CompetitorMonitorController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CompetitorMonitorController 集成测试")
class CompetitorMonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompetitorMonitorService competitorMonitorService;

    @Test
    @DisplayName("添加竞品账号 - 应返回 200")
    void add_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("accountId", "123456");
        body.put("accountName", "竞品账号");
        body.put("platform", "douyin");

        doNothing().when(competitorMonitorService).addCompetitor(eq(1L), eq("123456"), eq("竞品账号"), eq("douyin"));

        mockMvc.perform(post("/api/v1/short-video/competitor/add")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("添加成功"));
    }

    @Test
    @DisplayName("添加竞品账号（未登录）- 应返回 2001")
    void add_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("accountId", "123456");

        mockMvc.perform(post("/api/v1/short-video/competitor/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("竞品列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        when(competitorMonitorService.listCompetitors(eq(1L))).thenReturn(List.of(
                Map.of("id", 1L, "accountId", "123456", "accountName", "竞品账号", "platform", "douyin")
        ));

        mockMvc.perform(post("/api/v1/short-video/competitor/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("分析竞品 - 应返回 200")
    void analyze_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("competitorId", 1L);

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("competitorId", 1L);
        analysis.put("totalVideos", 100);
        analysis.put("avgViews", 50000);
        analysis.put("topTopics", List.of("美妆", "护肤"));

        when(competitorMonitorService.analyzeCompetitor(eq(1L), eq(1L))).thenReturn(analysis);

        mockMvc.perform(post("/api/v1/short-video/competitor/analyze")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.competitorId").value(1))
                .andExpect(jsonPath("$.data.totalVideos").value(100));
    }

    @Test
    @DisplayName("生成周报 - 应返回 200")
    void weeklyReport_shouldReturn200() throws Exception {
        when(competitorMonitorService.generateWeeklyReport(eq(1L)))
                .thenReturn("本周竞品分析报告：...");

        mockMvc.perform(post("/api/v1/short-video/competitor/weekly-report")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("本周竞品分析报告：..."));
    }

    @Test
    @DisplayName("删除竞品账号 - 应返回 204")
    void remove_shouldReturn204() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("competitorId", 1L);

        doNothing().when(competitorMonitorService).removeCompetitor(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/competitor/remove")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除竞品账号（缺少 competitorId）- 应返回 1001")
    void remove_missingCompetitorId_shouldReturn1001() throws Exception {
        Map<String, Long> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/competitor/remove")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("竞品列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/competitor/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
