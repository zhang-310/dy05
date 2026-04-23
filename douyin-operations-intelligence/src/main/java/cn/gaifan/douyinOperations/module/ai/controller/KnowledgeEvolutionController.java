package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeEvolutionService;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionOpportunityVO;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionReportVO;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Knowledge Library Evolution Controller
 * API endpoints for knowledge-library evolution analysis, optimization, and reporting.
 * Deliberately separated from {@link EvolutionController} to avoid sharing the same
 * /api/v1/ai/evolution namespace with viral/live-review workflows.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/knowledge-evolution")
public class KnowledgeEvolutionController {

    @Resource
    private KnowledgeEvolutionService evolutionService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    /**
     * Analyze evolution opportunities
     *
     * POST /api/v1/ai/knowledge-evolution/analyze
     * {
     *   "analysisScope": "LAST_7_DAYS",
     *   "includeArchived": false
     * }
     */
    @PostMapping("/analyze")
    public RESTResult<EvolutionOpportunityVO> analyzeEvolutionOpportunities(
            @RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("Analyzing evolution opportunities");
        String scope = (String) request.getOrDefault("analysisScope", "LAST_7_DAYS");
        int periodDays = parsePeriodDays(scope);

        EvolutionOpportunityVO analysis = evolutionService.analyzeEvolutionOpportunities(userId, periodDays);
        return RESTResult.success("Evolution analysis completed", analysis);
    }

    /**
     * Execute auto-optimization
     *
     * POST /api/v1/ai/knowledge-evolution/auto-optimize
     * {
     *   "analysisId": "evol_xxx",
     *   "actions": {
     *     "autoInclude": true,
     *     "autoMerge": true,
     *     "autoArchive": true
     *   },
     *   "approvalRequired": false
     * }
     */
    @PostMapping("/auto-optimize")
    public RESTResult<Map<String, Object>> executeAutoOptimization(
            @RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("Executing auto-optimization");
        String analysisId = (String) request.get("analysisId");

        @SuppressWarnings("unchecked")
        Map<String, Object> actions = (Map<String, Object>) request.getOrDefault("actions", Map.of());
        boolean autoInclude = (Boolean) actions.getOrDefault("autoInclude", false);
        boolean autoMerge = (Boolean) actions.getOrDefault("autoMerge", false);
        boolean autoArchive = (Boolean) actions.getOrDefault("autoArchive", false);

        Map<String, Object> result = evolutionService.executeAutoOptimization(userId, analysisId,
                autoInclude, autoMerge, autoArchive);
        return RESTResult.success("Auto-optimization executed", result);
    }

    /**
     * Generate evolution report
     *
     * POST /api/v1/ai/knowledge-evolution/report
     * {
     *   "reportType": "WEEKLY",
     *   "includeTopScripts": true,
     *   "includeStyleAnalysis": true,
     *   "page": 0,
     *   "rows": 20
     * }
     */
    @PostMapping("/report")
    public RESTResult<EvolutionReportVO> generateEvolutionReport(
            @RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("Generating evolution report");
        String reportType = (String) request.getOrDefault("reportType", "WEEKLY");
        LocalDate now = LocalDate.now();
        LocalDate startDate = reportType.equals("WEEKLY") ? now.minusWeeks(1) : now.minusMonths(1);

        EvolutionReportVO report = evolutionService.generateEvolutionReport(userId, reportType, startDate, now);
        return RESTResult.success("Report generated", report);
    }

    /**
     * Manually trigger deduplication
     *
     * POST /api/v1/ai/knowledge-evolution/deduplicate
     * {
     *   "similarityThreshold": 0.85
     * }
     */
    @PostMapping("/deduplicate")
    public RESTResult<Map<String, Object>> deduplicateKnowledge(
            @RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("Triggering deduplication");
        BigDecimal threshold = request.containsKey("similarityThreshold")
                ? new BigDecimal(((Number) request.get("similarityThreshold")).doubleValue())
                : new BigDecimal("0.85");

        Map<String, Object> result = evolutionService.deduplicateKnowledge(userId, threshold);
        return RESTResult.success("Deduplication completed", result);
    }

    /**
     * Get evolution history for a script
     *
     * POST /api/v1/ai/knowledge-evolution/history
     * {
     *   "scriptVersionId": 123,
     *   "page": 0,
     *   "rows": 10
     * }
     */
    @PostMapping("/history")
    public RESTResult<PageResultVO<Map<String, Object>>> getEvolutionHistory(
            @RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("Fetching evolution history");
        Long scriptVersionId = ((Number) request.get("scriptVersionId")).longValue();
        int page = ((Number) request.getOrDefault("page", 0)).intValue();
        int rows = ((Number) request.getOrDefault("rows", 20)).intValue();

        PageResultVO<Map<String, Object>> history = evolutionService.getEvolutionHistory(userId, scriptVersionId, page, rows);
        return RESTResult.success("History retrieved", history);
    }

    // ─── Private Helper Methods ───

    private int parsePeriodDays(String scope) {
        return switch (scope) {
            case "LAST_7_DAYS" -> 7;
            case "LAST_14_DAYS" -> 14;
            case "LAST_30_DAYS" -> 30;
            case "LAST_90_DAYS" -> 90;
            default -> 7;
        };
    }
}
