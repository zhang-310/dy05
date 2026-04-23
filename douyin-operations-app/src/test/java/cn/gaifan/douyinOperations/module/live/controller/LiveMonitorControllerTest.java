package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveMonitorService;
import cn.gaifan.douyinOperations.module.live.vo.LiveMonitorSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveMonitorVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveMonitorController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveMonitorController 集成测试")
class LiveMonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveMonitorService liveMonitorService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @MockBean
    private LiveSessionRepository liveSessionRepository;

    @BeforeEach
    void setUp() {
        when(dataScopeService.getVisibleUserIds(anyLong(), anyString())).thenReturn(List.of(1L));
        when(liveSessionRepository.findIdsByUserIdIn(anyList())).thenReturn(List.of(100L, 101L));
    }

    @Test
    @DisplayName("查询监控数据 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        LiveMonitorSearchVO searchVO = new LiveMonitorSearchVO();
        searchVO.setSessionId(100L);

        LiveMonitorVO monitorVO = new LiveMonitorVO();
        monitorVO.setId(1L);
        monitorVO.setSessionId(100L);
        monitorVO.setTimestamp(new Timestamp(System.currentTimeMillis()));
        monitorVO.setViewers(500);
        monitorVO.setLikes(1000L);
        monitorVO.setComments(50);

        PageResultVO<LiveMonitorVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(monitorVO));

        when(liveMonitorService.search(any(LiveMonitorSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/monitor/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].sessionId").value(100))
                .andExpect(jsonPath("$.data.list[0].viewers").value(500));
    }

    @Test
    @DisplayName("查询监控数据（空 body）- 应返回 200")
    void search_withEmptyBody_shouldReturn200() throws Exception {
        PageResultVO<LiveMonitorVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(liveMonitorService.search(any(LiveMonitorSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/monitor/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("查询监控数据（DataScope 过滤）- 应注入 sessionIds")
    void search_withDataScope_shouldInjectSessionIds() throws Exception {
        PageResultVO<LiveMonitorVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(liveMonitorService.search(any(LiveMonitorSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/monitor/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("保存监控数据 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        LiveMonitorVO monitorVO = new LiveMonitorVO();
        monitorVO.setSessionId(100L);
        monitorVO.setTimestamp(new Timestamp(System.currentTimeMillis()));
        monitorVO.setViewers(500);
        monitorVO.setLikes(1000L);

        when(liveMonitorService.save(any(LiveMonitorVO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/monitor/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(monitorVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("按场次获取监控数据 - 应返回 200")
    void getBySessionId_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        LiveMonitorVO monitorVO1 = new LiveMonitorVO();
        monitorVO1.setId(1L);
        monitorVO1.setSessionId(100L);
        monitorVO1.setViewers(500);

        LiveMonitorVO monitorVO2 = new LiveMonitorVO();
        monitorVO2.setId(2L);
        monitorVO2.setSessionId(100L);
        monitorVO2.setViewers(600);

        when(liveMonitorService.getBySessionId(eq(100L))).thenReturn(List.of(monitorVO1, monitorVO2));

        mockMvc.perform(post("/api/v1/live/monitor/by-session")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].sessionId").value(100))
                .andExpect(jsonPath("$.data[0].viewers").value(500))
                .andExpect(jsonPath("$.data[1].viewers").value(600));
    }

    @Test
    @DisplayName("按场次获取监控数据（缺少 sessionId）- 应返回 1001")
    void getBySessionId_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/monitor/by-session")
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
        mockMvc.perform(post("/api/v1/live/monitor/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
