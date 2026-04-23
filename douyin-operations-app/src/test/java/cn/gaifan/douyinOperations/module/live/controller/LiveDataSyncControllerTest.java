package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.service.DouyinLiveDataSyncService;
import cn.gaifan.douyinOperations.module.live.service.LiveDataSyncService;
import cn.gaifan.douyinOperations.module.live.vo.*;
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveDataSyncController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveDataSyncController 集成测试")
class LiveDataSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveDataSyncService liveDataSyncService;

    @MockBean
    private DouyinLiveDataSyncService douyinLiveDataSyncService;

    @Test
    @DisplayName("获取场次汇总数据 - 应返回 200")
    void getSessionData_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("sessionId", 100L);

        LiveSessionDataVO dataVO = new LiveSessionDataVO();
        dataVO.setId(1L);
        dataVO.setSessionId(100L);
        dataVO.setTotalViewers(5000);
        dataVO.setPeakViewers(1200);
        dataVO.setTotalLikes(8000L);
        dataVO.setTotalRevenue(new BigDecimal("50000.00"));
        dataVO.setSyncTime(new Timestamp(System.currentTimeMillis()));

        when(liveDataSyncService.getSessionData(eq(100L))).thenReturn(dataVO);

        mockMvc.perform(post("/api/v1/live/data/session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(100))
                .andExpect(jsonPath("$.data.totalViewers").value(5000));
    }

    @Test
    @DisplayName("获取场次汇总数据（缺少 sessionId）- 应返回 1001")
    void getSessionData_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Long> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/data/session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 sessionId"));
    }

    @Test
    @DisplayName("保存场次汇总数据 - 应返回 200")
    void saveSessionData_shouldReturn200() throws Exception {
        LiveSessionDataSaveVO saveVO = new LiveSessionDataSaveVO();
        saveVO.setSessionId(100L);
        saveVO.setTotalViewers(5000);
        saveVO.setPeakViewers(1200);
        saveVO.setTotalRevenue(new BigDecimal("50000.00"));

        LiveSessionDataVO dataVO = new LiveSessionDataVO();
        dataVO.setId(1L);
        dataVO.setSessionId(100L);
        dataVO.setTotalViewers(5000);

        when(liveDataSyncService.saveSessionData(any(LiveSessionDataSaveVO.class)))
                .thenReturn(dataVO);

        mockMvc.perform(post("/api/v1/live/data/session/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(100));
    }

    @Test
    @DisplayName("从监控数据同步场次汇总 - 应返回 200")
    void syncSessionData_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        LiveSessionDataVO dataVO = new LiveSessionDataVO();
        dataVO.setId(1L);
        dataVO.setSessionId(100L);
        dataVO.setTotalViewers(5000);

        when(liveDataSyncService.syncSessionData(eq(100L))).thenReturn(dataVO);

        mockMvc.perform(post("/api/v1/live/data/session/sync")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(100));
    }

    @Test
    @DisplayName("从抖音 API 同步直播数据 - 应返回 200")
    void syncFromDouyin_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("roomId", "room123");
        body.put("accessToken", "token123");

        LiveSessionDataVO dataVO = new LiveSessionDataVO();
        dataVO.setId(1L);
        dataVO.setSessionId(100L);
        dataVO.setTotalViewers(5000);

        when(douyinLiveDataSyncService.syncSessionDataFromDouyin(eq(100L), eq("room123"), eq("token123")))
                .thenReturn(dataVO);

        mockMvc.perform(post("/api/v1/live/data/session/sync-from-douyin")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(100));
    }

    @Test
    @DisplayName("从抖音 API 同步（缺少参数）- 应返回 1001")
    void syncFromDouyin_withoutParams_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        mockMvc.perform(post("/api/v1/live/data/session/sync-from-douyin")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("获取场次商品数据列表 - 应返回 200")
    void getProductData_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("sessionId", 100L);

        LiveProductDataVO productVO = new LiveProductDataVO();
        productVO.setId(1L);
        productVO.setSessionId(100L);
        productVO.setProductId(200L);
        productVO.setSaleQuantity(100);
        productVO.setRevenue(new BigDecimal("10000.00"));

        when(liveDataSyncService.getProductDataBySession(eq(100L)))
                .thenReturn(List.of(productVO));

        mockMvc.perform(post("/api/v1/live/data/product")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].sessionId").value(100));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("sessionId", 100L);

        mockMvc.perform(post("/api/v1/live/data/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
