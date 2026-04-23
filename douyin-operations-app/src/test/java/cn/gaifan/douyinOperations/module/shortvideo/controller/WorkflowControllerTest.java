package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.WorkflowExecutionService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("WorkflowController 集成测试")
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkflowExecutionService workflowExecutionService;

    @Test
    @DisplayName("执行工作流 - 应返回 200")
    void execute_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);
        body.put("startStep", "script");
        body.put("params", Map.of("key", "value"));

        when(workflowExecutionService.executeFrom(eq(1L), eq("script"), anyMap(), eq(1L)))
                .thenReturn("task-123");

        mockMvc.perform(post("/api/v1/short-video/workflow/execute")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.taskId").value("task-123"));
    }

    @Test
    @DisplayName("执行工作流（默认参数）- 应返回 200")
    void execute_defaultParams_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        when(workflowExecutionService.executeFrom(eq(0L), eq("script"), anyMap(), eq(1L)))
                .thenReturn("task-456");

        mockMvc.perform(post("/api/v1/short-video/workflow/execute")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.taskId").value("task-456"));
    }

    @Test
    @DisplayName("查询工作流任务状态 - 应返回 200")
    void status_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", "task-123");

        Map<String, Object> statusResult = new HashMap<>();
        statusResult.put("taskId", "task-123");
        statusResult.put("status", "running");
        statusResult.put("progress", 50);

        when(workflowExecutionService.getStatus(eq("task-123")))
                .thenReturn(statusResult);

        mockMvc.perform(post("/api/v1/short-video/workflow/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.taskId").value("task-123"))
                .andExpect(jsonPath("$.data.status").value("running"));
    }

    @Test
    @DisplayName("查询工作流任务状态（空 taskId）- 应返回 200")
    void status_emptyTaskId_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        Map<String, Object> statusResult = new HashMap<>();
        statusResult.put("error", "taskId not found");

        when(workflowExecutionService.getStatus(eq("")))
                .thenReturn(statusResult);

        mockMvc.perform(post("/api/v1/short-video/workflow/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("AI 微调节点参数 - 应返回 200")
    void aiAssist_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 1L);
        body.put("nodeId", "script-node");
        body.put("userInput", "优化脚本内容");

        Map<String, Object> assistResult = new HashMap<>();
        assistResult.put("nodeId", "script-node");
        assistResult.put("suggestions", "建议增加产品卖点");

        when(workflowExecutionService.aiAssistNode(eq(1L), eq("script-node"), eq("优化脚本内容"), eq(1L)))
                .thenReturn(assistResult);

        mockMvc.perform(post("/api/v1/short-video/workflow/ai-assist")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.nodeId").value("script-node"));
    }

    @Test
    @DisplayName("AI 微调节点参数（默认参数）- 应返回 200")
    void aiAssist_defaultParams_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        Map<String, Object> assistResult = new HashMap<>();
        assistResult.put("message", "no input provided");

        when(workflowExecutionService.aiAssistNode(eq(0L), eq(""), eq(""), eq(1L)))
                .thenReturn(assistResult);

        mockMvc.perform(post("/api/v1/short-video/workflow/ai-assist")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
