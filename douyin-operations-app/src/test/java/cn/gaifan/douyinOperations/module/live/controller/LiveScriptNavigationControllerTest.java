package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.service.LiveMonitorService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveScriptNavigationController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptNavigationController 集成测试")
class LiveScriptNavigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveMonitorService liveMonitorService;

    @Test
    @DisplayName("获取当前话术段 - 应返回 200")
    void getCurrentSlot_shouldReturn200() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("slotIndex", 2);
        result.put("scriptId", 100L);
        result.put("scriptType", "product");
        result.put("content", "这款产品非常好用");
        result.put("durationSec", 60);
        result.put("elapsedSec", 15);

        when(liveMonitorService.getCurrentSlot(eq(100L)))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/live/script-navigation/current-slot/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.slotIndex").value(2))
                .andExpect(jsonPath("$.data.scriptId").value(100))
                .andExpect(jsonPath("$.data.scriptType").value("product"));
    }

    @Test
    @DisplayName("获取所有话术列表 - 应返回 200")
    void getScriptSlots_shouldReturn200() throws Exception {
        Map<String, Object> slot1 = new HashMap<>();
        slot1.put("slotIndex", 0);
        slot1.put("scriptId", 1L);
        slot1.put("scriptType", "opening");
        slot1.put("durationSec", 30);

        Map<String, Object> slot2 = new HashMap<>();
        slot2.put("slotIndex", 1);
        slot2.put("scriptId", 2L);
        slot2.put("scriptType", "product");
        slot2.put("durationSec", 60);

        when(liveMonitorService.getScriptSlots(eq(100L)))
                .thenReturn(List.of(slot1, slot2));

        mockMvc.perform(get("/api/v1/live/script-navigation/scripts/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].slotIndex").value(0))
                .andExpect(jsonPath("$.data[0].scriptType").value("opening"))
                .andExpect(jsonPath("$.data[1].slotIndex").value(1))
                .andExpect(jsonPath("$.data[1].scriptType").value("product"));
    }

    @Test
    @DisplayName("获取实时数据 - 应返回 200")
    void getRealtimeMetrics_shouldReturn200() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("viewerCount", 1500);
        result.put("likeCount", 320);
        result.put("commentCount", 85);
        result.put("orderCount", 12);
        result.put("gmv", 5600.0);

        when(liveMonitorService.getRealtimeMetrics(eq(100L)))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/live/script-navigation/metrics/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.viewerCount").value(1500))
                .andExpect(jsonPath("$.data.likeCount").value(320))
                .andExpect(jsonPath("$.data.gmv").value(5600.0));
    }

    @Test
    @DisplayName("跳转到下一话术段 - 应返回 200")
    void nextSlot_shouldReturn200() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("slotIndex", 3);
        result.put("scriptId", 101L);
        result.put("scriptType", "interaction");

        when(liveMonitorService.nextSlot(eq(100L)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/script-navigation/next/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.slotIndex").value(3))
                .andExpect(jsonPath("$.data.scriptId").value(101));
    }

    @Test
    @DisplayName("跳转到指定话术段 - 应返回 200")
    void skipToSlot_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("slotIndex", 5);

        Map<String, Object> result = new HashMap<>();
        result.put("slotIndex", 5);
        result.put("scriptId", 105L);
        result.put("scriptType", "closing");

        when(liveMonitorService.skipToSlot(eq(100L), eq(5)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/script-navigation/skip/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.slotIndex").value(5))
                .andExpect(jsonPath("$.data.scriptId").value(105))
                .andExpect(jsonPath("$.data.scriptType").value("closing"));
    }

    @Test
    @DisplayName("跳转到指定话术段（无 slotIndex）- 应返回 200")
    void skipToSlot_withoutSlotIndex_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        Map<String, Object> result = new HashMap<>();
        result.put("slotIndex", 0);
        result.put("scriptId", 1L);

        when(liveMonitorService.skipToSlot(eq(100L), eq(0)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/live/script-navigation/skip/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.slotIndex").value(0));
    }
}
