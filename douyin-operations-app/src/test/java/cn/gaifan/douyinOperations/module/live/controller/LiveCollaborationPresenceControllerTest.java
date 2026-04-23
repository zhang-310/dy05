package cn.gaifan.douyinOperations.module.live.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
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

/**
 * LiveCollaborationPresenceController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveCollaborationPresenceController 集成测试")
class LiveCollaborationPresenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @MockBean
    private HashOperations<String, Object, Object> hashOperations;

    @Test
    @DisplayName("加入场次 - 应返回 200")
    void join_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("userName", "TestUser");

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        mockMvc.perform(post("/api/v1/live/collaboration/join")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("加入场次（缺少 sessionId）- 应返回 1001")
    void join_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userName", "TestUser");

        mockMvc.perform(post("/api/v1/live/collaboration/join")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 sessionId"));
    }

    @Test
    @DisplayName("离开场次 - 应返回 200")
    void leave_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        mockMvc.perform(post("/api/v1/live/collaboration/leave")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("离开场次（缺少 sessionId）- 应返回 1001")
    void leave_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/collaboration/leave")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 sessionId"));
    }

    @Test
    @DisplayName("查看在线用户 - 应返回 200")
    void viewers_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(Map.of());

        mockMvc.perform(post("/api/v1/live/collaboration/viewers")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("查看在线用户（缺少 sessionId）- 应返回 1001")
    void viewers_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/collaboration/viewers")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 sessionId"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        mockMvc.perform(post("/api/v1/live/collaboration/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
