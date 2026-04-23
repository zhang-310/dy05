package cn.gaifan.douyinOperations.module.workflow.controller;

import cn.gaifan.douyinOperations.module.workflow.service.WorkflowExecutor;
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
    private WorkflowExecutor workflowExecutor;

    @Test
    @DisplayName("执行工作流 - 应返回 200")
    void execute_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("workflowCode", "test_workflow");
        body.put("params", Map.of("key", "value"));

        WorkflowExecutor.WorkflowExecuteResult result =
                new WorkflowExecutor.WorkflowExecuteResult(true, "执行成功", Map.of("output", "data"), null);

        when(workflowExecutor.execute(eq("test_workflow"), anyMap()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/workflow/execute")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @DisplayName("执行工作流（未登录）- 应返回 2001")
    void execute_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("workflowCode", "test_workflow");

        mockMvc.perform(post("/api/v1/workflow/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("执行工作流（缺少 workflowCode）- 应返回 1001")
    void execute_missingWorkflowCode_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/workflow/execute")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
