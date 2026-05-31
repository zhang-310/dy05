package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionShortVideoExportService;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LiveSessionController 集成测试
 * 测试直播场次管理的核心功能：搜索、详情、保存、删除、状态更新
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveSessionController 集成测试")
class LiveSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveSessionService liveSessionService;

    @MockBean
    private LiveSessionShortVideoExportService liveSessionShortVideoExportService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @BeforeEach
    void setUp() {
        // Mock 数据权限服务
        when(dataScopeService.getVisibleUserIds(anyLong(), anyString())).thenReturn(List.of(1L));
    }

    @Test
    @DisplayName("查询直播场次 - 200")
    void search_shouldReturn200() throws Exception {
        LiveSessionSearchVO searchVO = new LiveSessionSearchVO();
        searchVO.setKeyword("护肤品直播");

        LiveSessionVO sessionVO = new LiveSessionVO();
        sessionVO.setId(1L);
        sessionVO.setLiveTitle("护肤品直播场次");
        sessionVO.setStatus(0);
        sessionVO.setViewers(1000);

        PageResultVO<LiveSessionVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(sessionVO));

        when(liveSessionService.search(any(LiveSessionSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/session/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].liveTitle").value("护肤品直播场次"));
    }

    @Test
    @DisplayName("获取直播场次详情 - 200")
    void get_shouldReturn200() throws Exception {
        LiveSessionVO sessionVO = new LiveSessionVO();
        sessionVO.setId(1L);
        sessionVO.setLiveTitle("护肤品直播场次");
        sessionVO.setLiveDescription("春季护肤品专场");
        sessionVO.setStatus(0);
        sessionVO.setViewers(1000);

        when(liveSessionService.getByIdWithScope(eq(1L), anyList())).thenReturn(sessionVO);

        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/live/session/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.liveTitle").value("护肤品直播场次"));
    }

    @Test
    @DisplayName("保存直播场次 - 200")
    void save_shouldReturn200() throws Exception {
        LiveSessionSaveVO saveVO = new LiveSessionSaveVO();
        saveVO.setUserId(1L);
        saveVO.setLiveTitle("新直播场次");
        saveVO.setLiveDescription("测试直播");
        saveVO.setStatus(0);

        when(liveSessionService.save(any(LiveSessionSaveVO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/session/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除直播场次 - 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/live/session/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    @DisplayName("场次数据概览 - 200")
    void getOverview_shouldReturn200() throws Exception {
        LiveSessionOverviewVO overviewVO = new LiveSessionOverviewVO();
        LiveSessionVO sessionVO = new LiveSessionVO();
        sessionVO.setId(1L);
        overviewVO.setSession(sessionVO);

        when(liveSessionService.getByIdWithScope(eq(1L), anyList())).thenReturn(new LiveSessionVO());
        when(liveSessionService.getOverview(1L)).thenReturn(overviewVO);

        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/live/session/overview")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.session.id").value(1));
    }

    @Test
    @DisplayName("开播准备清单 - 200")
    void getReadiness_shouldReturn200() throws Exception {
        LiveReadinessVO readinessVO = new LiveReadinessVO();
        readinessVO.setReady(true);

        when(liveSessionService.getReadiness(1L)).thenReturn(readinessVO);

        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/live/session/readiness")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.ready").value(true));
    }

    @Test
    @DisplayName("更新直播场次状态 - 200")
    void updateStatus_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("status", 1);

        mockMvc.perform(post("/api/v1/live/session/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("修改成功"));
    }

    @Test
    @DisplayName("更新观看人数 - 200")
    void updateViewers_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("viewers", 5000);

        mockMvc.perform(post("/api/v1/live/session/viewers")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("修改成功"));
    }

    @Test
    @DisplayName("更新点赞数 - 200")
    void updateLikes_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("likes", 10000L);

        mockMvc.perform(post("/api/v1/live/session/likes")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("修改成功"));
    }

    @Test
    @DisplayName("直播历史趋势分析 - 200")
    void analyzeTrend_shouldReturn200() throws Exception {
        LiveTrendRequestVO requestVO = new LiveTrendRequestVO();
        requestVO.setAccountId(1L);
        requestVO.setStartDate("2026-01-01");
        requestVO.setEndDate("2026-01-31");

        LiveTrendResultVO resultVO = new LiveTrendResultVO();
        resultVO.setAccountId(1L);

        when(liveSessionService.analyzeTrend(any(LiveTrendRequestVO.class), anyLong())).thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/live/session/trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accountId").value(1));
    }

    @Test
    @DisplayName("克隆场次 - 200")
    void clone_shouldReturn200() throws Exception {
        when(liveSessionService.cloneSession(eq(1L), eq(1L), anyString())).thenReturn(2L);

        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);
        body.put("newTitle", "克隆的直播场次");

        mockMvc.perform(post("/api/v1/live/session/clone")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @DisplayName("场次导出短视频 - 200")
    void exportToShortVideo_shouldReturn200() throws Exception {
        when(liveSessionShortVideoExportService.exportToShortVideoProject(eq(1L), eq(1L), eq("viral")))
                .thenReturn(new LiveSessionExportToShortVideoResultVO(10L, 100L));

        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);
        body.put("style", "viral");

        mockMvc.perform(post("/api/v1/live/session/export-to-short-video")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptId").value(10))
                .andExpect(jsonPath("$.data.projectId").value(100));
    }

    @Test
    @DisplayName("未登录访问 - 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/live/session/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
