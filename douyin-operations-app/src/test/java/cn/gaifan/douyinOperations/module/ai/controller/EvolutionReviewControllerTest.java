package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionReviewService;
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
 * EvolutionReviewController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("EvolutionReviewController 集成测试")
class EvolutionReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EvolutionReviewService reviewService;

    @Test
    @DisplayName("审核任务列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "pending");
        body.put("page", 0);
        body.put("rows", 20);

        Map<String, Object> task1 = new HashMap<>();
        task1.put("id", 1L);
        task1.put("status", "pending");
        task1.put("content", "待审核内容");

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>(1L, List.of(task1), 0, 20);

        when(reviewService.listReviewTasks(eq("pending"), eq(0), eq(20)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/evolution-review/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("pending"));
    }

    @Test
    @DisplayName("审核任务列表（默认参数）- 应返回 200")
    void list_defaultParams_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>(0L, List.of(), 0, 20);

        when(reviewService.listReviewTasks(isNull(), eq(0), eq(20)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/evolution-review/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("通过审核 - 应返回 204")
    void approve_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);
        body.put("comment", "审核通过");

        doNothing().when(reviewService).approve(eq(1L), eq(1L), eq("审核通过"));

        mockMvc.perform(post("/api/v1/ai/evolution-review/approve")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("通过审核（无评论）- 应返回 204")
    void approve_withoutComment_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);

        doNothing().when(reviewService).approve(eq(1L), eq(1L), isNull());

        mockMvc.perform(post("/api/v1/ai/evolution-review/approve")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("通过审核（缺少 taskId）- 应返回 1001")
    void approve_missingTaskId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("comment", "审核通过");

        mockMvc.perform(post("/api/v1/ai/evolution-review/approve")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("taskId 不能为空"));
    }

    @Test
    @DisplayName("拒绝审核 - 应返回 204")
    void reject_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 2L);
        body.put("comment", "内容不符合要求");

        doNothing().when(reviewService).reject(eq(2L), eq(1L), eq("内容不符合要求"));

        mockMvc.perform(post("/api/v1/ai/evolution-review/reject")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("拒绝审核（缺少 taskId）- 应返回 1001")
    void reject_missingTaskId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("comment", "拒绝");

        mockMvc.perform(post("/api/v1/ai/evolution-review/reject")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("taskId 不能为空"));
    }

    @Test
    @DisplayName("修订后通过 - 应返回 204")
    void revise_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 3L);
        body.put("revisedContent", "修订后的内容");
        body.put("comment", "已修订");

        doNothing().when(reviewService).revise(eq(3L), eq(1L), eq("修订后的内容"), eq("已修订"));

        mockMvc.perform(post("/api/v1/ai/evolution-review/revise")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("修订后通过（缺少 taskId）- 应返回 1001")
    void revise_missingTaskId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("revisedContent", "修订后的内容");

        mockMvc.perform(post("/api/v1/ai/evolution-review/revise")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("taskId 不能为空"));
    }

    @Test
    @DisplayName("修订后通过（修订内容为空）- 应返回 1001")
    void revise_emptyContent_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 3L);
        body.put("revisedContent", "");

        mockMvc.perform(post("/api/v1/ai/evolution-review/revise")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("修订内容不能为空"));
    }

    @Test
    @DisplayName("审核统计 - 应返回 200")
    void stats_shouldReturn200() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", 10);
        stats.put("approved", 50);
        stats.put("rejected", 5);

        when(reviewService.getReviewStats())
                .thenReturn(stats);

        mockMvc.perform(post("/api/v1/ai/evolution-review/stats")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.pending").value(10))
                .andExpect(jsonPath("$.data.approved").value(50))
                .andExpect(jsonPath("$.data.rejected").value(5));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/evolution-review/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
