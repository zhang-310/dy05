package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolvePendingDeepenRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveReportRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTopicRepository;
import cn.gaifan.douyinOperations.module.ai.service.DeepEvolveService;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveEngineService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveRoiService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTopicImportService;
import cn.gaifan.douyinOperations.module.ai.vo.VideoCompareRequestVO;
import cn.gaifan.douyinOperations.module.ai.vo.VideoCompareResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
 * EvolutionController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("EvolutionController 集成测试")
class EvolutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EvolutionService evolutionService;

    @MockBean
    private EvolveEngineService evolveEngineService;

    @MockBean
    private EvolveRoiService evolveRoiService;

    @MockBean
    private EvolveTopicImportService evolveTopicImportService;

    @MockBean
    private DeepEvolveService deepEvolveService;

    @MockBean
    private AiEvolvePendingDeepenRepository pendingDeepenRepository;

    @MockBean
    private AiEvolveTopicRepository evolveTopicRepository;

    @MockBean
    private AiEvolveReportRepository evolveReportRepository;

    @MockBean
    private AiEvolveTaskRepository aiEvolveTaskRepository;

    // ─── 爆款拆解测试 ────────────────────────────────────────────────

    @Test
    @DisplayName("爆款拆解列表 - 应返回 200")
    void viralList_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 20);

        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", 1L);
        item1.put("videoId", 100L);
        item1.put("status", 2);

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>(1L, List.of(item1), 0, 20);
        when(evolutionService.searchViralAnalysis(eq(1L), isNull(), eq(0), eq(20))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/evolution/viral/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("爆款拆解详情 - 应返回 200")
    void viralGet_shouldReturn200() throws Exception {
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", 1L);
        detail.put("reportContent", "分析报告内容");

        when(evolutionService.getViralAnalysis(eq(1L), eq(1L))).thenReturn(detail);

        mockMvc.perform(post("/api/v1/ai/evolution/viral/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("触发爆款拆解 - 应返回 200")
    void viralTrigger_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("videoId", 100L);
        body.put("accountId", 10L);

        when(evolutionService.triggerViralAnalysis(eq(100L), eq(1L), eq(10L))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/ai/evolution/viral/trigger")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("触发爆款拆解（缺少 videoId）- 应返回 1001")
    void viralTrigger_missingVideoId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/evolution/viral/trigger")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("videoId 不能为空"));
    }

    @Test
    @DisplayName("完成爆款拆解 - 应返回 204")
    void viralComplete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("reportContent", "分析报告");
        body.put("successFactors", "成功因素");
        body.put("replicableMethods", "可复制方法");
        body.put("qualityScore", 85);
        body.put("tokensUsed", 1000L);
        body.put("modelUsed", "gpt-4");

        doNothing().when(evolutionService).completeViralAnalysis(
                eq(1L), eq("分析报告"), eq("成功因素"), eq("可复制方法"), eq(85), eq(1000L), eq("gpt-4"),
                eq(1L), eq(false));

        mockMvc.perform(post("/api/v1/ai/evolution/viral/complete")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除爆款拆解 - 应返回 204")
    void viralDelete_shouldReturn204() throws Exception {
        doNothing().when(evolutionService).deleteViralAnalysis(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/ai/evolution/viral/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    // ─── 进化任务队列测试 ────────────────────────────────────────────────

    @Test
    @DisplayName("进化任务列表 - 应返回 200")
    void taskList_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 20);

        AiEvolveTask task1 = new AiEvolveTask();
        task1.setId(1L);
        task1.setTaskNo("TASK001");
        task1.setEvolveAngle("viral_analysis");
        task1.setStatus("completed");
        task1.setScoreTotal(85);
        task1.setCreateTime(new Timestamp(System.currentTimeMillis()));

        when(aiEvolveTaskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable p = inv.getArgument(1);
                    return new PageImpl<>(List.of(task1), p, 1);
                });

        mockMvc.perform(post("/api/v1/ai/evolution/task/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("触发进化任务 - 应返回 200")
    void taskTrigger_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskType", "viral_analysis");
        body.put("targetKbId", 10L);

        doNothing().when(evolveEngineService).runEvolution(eq(10L), eq("viral_analysis"), anyString());

        mockMvc.perform(post("/api/v1/ai/evolution/task/trigger")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.message").value("进化任务已触发"));
    }

    @Test
    @DisplayName("触发进化任务（缺少 taskType）- 应返回 1001")
    void taskTrigger_missingTaskType_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/evolution/task/trigger")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("taskType 不能为空"));
    }

    @Test
    @DisplayName("取消进化任务 - 应返回 204")
    void taskCancel_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);
        doNothing().when(evolveEngineService).cancelTask(eq(1L));

        mockMvc.perform(post("/api/v1/ai/evolution/task/cancel")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    // ─── 直播复盘测试 ────────────────────────────────────────────────

    @Test
    @DisplayName("直播复盘列表 - 应返回 200")
    void liveReviewList_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 20);

        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", 1L);
        item1.put("sessionId", 100L);

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>(1L, List.of(item1), 0, 20);
        when(evolutionService.searchLiveReviews(eq(1L), isNull(), eq(0), eq(20))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/evolution/live-review/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("直播复盘详情 - 应返回 200")
    void liveReviewGet_shouldReturn200() throws Exception {
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", 1L);
        detail.put("reportContent", "复盘报告");

        when(evolutionService.getLiveReview(eq(1L))).thenReturn(detail);

        mockMvc.perform(post("/api/v1/ai/evolution/live-review/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("触发直播复盘 - 应返回 200")
    void liveReviewTrigger_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);
        body.put("accountId", 10L);

        when(evolutionService.triggerLiveReview(eq(100L), eq(1L), eq(10L))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/ai/evolution/live-review/trigger")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("触发直播复盘（缺少 sessionId）- 应返回 1001")
    void liveReviewTrigger_missingSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/evolution/live-review/trigger")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("sessionId 不能为空"));
    }

    @Test
    @DisplayName("完成直播复盘 - 应返回 204")
    void liveReviewComplete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("reportContent", "复盘报告");
        body.put("topScripts", "优秀话术");
        body.put("weakPoints", "薄弱环节");
        body.put("tokensUsed", 1000L);
        body.put("modelUsed", "gpt-4");

        doNothing().when(evolutionService).completeLiveReview(
                eq(1L), eq("复盘报告"), eq("优秀话术"), eq("薄弱环节"), eq(1000L), eq("gpt-4"));

        mockMvc.perform(post("/api/v1/ai/evolution/live-review/complete")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除直播复盘 - 应返回 204")
    void liveReviewDelete_shouldReturn204() throws Exception {
        doNothing().when(evolutionService).deleteLiveReview(eq(1L));

        mockMvc.perform(post("/api/v1/ai/evolution/live-review/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    // ─── 视频对比分析测试 ────────────────────────────────────────────────

    @Test
    @DisplayName("视频对比分析 - 应返回 200")
    void videoCompare_shouldReturn200() throws Exception {
        VideoCompareRequestVO vo = new VideoCompareRequestVO();
        vo.setVideoId(100L);
        vo.setCompareType("similar");
        vo.setCompareVideoIds(List.of(101L, 102L));

        VideoCompareResultVO result = new VideoCompareResultVO();
        result.setGenerationTime(1500L);
        result.setTokenUsage(500);

        when(evolutionService.compareVideos(any(VideoCompareRequestVO.class), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/evolution/video/compare")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.generationTime").value(1500));
    }

    // ─── 统计测试 ────────────────────────────────────────────────────

    @Test
    @DisplayName("进化引擎统计 - 应返回 200")
    void stats_shouldReturn200() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("viralAnalysisTotal", 10L);
        stats.put("viralAnalysisDone", 7L);
        stats.put("liveReviewTotal", 3L);
        stats.put("liveReviewDone", 2L);
        stats.put("indexQueuePending", 1L);

        when(evolutionService.getEvolutionStats(eq(1L))).thenReturn(stats);

        mockMvc.perform(post("/api/v1/ai/evolution/stats")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.viralAnalysisTotal").value(10))
                .andExpect(jsonPath("$.data.viralAnalysisDone").value(7));
    }

    @Test
    @DisplayName("知识进化 ROI 指标 - 应返回 200")
    void roi_shouldReturn200() throws Exception {
        AiEvolveTask task1 = new AiEvolveTask();
        task1.setId(1L);
        task1.setStatus("completed");
        task1.setScoreTotal(85);
        task1.setKbId(10L);

        when(aiEvolveTaskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable p = inv.getArgument(1);
                    return new PageImpl<>(List.of(task1), p, 1);
                });

        mockMvc.perform(post("/api/v1/ai/evolution/roi")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalTasks").value(1));
    }

    @Test
    @DisplayName("知识质量分趋势 - 应返回 200")
    void scoreTrend_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("days", 7);

        AiEvolveTask task1 = new AiEvolveTask();
        task1.setId(1L);
        task1.setScoreTotal(85);
        task1.setCreateTime(new Timestamp(System.currentTimeMillis()));

        when(evolveEngineService.listRecentTasks(eq(200))).thenReturn(List.of(task1));

        mockMvc.perform(post("/api/v1/ai/evolution/score-trend")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("门面 status - 应返回 200")
    void evolveStatus_shouldReturn200() throws Exception {
        when(evolveEngineService.listRecentTasks(30)).thenReturn(List.of());
        when(evolveEngineService.listTopics(any(), any(), anyBoolean())).thenReturn(List.of());
        when(evolveEngineService.getScoreTrend(7)).thenReturn(List.of());
        when(evolveRoiService.getRoiMetrics(10)).thenReturn(Map.of());

        mockMvc.perform(post("/api/v1/ai/evolution/status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.running").value(false));
    }

    @Test
    @DisplayName("门面 topic/list - 应返回 200")
    void topicList_shouldReturn200() throws Exception {
        when(evolveEngineService.listTopics(any(), any(), anyBoolean())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ai/evolution/topic/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("quality-score/history - 应返回 200")
    void qualityScoreHistory_shouldReturn200() throws Exception {
        AiEvolveTask task1 = new AiEvolveTask();
        task1.setId(1L);
        task1.setScoreTotal(85);
        task1.setCreateTime(new Timestamp(System.currentTimeMillis()));
        when(evolveEngineService.listRecentTasks(200)).thenReturn(List.of(task1));

        Map<String, Object> body = new HashMap<>();
        body.put("days", 7);

        mockMvc.perform(post("/api/v1/ai/evolution/quality-score/history")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ─── 未认证测试 ────────────────────────────────────────────────────

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/ai/evolution/viral/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
