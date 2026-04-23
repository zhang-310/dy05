package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContentCalendarService;
import cn.gaifan.douyinOperations.module.shortvideo.service.PublishTimeRecommendationService;
import cn.gaifan.douyinOperations.module.shortvideo.service.impl.SvCategoryServiceImpl;
import cn.gaifan.douyinOperations.module.shortvideo.service.impl.SvCommentServiceImpl;
import cn.gaifan.douyinOperations.module.shortvideo.service.impl.SvVideoServiceImpl;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
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
 * ShortVideoController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoController 集成测试")
class ShortVideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SvVideoServiceImpl svVideoService;

    @MockBean
    private SvCommentServiceImpl svCommentService;

    @MockBean
    private SvCategoryServiceImpl svCategoryService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @MockBean
    private PublishTimeRecommendationService publishTimeRecommendationService;

    @MockBean
    private ContentCalendarService contentCalendarService;

    @Test
    @DisplayName("分页搜索视频 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        SvVideoSearchVO vo = new SvVideoSearchVO();
        vo.setPage(0);
        vo.setRows(30);

        PageResultVO<SvVideoVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(10L);
        pageResult.setList(List.of());

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(svVideoService.search(any())).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/content/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("获取视频详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        SvVideoVO video = new SvVideoVO();
        video.setId(1L);
        video.setTitle("测试视频");

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(svVideoService.getById(eq(1L), anyList())).thenReturn(video);

        mockMvc.perform(post("/api/v1/short-video/content/get")
                        .param("id", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("测试视频"));
    }

    @Test
    @DisplayName("新增视频 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        SvVideoSaveVO vo = new SvVideoSaveVO();
        vo.setTitle("新视频");
        vo.setAccountId(1L);

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(svVideoService.save(any(), anyList())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/short-video/content/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除视频 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        doNothing().when(svVideoService).delete(eq(1L), anyList());

        mockMvc.perform(post("/api/v1/short-video/content/delete")
                        .param("id", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("递增播放量 - 应返回 200")
    void incrementViewCount_shouldReturn200() throws Exception {
        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        doNothing().when(svVideoService).incrementViewCount(eq(1L), anyList());

        mockMvc.perform(post("/api/v1/short-video/content/increment-view-count")
                        .param("id", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("视频数据趋势 - 应返回 200")
    void dataTrend_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoId", 1L);

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(svVideoService.getDataTrend(eq(1L), anyList())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/short-video/content/data-trend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("内容日历视图 - 应返回 200")
    void contentCalendar_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("year", 2026);
        body.put("month", 4);

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(contentCalendarService.getCalendarView(eq(2026), eq(4), anyList())).thenReturn(Map.of());

        mockMvc.perform(post("/api/v1/short-video/content/calendar")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("内容日历统计 - 应返回 200")
    void contentCalendarStats_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("year", 2026);
        body.put("month", 4);

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(contentCalendarService.getCalendarStats(eq(2026), eq(4), anyList())).thenReturn(Map.of());

        mockMvc.perform(post("/api/v1/short-video/content/calendar-stats")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("发布时间推荐 - 应返回 200")
    void publishTimeRecommend_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("accountId", 1L);

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(publishTimeRecommendationService.getRecommendedTimes(eq(1L), anyList())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/short-video/content/publish-time-recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取分类列表 - 应返回 200")
    void categoryList_shouldReturn200() throws Exception {
        when(svCategoryService.listByOwner(eq(1L))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/short-video/category/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取分类详情 - 应返回 200")
    void categoryGet_shouldReturn200() throws Exception {
        SvCategoryVO category = new SvCategoryVO();
        category.setId(1L);
        category.setName("测试分类");

        when(svCategoryService.getById(eq(1L), eq(1L))).thenReturn(category);

        mockMvc.perform(post("/api/v1/short-video/category/get")
                        .param("id", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("保存分类 - 应返回 200")
    void categorySave_shouldReturn200() throws Exception {
        SvCategorySaveVO vo = new SvCategorySaveVO();
        vo.setName("新分类");
        vo.setOwnerId(1L);

        when(svCategoryService.save(any(), eq(1L))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/short-video/category/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除分类 - 应返回 200")
    void categoryDelete_shouldReturn200() throws Exception {
        doNothing().when(svCategoryService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/category/delete")
                        .param("id", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("分页搜索评论 - 应返回 200")
    void commentSearch_shouldReturn200() throws Exception {
        SvCommentSearchVO vo = new SvCommentSearchVO();
        vo.setPage(0);
        vo.setRows(30);

        PageResultVO<SvCommentVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(5L);
        pageResult.setList(List.of());

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(svCommentService.search(any(), anyList())).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/comment/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(5));
    }

    @Test
    @DisplayName("获取评论详情 - 应返回 200")
    void commentGet_shouldReturn200() throws Exception {
        SvCommentVO comment = new SvCommentVO();
        comment.setId(1L);

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(svCommentService.getById(eq(1L), anyList())).thenReturn(comment);

        mockMvc.perform(post("/api/v1/short-video/comment/get")
                        .param("id", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("保存评论 - 应返回 200")
    void commentSave_shouldReturn200() throws Exception {
        SvCommentSaveVO vo = new SvCommentSaveVO();
        vo.setVideoId(1L);
        vo.setContent("测试评论");

        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        when(svCommentService.save(any(), anyList())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/short-video/comment/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除评论 - 应返回 200")
    void commentDelete_shouldReturn200() throws Exception {
        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        doNothing().when(svCommentService).delete(eq(1L), anyList());

        mockMvc.perform(post("/api/v1/short-video/comment/delete")
                        .param("id", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("评论点赞 - 应返回 200")
    void commentLike_shouldReturn200() throws Exception {
        when(dataScopeService.getVisibleUserIds(eq(1L), eq("user"))).thenReturn(List.of(1L));
        doNothing().when(svCommentService).incrementLikeCount(eq(1L), anyList());

        mockMvc.perform(post("/api/v1/short-video/comment/increment-like-count")
                        .param("id", "1")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("搜索视频（未登录）- 应返回 2001")
    void search_unauthorized_shouldReturn2001() throws Exception {
        SvVideoSearchVO vo = new SvVideoSearchVO();

        mockMvc.perform(post("/api/v1/short-video/content/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
