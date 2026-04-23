package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.live.service.LiveAnalysisService;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAnalysisVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveReviewVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionVO;
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
 * LiveAnalysisController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveAnalysisController 集成测试")
class LiveAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveAnalysisService liveAnalysisService;

    @MockBean
    private LiveSessionService liveSessionService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @MockBean
    private AiQuotaService aiQuotaService;

    @MockBean
    private AiCallLogService aiCallLogService;

    @BeforeEach
    void setUp() {
        when(dataScopeService.getVisibleUserIds(anyLong(), anyString())).thenReturn(List.of(1L));
    }

    @Test
    @DisplayName("生成 AI 复盘报告 - 应返回 200")
    void generate_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        LiveAnalysisVO analysisVO = new LiveAnalysisVO();
        analysisVO.setRating("A");
        analysisVO.setSummary("本场直播表现优秀");
        analysisVO.setHighlights(List.of("开场吸引力强", "商品讲解清晰"));
        analysisVO.setIssues(List.of("中场节奏略慢"));
        analysisVO.setSuggestions(List.of("增加互动环节", "优化商品排序"));

        LiveAnalysisVO.ScriptEffectiveness effectiveness = new LiveAnalysisVO.ScriptEffectiveness();
        effectiveness.setScriptId(1L);
        effectiveness.setScriptType("opening");
        effectiveness.setViewerDelta(50);
        effectiveness.setScore(8.5);
        analysisVO.setScriptEffectiveness(List.of(effectiveness));

        doNothing().when(aiQuotaService).ensureQuota(1L);
        when(liveAnalysisService.generate(100L)).thenReturn(analysisVO);
        doNothing().when(aiQuotaService).consume(1L);
        when(aiCallLogService.log(any())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/analysis/generate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.rating").value("A"))
                .andExpect(jsonPath("$.data.summary").value("本场直播表现优秀"))
                .andExpect(jsonPath("$.data.highlights[0]").value("开场吸引力强"))
                .andExpect(jsonPath("$.data.scriptEffectiveness[0].scriptId").value(1));
    }

    @Test
    @DisplayName("获取 AI 分析报告 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        LiveSessionVO sessionVO = new LiveSessionVO();
        sessionVO.setId(100L);
        sessionVO.setUserId(1L);

        LiveAnalysisVO analysisVO = new LiveAnalysisVO();
        analysisVO.setRating("B");
        analysisVO.setSummary("本场直播表现良好");

        when(liveSessionService.getByIdWithScope(eq(100L), anyList())).thenReturn(sessionVO);
        when(liveAnalysisService.get(100L)).thenReturn(analysisVO);

        mockMvc.perform(post("/api/v1/live/analysis/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.rating").value("B"))
                .andExpect(jsonPath("$.data.summary").value("本场直播表现良好"));
    }

    @Test
    @DisplayName("获取 AI 复盘报告 - 应返回 200")
    void getReview_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        LiveSessionVO sessionVO = new LiveSessionVO();
        sessionVO.setId(100L);
        sessionVO.setUserId(1L);

        LiveReviewVO reviewVO = new LiveReviewVO();
        reviewVO.setId(1L);
        reviewVO.setSessionId(100L);
        reviewVO.setTotalViewers(5000L);
        reviewVO.setTotalGmv(new BigDecimal("50000.00"));
        reviewVO.setConversionRate(new BigDecimal("0.05"));
        reviewVO.setPeakViewers(1200L);
        reviewVO.setReportContent("详细复盘报告内容");
        reviewVO.setTopScripts("开场白、商品介绍");
        reviewVO.setWeakPoints("中场互动不足");
        reviewVO.setStatus(1);
        reviewVO.setCreateTime(new Timestamp(System.currentTimeMillis()));

        when(liveSessionService.getByIdWithScope(eq(100L), anyList())).thenReturn(sessionVO);
        when(liveAnalysisService.getReview(100L)).thenReturn(reviewVO);

        mockMvc.perform(post("/api/v1/live/analysis/review")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(100))
                .andExpect(jsonPath("$.data.totalViewers").value(5000))
                .andExpect(jsonPath("$.data.totalGmv").value(50000.00))
                .andExpect(jsonPath("$.data.topScripts").value("开场白、商品介绍"));
    }

    @Test
    @DisplayName("生成报告缺少 sessionId - 应返回 1001")
    void generate_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/analysis/generate")
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

        mockMvc.perform(post("/api/v1/live/analysis/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
