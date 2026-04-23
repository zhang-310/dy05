package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeEvolutionService;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionOpportunityVO;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionReportVO;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * KnowledgeEvolutionController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("KnowledgeEvolutionController 集成测试")
class KnowledgeEvolutionControllerTest {

    private static final String BASE_PATH = "/api/v1/ai/knowledge-evolution";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeEvolutionService evolutionService;

    @Test
    @DisplayName("分析进化机会 - 应返回 200")
    void analyzeEvolutionOpportunities_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("analysisScope", "LAST_7_DAYS");
        body.put("includeArchived", false);

        EvolutionOpportunityVO opportunity = new EvolutionOpportunityVO();
        opportunity.setAnalysisId("evol_123");
        opportunity.setPeriodStart("2026-03-31");
        opportunity.setPeriodEnd("2026-04-07");

        when(evolutionService.analyzeEvolutionOpportunities(eq(1L), eq(7)))
                .thenReturn(opportunity);

        mockMvc.perform(post(BASE_PATH + "/analyze")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.analysisId").value("evol_123"));
    }

    @Test
    @DisplayName("分析进化机会（默认范围）- 应返回 200")
    void analyzeEvolutionOpportunities_defaultScope_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        EvolutionOpportunityVO opportunity = new EvolutionOpportunityVO();
        opportunity.setAnalysisId("evol_456");

        when(evolutionService.analyzeEvolutionOpportunities(eq(1L), eq(7)))
                .thenReturn(opportunity);

        mockMvc.perform(post(BASE_PATH + "/analyze")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.analysisId").value("evol_456"));
    }

    @Test
    @DisplayName("执行自动优化 - 应返回 200")
    void executeAutoOptimization_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("analysisId", "evol_123");
        Map<String, Object> actions = new HashMap<>();
        actions.put("autoInclude", true);
        actions.put("autoMerge", true);
        actions.put("autoArchive", false);
        body.put("actions", actions);

        Map<String, Object> result = new HashMap<>();
        result.put("included", 5);
        result.put("merged", 3);
        result.put("archived", 0);

        when(evolutionService.executeAutoOptimization(eq(1L), eq("evol_123"), eq(true), eq(true), eq(false)))
                .thenReturn(result);

        mockMvc.perform(post(BASE_PATH + "/auto-optimize")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.included").value(5))
                .andExpect(jsonPath("$.data.merged").value(3));
    }

    @Test
    @DisplayName("生成进化报告 - 应返回 200")
    void generateEvolutionReport_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("reportType", "WEEKLY");
        body.put("includeTopScripts", true);

        EvolutionReportVO report = new EvolutionReportVO();
        report.setReportId("report_123");
        report.setPeriod("2026-03-31 to 2026-04-07");

        when(evolutionService.generateEvolutionReport(eq(1L), eq("WEEKLY"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(report);

        mockMvc.perform(post(BASE_PATH + "/report")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.reportId").value("report_123"))
                .andExpect(jsonPath("$.data.period").value("2026-03-31 to 2026-04-07"));
    }

    @Test
    @DisplayName("手动触发去重 - 应返回 200")
    void deduplicateKnowledge_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("similarityThreshold", 0.85);

        Map<String, Object> result = new HashMap<>();
        result.put("duplicatesFound", 8);
        result.put("duplicatesRemoved", 6);

        when(evolutionService.deduplicateKnowledge(eq(1L), any(BigDecimal.class)))
                .thenReturn(result);

        mockMvc.perform(post(BASE_PATH + "/deduplicate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.duplicatesFound").value(8))
                .andExpect(jsonPath("$.data.duplicatesRemoved").value(6));
    }

    @Test
    @DisplayName("手动触发去重（默认阈值）- 应返回 200")
    void deduplicateKnowledge_defaultThreshold_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        Map<String, Object> result = new HashMap<>();
        result.put("duplicatesFound", 5);

        when(evolutionService.deduplicateKnowledge(eq(1L), any(BigDecimal.class)))
                .thenReturn(result);

        mockMvc.perform(post(BASE_PATH + "/deduplicate")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.duplicatesFound").value(5));
    }

    @Test
    @DisplayName("获取进化历史 - 应返回 200")
    void getEvolutionHistory_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptVersionId", 123L);
        body.put("page", 0);
        body.put("rows", 10);

        Map<String, Object> history1 = new HashMap<>();
        history1.put("id", 1L);
        history1.put("action", "merged");
        history1.put("timestamp", "2026-04-01T10:00:00");

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>(1L, List.of(history1), 0, 10);

        when(evolutionService.getEvolutionHistory(eq(1L), eq(123L), eq(0), eq(10)))
                .thenReturn(pageResult);

        mockMvc.perform(post(BASE_PATH + "/history")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].action").value("merged"));
    }

    @Test
    @DisplayName("获取进化历史（默认分页）- 应返回 200")
    void getEvolutionHistory_defaultPagination_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scriptVersionId", 123L);

        PageResultVO<Map<String, Object>> pageResult = new PageResultVO<>(0L, List.of(), 0, 20);

        when(evolutionService.getEvolutionHistory(eq(1L), eq(123L), eq(0), eq(20)))
                .thenReturn(pageResult);

        mockMvc.perform(post(BASE_PATH + "/history")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
