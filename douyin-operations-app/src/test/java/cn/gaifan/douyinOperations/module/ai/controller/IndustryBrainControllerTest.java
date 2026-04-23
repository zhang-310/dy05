package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.service.brain.*;
import cn.gaifan.douyinOperations.module.ai.service.CompetitorInsightService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("IndustryBrainController 集成测试")
class IndustryBrainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IndustryKnowledgeGraphService knowledgeGraphService;

    @MockBean
    private TrendMonitorService trendMonitorService;

    @MockBean
    private UserCognitiveProfileService userCognitiveProfileService;

    @MockBean
    private CompetitorInsightService competitorInsightService;

    @Test
    @DisplayName("知识图谱查询 - 应返回 200")
    void knowledgeGraphQuery_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("entityType", "topic");
        body.put("keyword", "护肤");
        body.put("limit", 20);

        Map<String, Object> entity = new HashMap<>();
        entity.put("id", 1);
        entity.put("name", "护肤品");

        when(knowledgeGraphService.isAvailable()).thenReturn(true);
        when(knowledgeGraphService.queryEntities(anyString(), anyString(), anyInt(), anyLong()))
                .thenReturn(List.of(entity));

        mockMvc.perform(post("/api/v1/ai/brain/knowledge-graph/query")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("行业洞察 - 应返回真实聚合结构而非空数据")
    void industryInsights_shouldReturnAggregatedData() throws Exception {
        when(trendMonitorService.isAvailable()).thenReturn(true);
        when(trendMonitorService.getCurrentTrends(eq("护肤"), anyInt()))
                .thenReturn(List.of(new TrendMonitorService.TrendSignal(
                        "dy_1", "玻尿酸", "护肤", 88.0, System.currentTimeMillis(), "tianapi", "热搜"
                )));
        when(competitorInsightService.getRecentInsights("护肤", 7))
                .thenReturn(List.of(Map.of("summary", "竞品近期主打高保湿卖点")));
        when(competitorInsightService.getDifferentiationAdvice("护肤"))
                .thenReturn("建议强化功效证据与场景化表达");
        when(userCognitiveProfileService.isAvailable()).thenReturn(true);
        when(userCognitiveProfileService.getProfile(1L))
                .thenReturn(new UserCognitiveProfileService.UserProfile(
                        1L,
                        Map.of("种草", 0.8, "功效", 0.6),
                        List.of("口语化"),
                        0.5,
                        Map.of(),
                        System.currentTimeMillis()
                ));

        mockMvc.perform(post("/api/v1/ai/brain/industry/insights")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("category", "护肤"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.行业分类").value("护肤"))
                .andExpect(jsonPath("$.data.趋势热点[0]").value("玻尿酸"))
                .andExpect(jsonPath("$.data.差异化建议").value("建议强化功效证据与场景化表达"))
                .andExpect(jsonPath("$.data.数据口径").exists());
    }

    @Test
    @DisplayName("知识图谱查询（未登录）- 应返回 2001")
    void knowledgeGraphQuery_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("entityType", "topic");

        mockMvc.perform(post("/api/v1/ai/brain/knowledge-graph/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("查询相关子图 JSON - 应返回 200")
    void knowledgeGraphSubgraphJson_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("query", "护肤品");
        body.put("limit", 30);

        Map<String, Object> graph = new HashMap<>();
        graph.put("nodes", List.of());
        graph.put("edges", List.of());
        graph.put("contradictions", List.of());

        when(knowledgeGraphService.isAvailable()).thenReturn(true);
        when(knowledgeGraphService.getGraphJsonForQuery(anyString(), anyLong(), anyInt()))
                .thenReturn(graph);

        mockMvc.perform(post("/api/v1/ai/brain/knowledge-graph/subgraph-json")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("GraphRAG 多跳路径上下文 - 应返回 200")
    void graphRagContext_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("query", "护肤品推荐");

        when(knowledgeGraphService.isAvailable()).thenReturn(true);
        when(knowledgeGraphService.getGraphContextForQuery(anyString(), anyLong(), anyInt()))
                .thenReturn("相关上下文");

        mockMvc.perform(post("/api/v1/ai/brain/knowledge-graph/graphrag-context")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("GraphRAG 多跳路径上下文（未登录）- 应返回 2001")
    void graphRagContext_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("query", "护肤品推荐");

        mockMvc.perform(post("/api/v1/ai/brain/knowledge-graph/graphrag-context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
