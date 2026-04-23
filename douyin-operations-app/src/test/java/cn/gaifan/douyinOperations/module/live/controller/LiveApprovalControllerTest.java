package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.entity.LiveApprovalLog;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.service.LiveApprovalService;
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
 * LiveApprovalController 集成测试
 * 注意：此 Controller 已标记为 @Deprecated，但仍需测试以确保向后兼容
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveApprovalController 集成测试")
class LiveApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveApprovalService liveApprovalService;

    @Test
    @DisplayName("提交审批 - 应返回 200")
    void submit_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);

        doNothing().when(liveApprovalService).submitForApproval(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/live/approval/submit")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("已提交审批"));
    }

    @Test
    @DisplayName("审批通过 - 应返回 200")
    void approve_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);
        body.put("scriptId", 10L);
        body.put("comment", "审批通过");

        doNothing().when(liveApprovalService).approve(eq(1L), eq(10L), eq(1L), eq("审批通过"));

        mockMvc.perform(post("/api/v1/live/approval/approve")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("审批通过"));
    }

    @Test
    @DisplayName("审批拒绝 - 应返回 200")
    void reject_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);
        body.put("scriptId", 10L);
        body.put("comment", "内容不符合规范");

        doNothing().when(liveApprovalService).reject(eq(1L), eq(10L), eq(1L), eq("内容不符合规范"));

        mockMvc.perform(post("/api/v1/live/approval/reject")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("审批已拒绝"));
    }

    @Test
    @DisplayName("审批历史 - 应返回 200")
    void history_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);

        LiveApprovalLog log1 = new LiveApprovalLog();
        log1.setId(1L);
        log1.setSessionId(1L);
        log1.setScriptId(10L);
        log1.setAction("submit");
        log1.setOperatorId(1L);
        log1.setComment("提交审批");
        log1.setCreateTime(new Timestamp(System.currentTimeMillis()));

        LiveApprovalLog log2 = new LiveApprovalLog();
        log2.setId(2L);
        log2.setSessionId(1L);
        log2.setScriptId(10L);
        log2.setAction("approve");
        log2.setOperatorId(2L);
        log2.setComment("审批通过");
        log2.setCreateTime(new Timestamp(System.currentTimeMillis()));

        when(liveApprovalService.getApprovalHistory(1L)).thenReturn(List.of(log1, log2));

        mockMvc.perform(post("/api/v1/live/approval/history")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].action").value("submit"))
                .andExpect(jsonPath("$.data[1].action").value("approve"));
    }

    @Test
    @DisplayName("待审批列表 - 应返回 200")
    void pending_shouldReturn200() throws Exception {
        LiveSession session1 = new LiveSession();
        session1.setId(1L);
        session1.setUserId(1L);
        session1.setLiveTitle("待审批场次1");
        session1.setStatus(1);

        LiveSession session2 = new LiveSession();
        session2.setId(2L);
        session2.setUserId(1L);
        session2.setLiveTitle("待审批场次2");
        session2.setStatus(1);

        when(liveApprovalService.getPendingApprovals(1L)).thenReturn(List.of(session1, session2));

        mockMvc.perform(post("/api/v1/live/approval/pending")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].liveTitle").value("待审批场次1"))
                .andExpect(jsonPath("$.data[1].liveTitle").value("待审批场次2"));
    }

    @Test
    @DisplayName("提交审批缺少 sessionId - 应返回 1001")
    void submit_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/approval/submit")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 sessionId"));
    }

    @Test
    @DisplayName("审批通过缺少 scriptId - 应返回 1001")
    void approve_withoutScriptId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);

        mockMvc.perform(post("/api/v1/live/approval/approve")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 scriptId"));
    }
}
