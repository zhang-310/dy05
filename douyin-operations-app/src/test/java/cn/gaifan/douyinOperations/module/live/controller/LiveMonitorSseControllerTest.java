package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveMonitorSseController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveMonitorSseController 集成测试")
class LiveMonitorSseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveMonitorRepository liveMonitorRepository;

    @MockBean
    private LiveSessionRepository liveSessionRepository;

    @Test
    @DisplayName("SSE 流连接（已认证）- 应返回 200")
    void stream_withAuth_shouldReturn200() throws Exception {
        LiveMonitor monitor = new LiveMonitor();
        monitor.setId(1L);
        monitor.setSessionId(100L);
        monitor.setTimestamp(new Timestamp(System.currentTimeMillis()));
        monitor.setViewers(500);
        monitor.setLikes(1000L);

        when(liveMonitorRepository.findBySessionId(eq(100L))).thenReturn(List.of(monitor));

        mockMvc.perform(get("/api/v1/live/monitor/stream/100")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SSE 流连接（未认证）- 应返回错误事件")
    void stream_withoutAuth_shouldReturnErrorEvent() throws Exception {
        mockMvc.perform(get("/api/v1/live/monitor/stream/100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取监控快照 - 应返回 200")
    void snapshot_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        LiveMonitor monitor1 = new LiveMonitor();
        monitor1.setId(1L);
        monitor1.setSessionId(100L);
        monitor1.setTimestamp(new Timestamp(System.currentTimeMillis() - 10000));
        monitor1.setViewers(500);
        monitor1.setLikes(1000L);
        monitor1.setComments(50);
        monitor1.setShares(10);
        monitor1.setProductImpressions(200);

        LiveMonitor monitor2 = new LiveMonitor();
        monitor2.setId(2L);
        monitor2.setSessionId(100L);
        monitor2.setTimestamp(new Timestamp(System.currentTimeMillis()));
        monitor2.setViewers(600);
        monitor2.setLikes(1200L);
        monitor2.setComments(60);
        monitor2.setShares(15);
        monitor2.setProductImpressions(250);

        when(liveMonitorRepository.findBySessionId(eq(100L))).thenReturn(List.of(monitor1, monitor2));

        mockMvc.perform(post("/api/v1/live/monitor/snapshot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(100))
                .andExpect(jsonPath("$.viewers").value(600))
                .andExpect(jsonPath("$.peakViewers").value(600))
                .andExpect(jsonPath("$.likes").value(1200))
                .andExpect(jsonPath("$.totalLikes").value(1200))
                .andExpect(jsonPath("$.dataPoints").value(2));
    }

    @Test
    @DisplayName("获取监控快照（空数据）- 应返回默认值")
    void snapshot_withEmptyData_shouldReturnDefaults() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        when(liveMonitorRepository.findBySessionId(eq(100L))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/live/monitor/snapshot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(100))
                .andExpect(jsonPath("$.viewers").value(0))
                .andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.dataPoints").value(0));
    }

    @Test
    @DisplayName("获取监控快照（缺少 sessionId）- 应返回 1001")
    void snapshot_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/monitor/snapshot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("推送监控数据（缺少 sessionId）- 应返回失败")
    void pushData_withoutSessionId_shouldReturnFailure() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("viewers", 500);

        mockMvc.perform(post("/api/v1/live/monitor/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("sessionId 不能为空"));
    }
}
