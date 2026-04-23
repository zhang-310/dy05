package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveRhythmOptimizer;
import cn.gaifan.douyinOperations.module.live.service.ProductStrategyRecommender;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
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

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveRhythmController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveRhythmController 集成测试")
class LiveRhythmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveRhythmOptimizer liveRhythmOptimizer;

    @MockBean
    private ProductStrategyRecommender productStrategyRecommender;

    @MockBean
    private LiveScriptRepository liveScriptRepository;

    @Test
    @DisplayName("优化直播节奏 - 应返回 200")
    void optimize_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        Map<String, Object> optimizedSchedule = new HashMap<>();
        optimizedSchedule.put("totalDuration", 3600);
        optimizedSchedule.put("slots", List.of(
                Map.of("scriptId", 1L, "sequenceNo", 1, "duration", 300),
                Map.of("scriptId", 2L, "sequenceNo", 2, "duration", 600)
        ));

        when(liveRhythmOptimizer.optimizeSchedule(eq(1L), eq(100L)))
                .thenReturn(optimizedSchedule);

        mockMvc.perform(post("/api/v1/live/rhythm/optimize")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalDuration").value(3600));
    }

    @Test
    @DisplayName("商品讲解策略推荐 - 应返回 200")
    void productStrategy_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 50L);

        Map<String, Object> strategy = new HashMap<>();
        strategy.put("recommendedDuration", 180);
        strategy.put("keyPoints", List.of("功效", "成分", "使用方法"));
        strategy.put("style", "professional");

        when(productStrategyRecommender.recommend(eq(1L), eq(50L)))
                .thenReturn(strategy);

        mockMvc.perform(post("/api/v1/live/rhythm/product-strategy")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.recommendedDuration").value(180))
                .andExpect(jsonPath("$.data.style").value("professional"));
    }

    @Test
    @DisplayName("排品顺序推荐 - 应返回 200")
    void batchOrder_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        Map<String, Object> batchOrder = new HashMap<>();
        batchOrder.put("recommendedOrder", List.of(50L, 51L, 52L));
        batchOrder.put("reasoning", "按照历史转化率排序");

        when(productStrategyRecommender.recommendBatchOrder(eq(1L), eq(100L)))
                .thenReturn(batchOrder);

        mockMvc.perform(post("/api/v1/live/rhythm/batch-order")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.reasoning").value("按照历史转化率排序"));
    }

    @Test
    @DisplayName("保存节奏方案 - 应返回 200")
    void saveRhythm_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        List<Map<String, Object>> slots = new ArrayList<>();
        Map<String, Object> slot1 = new HashMap<>();
        slot1.put("scriptId", 1L);
        slot1.put("sequenceNo", 1);
        slot1.put("durationLimitSec", 300);
        slots.add(slot1);

        Map<String, Object> slot2 = new HashMap<>();
        slot2.put("scriptId", 2L);
        slot2.put("sequenceNo", 2);
        slot2.put("durationLimitSec", 600);
        slots.add(slot2);

        body.put("slots", slots);

        LiveScript script1 = new LiveScript();
        script1.setId(1L);
        script1.setSessionId(100L);

        LiveScript script2 = new LiveScript();
        script2.setId(2L);
        script2.setSessionId(100L);

        when(liveScriptRepository.findById(1L)).thenReturn(Optional.of(script1));
        when(liveScriptRepository.findById(2L)).thenReturn(Optional.of(script2));
        when(liveScriptRepository.save(any(LiveScript.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/v1/live/rhythm/save-rhythm")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(100))
                .andExpect(jsonPath("$.data.updatedSlots").value(2))
                .andExpect(jsonPath("$.data.status").value("ok"));
    }

    @Test
    @DisplayName("保存节奏方案（空 slots）- 应返回 1001")
    void saveRhythm_withEmptySlots_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("slots", List.of());

        mockMvc.perform(post("/api/v1/live/rhythm/save-rhythm")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("slots 不能为空"));
    }

    @Test
    @DisplayName("优化直播节奏缺少 sessionId - 应返回 1001")
    void optimize_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/rhythm/optimize")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("sessionId 不能为空"));
    }

    @Test
    @DisplayName("商品策略推荐缺少 productId - 应返回 1001")
    void productStrategy_withoutProductId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/rhythm/product-strategy")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("productId 不能为空"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        mockMvc.perform(post("/api/v1/live/rhythm/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
