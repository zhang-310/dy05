package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.service.LiveScriptCommentService;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptCommentSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptCommentVO;
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

import java.sql.Timestamp;
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
 * LiveScriptCommentController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptCommentController 集成测试")
class LiveScriptCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveScriptCommentService commentService;

    @Test
    @DisplayName("获取话术的评论列表 - 应返回 200")
    void getByScript_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 100L);

        LiveScriptCommentVO commentVO = new LiveScriptCommentVO();
        commentVO.setId(1L);
        commentVO.setScriptId(100L);
        commentVO.setSessionId(10L);
        commentVO.setUserId(1L);
        commentVO.setUserName("测试用户");
        commentVO.setContent("这段话术需要优化");
        commentVO.setResolved(0);
        commentVO.setCreateTime(new Timestamp(System.currentTimeMillis()));

        when(commentService.getByScript(100L)).thenReturn(List.of(commentVO));

        mockMvc.perform(post("/api/v1/live/script-comment/by-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].scriptId").value(100))
                .andExpect(jsonPath("$.data[0].content").value("这段话术需要优化"));
    }

    @Test
    @DisplayName("获取场次的所有评论 - 应返回 200")
    void getBySession_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 10L);

        LiveScriptCommentVO commentVO = new LiveScriptCommentVO();
        commentVO.setId(1L);
        commentVO.setScriptId(100L);
        commentVO.setSessionId(10L);
        commentVO.setContent("场次评论");
        commentVO.setResolved(0);

        when(commentService.getBySession(10L)).thenReturn(List.of(commentVO));

        mockMvc.perform(post("/api/v1/live/script-comment/by-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].sessionId").value(10));
    }

    @Test
    @DisplayName("添加评论 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        LiveScriptCommentSaveVO saveVO = new LiveScriptCommentSaveVO();
        saveVO.setScriptId(100L);
        saveVO.setSessionId(10L);
        saveVO.setContent("这是一条新评论");

        LiveScriptCommentVO commentVO = new LiveScriptCommentVO();
        commentVO.setId(1L);
        commentVO.setScriptId(100L);
        commentVO.setSessionId(10L);
        commentVO.setContent("这是一条新评论");
        commentVO.setResolved(0);

        when(commentService.addComment(any(LiveScriptCommentSaveVO.class), eq(1L)))
                .thenReturn(commentVO);

        mockMvc.perform(post("/api/v1/live/script-comment/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").value("这是一条新评论"));
    }

    @Test
    @DisplayName("添加回复评论 - 应返回 200")
    void save_withParentId_shouldReturn200() throws Exception {
        LiveScriptCommentSaveVO saveVO = new LiveScriptCommentSaveVO();
        saveVO.setScriptId(100L);
        saveVO.setSessionId(10L);
        saveVO.setContent("这是一条回复");
        saveVO.setParentId(5L);

        LiveScriptCommentVO commentVO = new LiveScriptCommentVO();
        commentVO.setId(2L);
        commentVO.setScriptId(100L);
        commentVO.setSessionId(10L);
        commentVO.setContent("这是一条回复");
        commentVO.setParentId(5L);
        commentVO.setResolved(0);

        when(commentService.addComment(any(LiveScriptCommentSaveVO.class), eq(1L)))
                .thenReturn(commentVO);

        mockMvc.perform(post("/api/v1/live/script-comment/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.parentId").value(5));
    }

    @Test
    @DisplayName("标记评论已解决 - 应返回 204")
    void resolve_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("commentId", 1L);

        doNothing().when(commentService).resolveComment(1L, 1L);

        mockMvc.perform(post("/api/v1/live/script-comment/resolve")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除评论 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("commentId", 1L);

        doNothing().when(commentService).deleteComment(1L, 1L);

        mockMvc.perform(post("/api/v1/live/script-comment/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("场次未解决评论数 - 应返回 200")
    void unresolvedCount_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 10L);

        when(commentService.countUnresolved(10L)).thenReturn(5L);

        mockMvc.perform(post("/api/v1/live/script-comment/unresolved-count")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("按话术统计未解决评论数 - 应返回 200")
    void unresolvedByScript_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 10L);

        Map<Long, Long> countMap = new HashMap<>();
        countMap.put(100L, 3L);
        countMap.put(101L, 2L);

        when(commentService.countUnresolvedByScript(10L)).thenReturn(countMap);

        mockMvc.perform(post("/api/v1/live/script-comment/unresolved-by-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.100").value(3))
                .andExpect(jsonPath("$.data.101").value(2));
    }

    @Test
    @DisplayName("获取话术评论缺少 scriptId - 应返回 1001")
    void getByScript_withoutScriptId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/script-comment/by-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 scriptId"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 100L);

        mockMvc.perform(post("/api/v1/live/script-comment/by-script")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
