package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AiCallLogController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AiCallLogController 集成测试")
class AiCallLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiCallLogService aiCallLogService;

    @Test
    @DisplayName("关联调用记录与视频 - 应返回 204")
    void link_withVideoId_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("callLogId", 1L);
        body.put("videoId", 100L);

        doNothing().when(aiCallLogService).linkToPublish(eq(1L), eq(100L), isNull());

        mockMvc.perform(post("/api/v1/ai/call-log/link")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("关联调用记录与直播场次 - 应返回 204")
    void link_withSessionId_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("callLogId", 2L);
        body.put("sessionId", 200L);

        doNothing().when(aiCallLogService).linkToPublish(eq(2L), isNull(), eq(200L));

        mockMvc.perform(post("/api/v1/ai/call-log/link")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("关联调用记录（同时提供 videoId 和 sessionId）- 应返回 204")
    void link_withBothIds_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("callLogId", 3L);
        body.put("videoId", 300L);
        body.put("sessionId", 400L);

        doNothing().when(aiCallLogService).linkToPublish(eq(3L), eq(300L), eq(400L));

        mockMvc.perform(post("/api/v1/ai/call-log/link")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("关联调用记录（缺少 callLogId）- 应返回 1001")
    void link_missingCallLogId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoId", 100L);

        mockMvc.perform(post("/api/v1/ai/call-log/link")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("callLogId 与 videoId/sessionId 至少各提供一个"));
    }

    @Test
    @DisplayName("关联调用记录（缺少 videoId 和 sessionId）- 应返回 1001")
    void link_missingBothIds_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("callLogId", 1L);

        mockMvc.perform(post("/api/v1/ai/call-log/link")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("callLogId 与 videoId/sessionId 至少各提供一个"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("callLogId", 1L);
        body.put("videoId", 100L);

        mockMvc.perform(post("/api/v1/ai/call-log/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
