package cn.gaifan.douyinOperations.module.attribution.controller;

import cn.gaifan.douyinOperations.module.attribution.service.AttributionService;
import cn.gaifan.douyinOperations.module.attribution.vo.AttributionTriggerVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AttributionController 集成测试")
class AttributionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AttributionService attributionService;

    @Test
    @DisplayName("触发归因分析 - 应返回 200")
    void trigger_shouldReturn200() throws Exception {
        AttributionTriggerVO triggerVO = new AttributionTriggerVO();
        triggerVO.setSessionId(1L);

        when(attributionService.triggerAttribution(any(AttributionTriggerVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/attribution/trigger")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(triggerVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("触发归因分析（未登录）- 应返回 2001")
    void trigger_unauthorized_shouldReturn2001() throws Exception {
        AttributionTriggerVO triggerVO = new AttributionTriggerVO();
        triggerVO.setSessionId(1L);

        mockMvc.perform(post("/api/v1/attribution/trigger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(triggerVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取场次归因数据 - 应返回 200")
    void getBySession_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("sessionId", 1L);

        List<Map<String, Object>> data = List.of(Map.of("id", 1L, "value", "test"));

        when(attributionService.getBySessionId(eq(1L)))
                .thenReturn(data);

        mockMvc.perform(post("/api/v1/attribution/session")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取场次归因数据（未登录）- 应返回 2001")
    void getBySession_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("sessionId", 1L);

        mockMvc.perform(post("/api/v1/attribution/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取场次归因数据（缺少 sessionId）- 应返回 1001")
    void getBySession_missingSessionId_shouldReturn1001() throws Exception {
        Map<String, Long> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/attribution/session")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("获取归因汇总 - 应返回 200")
    void getSummary_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("sessionId", 1L);

        Map<String, Object> data = Map.of("total", 100, "count", 10);

        when(attributionService.getSummary(eq(1L)))
                .thenReturn(data);

        mockMvc.perform(post("/api/v1/attribution/summary")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取归因汇总（未登录）- 应返回 2001")
    void getSummary_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("sessionId", 1L);

        mockMvc.perform(post("/api/v1/attribution/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取归因详情 - 应返回 200")
    void getById_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("id", 1L);

        Map<String, Object> data = Map.of("id", 1L, "details", "test");

        when(attributionService.getById(eq(1L)))
                .thenReturn(data);

        mockMvc.perform(post("/api/v1/attribution/get")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取归因详情（未登录）- 应返回 2001")
    void getById_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/attribution/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除场次归因数据 - 应返回 204")
    void deleteBySession_shouldReturn204() throws Exception {
        doNothing().when(attributionService).deleteBySessionId(eq(1L));

        mockMvc.perform(delete("/api/v1/attribution/session/1")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除场次归因数据（未登录）- 应返回 2001")
    void deleteBySession_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(delete("/api/v1/attribution/session/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
