package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationTask;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.service.LiveFullGenerationProgressTracker;
import cn.gaifan.douyinOperations.module.live.service.LiveGenerationTaskService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveScriptGenerationController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptGenerationController 集成测试")
class LiveScriptGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveAiService liveAiService;

    @MockBean
    private LiveGenerationTaskService generationTaskService;

    @MockBean
    private AiQuotaService aiQuotaService;

    @MockBean
    private AiCallLogService aiCallLogService;

    @MockBean
    private LiveProductRepository liveProductRepository;

    @MockBean
    private LiveFullGenerationProgressTracker fullGenerationProgressTracker;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("生成开场话术（已废弃）- 应返回 200")
    void generateOpening_shouldReturn200() throws Exception {
        LiveAiGenerateVO vo = new LiveAiGenerateVO();
        vo.setSessionId(100L);
        vo.setStyle("enthusiastic");
        vo.setModelId(1L);

        LiveAiResultVO result = new LiveAiResultVO();
        result.setContent("欢迎来到直播间，今天给大家带来超值好物");

        doNothing().when(aiQuotaService).ensureQuota(eq(1L));
        when(liveAiService.generateOpening(any(LiveAiGenerateVO.class)))
                .thenReturn(result);
        doNothing().when(aiQuotaService).consume(eq(1L));
        when(aiCallLogService.log(any(AiCallLogService.LogEntry.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/ai/generate-opening")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").value("欢迎来到直播间，今天给大家带来超值好物"));
    }

    @Test
    @DisplayName("按槽位需求生成单段话术 - 应返回 200")
    void generateForSlot_shouldReturn200() throws Exception {
        SlotGenerateVO vo = new SlotGenerateVO();
        vo.setScriptId(1L);
        vo.setRequirement("介绍产品成分");
        vo.setDurationLimitSec(60);
        vo.setModelId(1L);

        doNothing().when(aiQuotaService).ensureQuota(eq(1L));
        when(liveAiService.generateForSlot(eq(1L), eq("介绍产品成分"), eq(60), eq(1L)))
                .thenReturn("这款产品含有玻尿酸、烟酰胺等核心成分");
        doNothing().when(aiQuotaService).consume(eq(1L));
        when(aiCallLogService.log(any(AiCallLogService.LogEntry.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/ai/generate-slot")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("这款产品含有玻尿酸、烟酰胺等核心成分"));
    }

    @Test
    @DisplayName("生成完整话术流程 - 应返回 200")
    void generateFull_shouldReturn200() throws Exception {
        LiveAiGenerateVO vo = new LiveAiGenerateVO();
        vo.setSessionId(100L);
        vo.setStyle("professional");
        vo.setModelId(1L);

        LiveAiResultVO result1 = new LiveAiResultVO();
        result1.setContent("开场话术");

        LiveAiResultVO result2 = new LiveAiResultVO();
        result2.setContent("产品介绍");

        LiveAiFullResultVO fullResult = new LiveAiFullResultVO();
        fullResult.setResults(List.of(result1, result2));
        fullResult.setConsumption(2);

        doNothing().when(aiQuotaService).ensureQuota(eq(1L));
        when(liveAiService.generateFull(any(LiveAiGenerateVO.class)))
                .thenReturn(fullResult);
        doNothing().when(aiQuotaService).consume(eq(1L), eq(2));
        when(aiCallLogService.log(any(AiCallLogService.LogEntry.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/ai/generate-full")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].content").value("开场话术"))
                .andExpect(jsonPath("$.data[1].content").value("产品介绍"));
    }

    @Test
    @DisplayName("查询场次是否正在执行一键生成 - 应返回 200")
    void generateFullInProgress_shouldReturn200() throws Exception {
        SessionIdVO vo = new SessionIdVO();
        vo.setSessionId(100L);

        when(fullGenerationProgressTracker.isMarked(eq(100L)))
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/live/ai/generate-full-in-progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.inProgress").value(true));
    }

    @Test
    @DisplayName("查询场次是否正在执行一键生成（无 sessionId）- 应返回 200")
    void generateFullInProgress_withoutSessionId_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/live/ai/generate-full-in-progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.inProgress").value(false));
    }

    @Test
    @DisplayName("查询场次当前活跃的生成任务 - 应返回 200")
    void getActiveGenerationTask_shouldReturn200() throws Exception {
        SessionIdVO vo = new SessionIdVO();
        vo.setSessionId(100L);

        LiveGenerationTask task = new LiveGenerationTask();
        task.setId(1L);
        task.setSessionId(100L);
        task.setStatus("running");
        task.setTotalSlots(10);
        task.setCompletedSlots(5);

        when(generationTaskService.getActiveTask(eq(100L)))
                .thenReturn(Optional.of(task));

        mockMvc.perform(post("/api/v1/live/ai/generation-task/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.sessionId").value(100))
                .andExpect(jsonPath("$.data.status").value("running"));
    }

    @Test
    @DisplayName("生成骨架 - 应返回 200")
    void generateSkeleton_shouldReturn200() throws Exception {
        SkeletonGenerateVO vo = new SkeletonGenerateVO();
        vo.setSessionId(100L);
        vo.setModelId(1L);

        SkeletonSlotVO slot1 = new SkeletonSlotVO();
        slot1.setScriptType("opening");
        slot1.setSuggestedDurationSec(30);

        SkeletonSlotVO slot2 = new SkeletonSlotVO();
        slot2.setScriptType("product");
        slot2.setSuggestedDurationSec(60);

        doNothing().when(aiQuotaService).ensureQuota(eq(1L));
        when(liveAiService.generateSkeleton(eq(100L), eq(1L), eq(1L)))
                .thenReturn(List.of(slot1, slot2));
        doNothing().when(aiQuotaService).consume(eq(1L));
        when(aiCallLogService.log(any(AiCallLogService.LogEntry.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/ai/generate-skeleton")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].scriptType").value("opening"))
                .andExpect(jsonPath("$.data[1].scriptType").value("product"));
    }

    @Test
    @DisplayName("生成产品 AI 话术 - 应返回 200")
    void generateProductScript_shouldReturn200() throws Exception {
        ProductScriptGenerateVO vo = new ProductScriptGenerateVO();
        vo.setProductId(200L);
        vo.setScriptType("promotion");
        vo.setStyle("professional");

        ProductScriptResultVO result = new ProductScriptResultVO();
        result.setScriptContent("这款产品非常适合干性肌肤使用");

        doNothing().when(aiQuotaService).ensureQuota(eq(1L));
        when(liveAiService.generateProductScript(any(ProductScriptGenerateVO.class), eq(1L)))
                .thenReturn(result);
        doNothing().when(aiQuotaService).consume(eq(1L));
        when(aiCallLogService.log(any(AiCallLogService.LogEntry.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/ai/generate-product-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptContent").value("这款产品非常适合干性肌肤使用"));
    }

    @Test
    @DisplayName("AI 商品排序建议 - 应返回 200")
    void sortSuggest_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        LiveProduct product1 = new LiveProduct();
        product1.setId(1L);
        product1.setProductType("hot");

        LiveProduct product2 = new LiveProduct();
        product2.setId(2L);
        product2.setProductType("profit");

        when(liveProductRepository.findBySessionId(eq(100L)))
                .thenReturn(java.util.Arrays.asList(product1, product2));

        mockMvc.perform(post("/api/v1/live/ai/sort-suggest")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.productIds[0]").value(1))
                .andExpect(jsonPath("$.data.productIds[1]").value(2))
                .andExpect(jsonPath("$.data.reason").exists());
    }

    @Test
    @DisplayName("AI 商品排序建议（缺少 sessionId）- 应返回 1001")
    void sortSuggest_withoutSessionId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/live/ai/sort-suggest")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("sessionId 不能为空"));
    }

    @Test
    @DisplayName("AI 商品排序建议（场次无商品）- 应返回 1005")
    void sortSuggest_noProducts_shouldReturn1005() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 100L);

        when(liveProductRepository.findBySessionId(eq(100L)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/v1/live/ai/sort-suggest")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1005));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        LiveAiGenerateVO vo = new LiveAiGenerateVO();
        vo.setSessionId(100L);

        mockMvc.perform(post("/api/v1/live/ai/generate-opening")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
