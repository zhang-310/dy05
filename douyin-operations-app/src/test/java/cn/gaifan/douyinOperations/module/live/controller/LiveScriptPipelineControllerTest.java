package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptPipeline;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptPipelineService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveScriptPipelineController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptPipelineController 集成测试")
class LiveScriptPipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveScriptPipelineService pipelineService;

    @Test
    @DisplayName("启动流水线 - 应返回 200")
    void start_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("modelId", 1L);
        body.put("style", "专业");
        body.put("useKbRef", true);

        LiveScriptPipeline pipeline = new LiveScriptPipeline();
        pipeline.setId(1L);
        pipeline.setSessionId(100L);
        pipeline.setStatus("pending");
        pipeline.setTotalScripts(10);
        pipeline.setCompletedScripts(0);
        pipeline.setRefinedScripts(0);
        pipeline.setFailedScripts(0);
        pipeline.setOwnerId(1L);
        pipeline.setCreateTime(new Timestamp(System.currentTimeMillis()));

        when(pipelineService.startPipeline(eq(100L), eq(1L), eq("专业"), eq(true), eq(1L)))
                .thenReturn(pipeline);

        mockMvc.perform(post("/api/v1/live/pipeline/start")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(100))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    @DisplayName("启动流水线（缺少 sessionId）- 应返回 1001")
    void start_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("modelId", 1L);

        mockMvc.perform(post("/api/v1/live/pipeline/start")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 sessionId"));
    }

    @Test
    @DisplayName("查询流水线状态 - 应返回 200")
    void status_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        LiveScriptPipeline pipeline = new LiveScriptPipeline();
        pipeline.setId(1L);
        pipeline.setSessionId(100L);
        pipeline.setStatus("generating");
        pipeline.setTotalScripts(10);
        pipeline.setCompletedScripts(5);
        pipeline.setRefinedScripts(2);
        pipeline.setFailedScripts(0);

        when(pipelineService.getPipelineStatus(eq(1L), eq(1L))).thenReturn(pipeline);

        mockMvc.perform(post("/api/v1/live/pipeline/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("generating"))
                .andExpect(jsonPath("$.data.completedScripts").value(5));
    }

    @Test
    @DisplayName("查询流水线状态（缺少 id）- 应返回 1001")
    void status_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/pipeline/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 id"));
    }

    @Test
    @DisplayName("取消流水线 - 应返回 200")
    void cancel_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(pipelineService).cancelPipeline(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/live/pipeline/cancel")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("取消流水线（缺少 id）- 应返回 1001")
    void cancel_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/pipeline/cancel")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 id"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        mockMvc.perform(post("/api/v1/live/pipeline/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
