package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.service.LiveRealtimePanelService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveRealtimePanelController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveRealtimePanelController 集成测试")
class LiveRealtimePanelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveRealtimePanelService realtimePanelService;

    @Test
    @DisplayName("初始化面板 - 应返回 200")
    void initializePanel_shouldReturn200() throws Exception {
        SlotOperationVO body = new SlotOperationVO();
        body.setLiveSessionId(100L);

        PanelInitVO initVO = new PanelInitVO();
        initVO.setLiveSessionId(100L);
        initVO.setCurrentSlotIndex(0);
        initVO.setSlots(List.of());

        when(realtimePanelService.initializePanel(eq(100L), eq(1L))).thenReturn(initVO);

        mockMvc.perform(post("/api/v1/live/realtime-panel/init")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.liveSessionId").value(100))
                .andExpect(jsonPath("$.data.currentSlotIndex").value(0));
    }

    @Test
    @DisplayName("SSE 流连接 - 应返回 200")
    void streamRealtimeData_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/live/realtime-panel/stream/100")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("下一话术 - 应返回 200")
    void nextSlot_shouldReturn200() throws Exception {
        SlotOperationVO body = new SlotOperationVO();
        body.setLiveSessionId(100L);

        LiveSessionScriptSlotVO slotVO = new LiveSessionScriptSlotVO();
        slotVO.setSlotIndex(1);
        slotVO.setContent("下一段话术内容");
        slotVO.setDurationSeconds(60);
        slotVO.setStartedAt(LocalDateTime.now());

        when(realtimePanelService.nextSlot(eq(100L), eq(1L))).thenReturn(slotVO);

        mockMvc.perform(post("/api/v1/live/realtime-panel/next-slot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.slotIndex").value(1))
                .andExpect(jsonPath("$.data.content").value("下一段话术内容"));
    }

    @Test
    @DisplayName("上一话术 - 应返回 200")
    void prevSlot_shouldReturn200() throws Exception {
        SlotOperationVO body = new SlotOperationVO();
        body.setLiveSessionId(100L);

        LiveSessionScriptSlotVO slotVO = new LiveSessionScriptSlotVO();
        slotVO.setSlotIndex(0);
        slotVO.setContent("上一段话术内容");
        slotVO.setDurationSeconds(60);

        when(realtimePanelService.prevSlot(eq(100L), eq(1L))).thenReturn(slotVO);

        mockMvc.perform(post("/api/v1/live/realtime-panel/prev-slot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.slotIndex").value(0));
    }

    @Test
    @DisplayName("跳转话术 - 应返回 200")
    void jumpSlot_shouldReturn200() throws Exception {
        SlotOperationVO body = new SlotOperationVO();
        body.setLiveSessionId(100L);
        body.setSlotIndex(3);

        LiveSessionScriptSlotVO slotVO = new LiveSessionScriptSlotVO();
        slotVO.setSlotIndex(3);
        slotVO.setContent("第3段话术内容");

        when(realtimePanelService.jumpSlot(eq(100L), eq(3), eq(1L))).thenReturn(slotVO);

        mockMvc.perform(post("/api/v1/live/realtime-panel/jump-slot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.slotIndex").value(3));
    }

    @Test
    @DisplayName("跳转话术（缺少 slotIndex）- 应返回 1001")
    void jumpSlot_withoutSlotIndex_shouldReturn1001() throws Exception {
        SlotOperationVO body = new SlotOperationVO();
        body.setLiveSessionId(100L);

        mockMvc.perform(post("/api/v1/live/realtime-panel/jump-slot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("标记完成 - 应返回 200")
    void completeSlot_shouldReturn200() throws Exception {
        SlotOperationVO body = new SlotOperationVO();
        body.setLiveSessionId(100L);
        body.setSlotIndex(2);

        LiveSessionScriptSlotVO slotVO = new LiveSessionScriptSlotVO();
        slotVO.setSlotIndex(2);
        slotVO.setContent("已完成的话术");

        when(realtimePanelService.completeSlot(eq(100L), eq(2), eq(1L))).thenReturn(slotVO);

        mockMvc.perform(post("/api/v1/live/realtime-panel/complete-slot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.slotIndex").value(2));
    }

    @Test
    @DisplayName("标记完成（缺少 slotIndex）- 应返回 1001")
    void completeSlot_withoutSlotIndex_shouldReturn1001() throws Exception {
        SlotOperationVO body = new SlotOperationVO();
        body.setLiveSessionId(100L);

        mockMvc.perform(post("/api/v1/live/realtime-panel/complete-slot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("更新实时数据 - 应返回 200")
    void updateRealtimeData_shouldReturn200() throws Exception {
        RealtimeDataSaveVO body = new RealtimeDataSaveVO();
        body.setLiveSessionId(100L);
        body.setLikeCount(1000);
        body.setCommentCount(50);
        body.setViewerCount(500);
        body.setWatchedCount(2000);

        LiveSessionRealtimeDataVO dataVO = new LiveSessionRealtimeDataVO();
        dataVO.setLiveSessionId(100L);
        dataVO.setLikeCount(1000);
        dataVO.setCommentCount(50);
        dataVO.setViewerCount(500);
        dataVO.setWatchedCount(2000);

        when(realtimePanelService.updateRealtimeData(any(RealtimeDataSaveVO.class), eq(1L))).thenReturn(dataVO);

        mockMvc.perform(post("/api/v1/live/realtime-panel/update-data")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.likeCount").value(1000))
                .andExpect(jsonPath("$.data.viewerCount").value(500));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        SlotOperationVO body = new SlotOperationVO();
        body.setLiveSessionId(100L);

        mockMvc.perform(post("/api/v1/live/realtime-panel/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
