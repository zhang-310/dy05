package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.VideoGenerationTaskService;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("VideoGenerationTaskController 集成测试")
class VideoGenerationTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VideoGenerationTaskService taskService;

    @Test
    @DisplayName("提交异步任务 - 应返回 200")
    void submit_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("shotListId", 1L);
        body.put("projectId", 1L);
        body.put("quality", "high");
        body.put("aspectRatio", "16:9");

        Map<String, Object> keyframe = new HashMap<>();
        keyframe.put("shotNumber", 1);
        keyframe.put("imageUrl", "https://example.com/image.jpg");
        body.put("keyframes", List.of(keyframe));

        when(taskService.submitTask(eq(1L), eq(1L), eq(1L), anyList(), eq("high"), eq("16:9")))
                .thenReturn(100L);

        mockMvc.perform(post("/api/v1/short-video/video-task/submit")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.taskId").value(100));
    }

    @Test
    @DisplayName("提交异步任务（未登录）- 应返回 2001")
    void submit_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("keyframes", List.of(Map.of("shotNumber", 1)));

        mockMvc.perform(post("/api/v1/short-video/video-task/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("提交异步任务（缺少 keyframes）- 应返回 1001")
    void submit_missingKeyframes_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("shotListId", 1L);

        mockMvc.perform(post("/api/v1/short-video/video-task/submit")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("查询任务状态 - 应返回 200")
    void status_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 100L);

        VideoGenerationTaskService.TaskStatusVO statusVO =
                new VideoGenerationTaskService.TaskStatusVO(
                        100L,
                        "processing",
                        5,
                        10,
                        "Processing videos",
                        List.of(),
                        null
                );

        when(taskService.getStatus(eq(100L), eq(1L))).thenReturn(statusVO);

        mockMvc.perform(post("/api/v1/short-video/video-task/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.taskId").value(100))
                .andExpect(jsonPath("$.data.status").value("processing"));
    }

    @Test
    @DisplayName("查询任务状态（缺少 taskId）- 应返回 1001")
    void status_missingTaskId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/video-task/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("查询任务状态（未登录）- 应返回 2001")
    void status_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 100L);

        mockMvc.perform(post("/api/v1/short-video/video-task/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("取消任务 - 应返回 204")
    void cancel_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 100L);

        doNothing().when(taskService).cancelTask(eq(100L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/video-task/cancel")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("取消任务（缺少 taskId）- 应返回 1001")
    void cancel_missingTaskId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/video-task/cancel")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("取消任务（未登录）- 应返回 2001")
    void cancel_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 100L);

        mockMvc.perform(post("/api/v1/short-video/video-task/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("重试失败任务 - 应返回 200")
    void retry_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 100L);

        when(taskService.retryTask(eq(100L), eq(1L))).thenReturn(101L);

        mockMvc.perform(post("/api/v1/short-video/video-task/retry")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.taskId").value(101));
    }

    @Test
    @DisplayName("重试失败任务（缺少 taskId）- 应返回 1001")
    void retry_missingTaskId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/video-task/retry")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("重试失败任务（未登录）- 应返回 2001")
    void retry_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 100L);

        mockMvc.perform(post("/api/v1/short-video/video-task/retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
