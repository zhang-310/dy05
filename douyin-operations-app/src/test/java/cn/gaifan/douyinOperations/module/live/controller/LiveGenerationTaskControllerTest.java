package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationTask;
import cn.gaifan.douyinOperations.module.live.service.LiveGenerationTaskService;
import cn.gaifan.douyinOperations.module.live.vo.LiveGenerationTaskVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveGenerationTaskController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveGenerationTaskController 集成测试")
class LiveGenerationTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveGenerationTaskService liveGenerationTaskService;

    @Test
    @DisplayName("获取最新生成任务 - 应返回 200")
    void latest_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        LiveGenerationTaskVO taskVO = new LiveGenerationTaskVO();
        taskVO.setId(1L);
        taskVO.setSessionId(100L);
        taskVO.setStatus("RUNNING");
        taskVO.setTotalSlots(10);
        taskVO.setCompletedSlots(5);
        taskVO.setFailedSlots(0);

        when(liveGenerationTaskService.getLatestBySession(eq(100L))).thenReturn(taskVO);

        mockMvc.perform(post("/api/v1/live/generation-task/latest")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(100))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.completedSlots").value(5));
    }

    @Test
    @DisplayName("获取最新生成任务（缺少 sessionId）- 应返回 1001")
    void latest_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/generation-task/latest")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 sessionId"));
    }

    @Test
    @DisplayName("创建生成任务 - 应返回 200")
    void create_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("totalSlots", 10);
        body.put("style", "专业");
        body.put("modelId", 1L);
        body.put("useKbRef", true);
        body.put("hotKeywords", List.of("护肤", "美白"));

        LiveGenerationTask task = new LiveGenerationTask();
        task.setId(1L);
        task.setSessionId(100L);
        task.setStatus("PENDING");
        task.setTotalSlots(10);
        task.setCompletedSlots(0);
        task.setFailedSlots(0);
        task.setStyle("专业");
        task.setModelId(1L);
        task.setUseKbRef(true);
        task.setOwnerId(1L);
        task.setCreateTime(new Timestamp(System.currentTimeMillis()));
        task.setUpdateTime(new Timestamp(System.currentTimeMillis()));

        when(liveGenerationTaskService.createTask(
                eq(100L), eq(1L), eq(10), eq("专业"), eq(1L), eq(true), anyList()))
                .thenReturn(task);

        mockMvc.perform(post("/api/v1/live/generation-task/create")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(100))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalSlots").value(10));
    }

    @Test
    @DisplayName("创建生成任务（缺少 sessionId）- 应返回 1001")
    void create_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("totalSlots", 10);

        mockMvc.perform(post("/api/v1/live/generation-task/create")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 sessionId"));
    }

    @Test
    @DisplayName("更新生成任务进度 - 应返回 204")
    void updateProgress_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);
        body.put("completedSlots", 5);
        body.put("failedSlots", 1);

        mockMvc.perform(post("/api/v1/live/generation-task/update-progress")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("更新生成任务进度（缺少 taskId）- 应返回 1001")
    void updateProgress_withoutTaskId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("completedSlots", 5);

        mockMvc.perform(post("/api/v1/live/generation-task/update-progress")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 taskId"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        mockMvc.perform(post("/api/v1/live/generation-task/latest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
