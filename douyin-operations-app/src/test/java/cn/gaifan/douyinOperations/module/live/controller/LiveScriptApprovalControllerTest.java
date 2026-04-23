package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptApprovalService;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptApprovalSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptApprovalSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptApprovalVO;
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
 * LiveScriptApprovalController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptApprovalController 集成测试")
class LiveScriptApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveScriptApprovalService approvalService;

    @Test
    @DisplayName("提交话术审核 - 应返回 200")
    void submit_shouldReturn200() throws Exception {
        LiveScriptApprovalSaveVO vo = new LiveScriptApprovalSaveVO();
        vo.setScriptId(1L);
        vo.setComments("请审核");

        LiveScriptApprovalVO result = new LiveScriptApprovalVO();
        result.setId(1L);
        result.setScriptId(1L);
        result.setStatus(0);

        when(approvalService.submit(eq(1L), eq("请审核"), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/live/script-approval/submit")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    @DisplayName("整场批量提交审核 - 应返回 200")
    void submitBySession_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1L);
        body.put("comments", "批量审核");

        LiveScriptApprovalVO approval1 = new LiveScriptApprovalVO();
        approval1.setId(1L);
        approval1.setScriptId(10L);

        LiveScriptApprovalVO approval2 = new LiveScriptApprovalVO();
        approval2.setId(2L);
        approval2.setScriptId(20L);

        when(approvalService.submitBySession(eq(1L), eq("批量审核"), eq(1L)))
                .thenReturn(List.of(approval1, approval2));

        mockMvc.perform(post("/api/v1/live/script-approval/submit-by-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(1))
                .andExpect(jsonPath("$.data.submitted").value(2))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("审批话术（通过）- 应返回 200")
    void review_approve_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("approvalId", 1L);
        body.put("action", "approve");
        body.put("comments", "通过");

        LiveScriptApprovalVO result = new LiveScriptApprovalVO();
        result.setId(1L);
        result.setStatus(1);

        when(approvalService.review(eq(1L), eq("approve"), eq("通过"), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/script-approval/review")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    @DisplayName("审批话术（拒绝）- 应返回 200")
    void review_reject_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("approvalId", 1L);
        body.put("action", "reject");
        body.put("comments", "不符合要求");

        LiveScriptApprovalVO result = new LiveScriptApprovalVO();
        result.setId(1L);
        result.setStatus(2);

        when(approvalService.review(eq(1L), eq("reject"), eq("不符合要求"), eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/script-approval/review")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value(2));
    }

    @Test
    @DisplayName("撤回审核 - 应返回 200")
    void revoke_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 1L);

        doNothing().when(approvalService).revoke(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/live/script-approval/revoke")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("分页查询审核记录 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        LiveScriptApprovalSearchVO vo = new LiveScriptApprovalSearchVO();
        vo.setPage(0);
        vo.setRows(30);

        PageResultVO<LiveScriptApprovalVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(10L);
        pageResult.setList(List.of());

        when(approvalService.search(any())).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/script-approval/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("查询话术审核历史 - 应返回 200")
    void history_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptId", 1L);

        LiveScriptApprovalVO approval1 = new LiveScriptApprovalVO();
        approval1.setId(1L);
        approval1.setStatus(1);

        when(approvalService.history(eq(1L))).thenReturn(List.of(approval1));

        mockMvc.perform(post("/api/v1/live/script-approval/history")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("提交话术审核（未登录）- 应返回 2001")
    void submit_unauthorized_shouldReturn2001() throws Exception {
        LiveScriptApprovalSaveVO vo = new LiveScriptApprovalSaveVO();
        vo.setScriptId(1L);

        mockMvc.perform(post("/api/v1/live/script-approval/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("审批话术（非管理员）- 应返回 2002")
    void review_nonAdmin_shouldReturn2002() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("approvalId", 1L);
        body.put("action", "approve");

        mockMvc.perform(post("/api/v1/live/script-approval/review")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }
}
