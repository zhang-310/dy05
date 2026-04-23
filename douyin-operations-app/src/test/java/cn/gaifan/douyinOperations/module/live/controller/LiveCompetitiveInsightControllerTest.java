package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.service.LiveCompetitiveInsightService;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightVO;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveCompetitiveInsightController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveCompetitiveInsightController 集成测试")
class LiveCompetitiveInsightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveCompetitiveInsightService insightService;

    @Test
    @DisplayName("分页查询竞品洞察 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        LiveCompetitiveInsightSearchVO searchVO = new LiveCompetitiveInsightSearchVO();

        LiveCompetitiveInsightVO insightVO = new LiveCompetitiveInsightVO();
        insightVO.setId(1L);
        insightVO.setOwnerId(1L);
        insightVO.setSessionId(100L);
        insightVO.setCompetitorLabel("竞品A");
        insightVO.setProductPrice(new BigDecimal("299.00"));
        insightVO.setMarketSharePercent(new BigDecimal("15.5"));
        insightVO.setGmvEstimate(new BigDecimal("1000000.00"));
        insightVO.setWinLossNotes("价格优势明显");
        insightVO.setCreateTime(new Timestamp(System.currentTimeMillis()));

        PageResultVO<LiveCompetitiveInsightVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(insightVO));

        when(insightService.search(any(LiveCompetitiveInsightSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/competitive-insight/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].competitorLabel").value("竞品A"));
    }

    @Test
    @DisplayName("分页查询竞品洞察（空 body）- 应返回 200")
    void search_withEmptyBody_shouldReturn200() throws Exception {
        PageResultVO<LiveCompetitiveInsightVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(insightService.search(any(LiveCompetitiveInsightSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/competitive-insight/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("获取竞品洞察详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        LiveCompetitiveInsightVO insightVO = new LiveCompetitiveInsightVO();
        insightVO.setId(1L);
        insightVO.setOwnerId(1L);
        insightVO.setSessionId(100L);
        insightVO.setCompetitorLabel("竞品A");
        insightVO.setProductPrice(new BigDecimal("299.00"));
        insightVO.setMarketSharePercent(new BigDecimal("15.5"));

        when(insightService.getById(eq(1L), eq(1L))).thenReturn(insightVO);

        mockMvc.perform(post("/api/v1/live/competitive-insight/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.competitorLabel").value("竞品A"));
    }

    @Test
    @DisplayName("获取竞品洞察详情（缺少 id）- 应返回 1001")
    void get_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/competitive-insight/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 id"));
    }

    @Test
    @DisplayName("保存竞品洞察 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        LiveCompetitiveInsightSaveVO saveVO = new LiveCompetitiveInsightSaveVO();
        saveVO.setSessionId(100L);
        saveVO.setCompetitorLabel("竞品B");
        saveVO.setProductPrice(new BigDecimal("399.00"));
        saveVO.setMarketSharePercent(new BigDecimal("20.0"));
        saveVO.setGmvEstimate(new BigDecimal("2000000.00"));
        saveVO.setWinLossNotes("品牌知名度高");

        when(insightService.save(any(LiveCompetitiveInsightSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/competitive-insight/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除竞品洞察 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(insightService).delete(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/live/competitive-insight/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除竞品洞察（缺少 id）- 应返回 1001")
    void delete_withoutId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/competitive-insight/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("缺少 id"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/live/competitive-insight/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
