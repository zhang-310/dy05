package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDigitalHumanTask;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDigitalHumanTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.DigitalHumanPollingService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DigitalHumanWebhookController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DigitalHumanWebhookController 集成测试")
class DigitalHumanWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SvDigitalHumanTaskRepository taskRepository;

    @MockBean(name = "digitalHumanPollingServiceImpl")
    private DigitalHumanPollingService pollingService;

    @Test
    @DisplayName("HeyGen 回调（成功）- 应返回 200")
    void heygenWebhook_success_shouldReturn200() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", "avatar_video.success");
        Map<String, Object> data = new HashMap<>();
        data.put("video_id", "heygen123");
        data.put("video_url", "https://example.com/video.mp4");
        payload.put("data", data);

        SvDigitalHumanTask task = new SvDigitalHumanTask();
        task.setId(1L);
        task.setProvider("heygen");
        task.setExternalTaskId("heygen123");
        task.setStatus("PROCESSING");

        when(taskRepository.findByProviderAndExternalTaskIdAndDeleted(eq("heygen"), eq("heygen123"), eq(0)))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(SvDigitalHumanTask.class))).thenReturn(task);

        mockMvc.perform(post("/api/v1/short-video/webhooks/heygen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("ok"));

        verify(taskRepository).save(argThat(t ->
            "COMPLETED".equals(t.getStatus()) &&
            "https://example.com/video.mp4".equals(t.getVideoUrl())
        ));
    }

    @Test
    @DisplayName("HeyGen 回调（失败）- 应返回 200")
    void heygenWebhook_failure_shouldReturn200() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", "avatar_video.failed");
        Map<String, Object> data = new HashMap<>();
        data.put("video_id", "heygen123");
        data.put("error", "Generation failed");
        payload.put("data", data);

        SvDigitalHumanTask task = new SvDigitalHumanTask();
        task.setId(1L);
        task.setProvider("heygen");
        task.setExternalTaskId("heygen123");
        task.setStatus("PROCESSING");

        when(taskRepository.findByProviderAndExternalTaskIdAndDeleted(eq("heygen"), eq("heygen123"), eq(0)))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(SvDigitalHumanTask.class))).thenReturn(task);

        mockMvc.perform(post("/api/v1/short-video/webhooks/heygen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("ok"));

        verify(taskRepository).save(argThat(t -> "FAILED".equals(t.getStatus())));
    }

    @Test
    @DisplayName("HeyGen 回调（任务不存在）- 应返回 200")
    void heygenWebhook_taskNotFound_shouldReturn200() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", "avatar_video.success");
        Map<String, Object> data = new HashMap<>();
        data.put("video_id", "unknown");
        payload.put("data", data);

        when(taskRepository.findByProviderAndExternalTaskIdAndDeleted(eq("heygen"), eq("unknown"), eq(0)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/short-video/webhooks/heygen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("ignored"));
    }

    @Test
    @DisplayName("D-ID 回调（成功）- 应返回 200")
    void didWebhook_success_shouldReturn200() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "did123");
        payload.put("status", "done");
        payload.put("result_url", "https://example.com/result.mp4");

        SvDigitalHumanTask task = new SvDigitalHumanTask();
        task.setId(1L);
        task.setProvider("did");
        task.setExternalTaskId("did123");
        task.setStatus("PROCESSING");

        when(taskRepository.findByProviderAndExternalTaskIdAndDeleted(eq("did"), eq("did123"), eq(0)))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(SvDigitalHumanTask.class))).thenReturn(task);

        mockMvc.perform(post("/api/v1/short-video/webhooks/did")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("ok"));

        verify(taskRepository).save(argThat(t ->
            "COMPLETED".equals(t.getStatus()) &&
            "https://example.com/result.mp4".equals(t.getVideoUrl())
        ));
    }

    @Test
    @DisplayName("D-ID 回调（失败）- 应返回 200")
    void didWebhook_failure_shouldReturn200() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "did123");
        payload.put("status", "error");
        payload.put("error", "Processing error");

        SvDigitalHumanTask task = new SvDigitalHumanTask();
        task.setId(1L);
        task.setProvider("did");
        task.setExternalTaskId("did123");
        task.setStatus("PROCESSING");

        when(taskRepository.findByProviderAndExternalTaskIdAndDeleted(eq("did"), eq("did123"), eq(0)))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(SvDigitalHumanTask.class))).thenReturn(task);

        mockMvc.perform(post("/api/v1/short-video/webhooks/did")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("ok"));

        verify(taskRepository).save(argThat(t -> "FAILED".equals(t.getStatus())));
    }

    @Test
    @DisplayName("D-ID 回调（任务不存在）- 应返回 200")
    void didWebhook_taskNotFound_shouldReturn200() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "unknown");
        payload.put("status", "done");

        when(taskRepository.findByProviderAndExternalTaskIdAndDeleted(eq("did"), eq("unknown"), eq(0)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/short-video/webhooks/did")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("ignored"));
    }
}
