package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.module.product.service.ScriptOptimizationService;
import cn.gaifan.douyinOperations.module.product.vo.OptimizationSuggestionVO;
import cn.gaifan.douyinOperations.module.product.vo.RegeneratedScriptVO;
import cn.gaifan.douyinOperations.module.product.vo.ScriptAnalysisResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 话术优化建议控制器
 * 提供话术分析、优化建议生成、话术重新生成等接口
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/product/script")
@Tag(name = "话术优化建议系统", description = "AI 话术自动优化建议 API")
public class ScriptOptimizationController {

    private final ScriptOptimizationService scriptOptimizationService;

    public ScriptOptimizationController(ScriptOptimizationService scriptOptimizationService) {
        this.scriptOptimizationService = scriptOptimizationService;
    }

    /**
     * 分析话术效果
     *
     * Request JSON:
     * {
     *   "scriptVersionId": 12345,
     *   "dataSource": "LIVE_MONITOR",
     *   "analysisType": "COMPREHENSIVE"
     * }
     */
    @PostMapping("/analyze")
    @Operation(summary = "分析话术效果", description = "对话术版本进行综合分析，识别弱点和改进机会")
    public RESTResult<ScriptAnalysisResultVO> analyzeScript(@RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("分析话术接口：request={}", request);

        Long scriptVersionId = Long.valueOf(request.get("scriptVersionId").toString());
        String dataSource = request.getOrDefault("dataSource", "LIVE_MONITOR").toString();
        String analysisType = request.getOrDefault("analysisType", "COMPREHENSIVE").toString();

        ScriptAnalysisResultVO result = scriptOptimizationService.analyzeScript(
                scriptVersionId, dataSource, analysisType, userId);

        return RESTResult.success(result);
    }

    /**
     * 获取优化建议
     *
     * Request JSON:
     * {
     *   "scriptVersionId": 12345,
     *   "analysisResultId": 99999,
     *   "topN": 10
     * }
     */
    @PostMapping("/suggestions")
    @Operation(summary = "获取优化建议", description = "基于分析结果，生成高优先级的优化建议")
    public RESTResult<List<OptimizationSuggestionVO>> getOptimizationSuggestions(
            @RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("获取优化建议接口：request={}", request);

        Long scriptVersionId = Long.valueOf(request.get("scriptVersionId").toString());
        Long analysisResultId = Long.valueOf(request.get("analysisResultId").toString());
        Integer topN = request.containsKey("topN") ? Integer.valueOf(request.get("topN").toString()) : 10;

        List<OptimizationSuggestionVO> suggestions = scriptOptimizationService.getOptimizationSuggestions(
                scriptVersionId, analysisResultId, topN, userId);

        return RESTResult.success(suggestions);
    }

    /**
     * 重新生成话术
     *
     * Request JSON:
     * {
     *   "scriptVersionId": 12345,
     *   "suggestionId": 55555,
     *   "generationStyles": ["FRIENDLY", "HUMOROUS", "PREMIUM"]
     * }
     */
    @PostMapping("/regenerate")
    @Operation(summary = "重新生成话术", description = "基于优化建议，使用 AI 重新生成不同风格的话术版本")
    public RESTResult<RegeneratedScriptResponse> regenerateOptimized(@RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("重新生成话术接口：request={}", request);

        Long scriptVersionId = Long.valueOf(request.get("scriptVersionId").toString());
        Long suggestionId = Long.valueOf(request.get("suggestionId").toString());
        @SuppressWarnings("unchecked")
        List<String> generationStyles = (List<String>) request.getOrDefault("generationStyles",
                List.of("FRIENDLY", "HUMOROUS", "PREMIUM"));

        List<RegeneratedScriptVO> variants = scriptOptimizationService.regenerateScript(
                scriptVersionId, suggestionId, generationStyles, userId);

        RegeneratedScriptResponse response = RegeneratedScriptResponse.builder()
                .regenerationTaskId("regen_" + System.currentTimeMillis())
                .variants(variants)
                .generatedAt(java.time.LocalDateTime.now())
                .build();

        return RESTResult.success(response);
    }

    /**
     * 查询优化历史
     *
     * Request JSON:
     * {
     *   "scriptVersionId": 12345,
     *   "page": 0,
     *   "rows": 10
     * }
     */
    @PostMapping("/optimization-history")
    @Operation(summary = "查询优化历史", description = "查看特定话术版本的优化历史记录")
    public RESTResult<PageResultVO<ScriptAnalysisResultVO>> optimizationHistory(
            @RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("查询优化历史接口：request={}", request);

        Long scriptVersionId = Long.valueOf(request.get("scriptVersionId").toString());
        Integer page = request.containsKey("page") ? Integer.valueOf(request.get("page").toString()) : 0;
        Integer rows = request.containsKey("rows") ? Integer.valueOf(request.get("rows").toString()) : 30;

        PageResultVO<ScriptAnalysisResultVO> result = scriptOptimizationService.getOptimizationHistory(
                scriptVersionId, page, rows, userId);

        return RESTResult.success(result);
    }

    /**
     * 采纳优化建议
     *
     * Request JSON:
     * {
     *   "suggestionId": 55555
     * }
     */
    @PostMapping("/accept-suggestion")
    @Operation(summary = "采纳优化建议", description = "标记优化建议为已采纳")
    public RESTResult<Boolean> acceptSuggestion(@RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("采纳建议接口：request={}", request);

        Long suggestionId = Long.valueOf(request.get("suggestionId").toString());

        Boolean result = scriptOptimizationService.acceptSuggestion(suggestionId, userId);

        return RESTResult.success(result);
    }

    /**
     * 拒绝优化建议
     *
     * Request JSON:
     * {
     *   "suggestionId": 55555,
     *   "notes": "不适用于我们的场景"
     * }
     */
    @PostMapping("/reject-suggestion")
    @Operation(summary = "拒绝优化建议", description = "标记优化建议为已拒绝")
    public RESTResult<Boolean> rejectSuggestion(@RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("拒绝建议接口：request={}", request);

        Long suggestionId = Long.valueOf(request.get("suggestionId").toString());
        String notes = request.getOrDefault("notes", "").toString();

        Boolean result = scriptOptimizationService.rejectSuggestion(suggestionId, notes, userId);

        return RESTResult.success(result);
    }

    /**
     * 应用重新生成的话术版本
     *
     * Request JSON:
     * {
     *   "regeneratedVersionId": 1001
     * }
     */
    @PostMapping("/apply-regenerated")
    @Operation(summary = "应用重新生成的话术", description = "将生成的话术版本应用到实际使用中")
    public RESTResult<Boolean> applyRegeneratedVersion(@RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("应用生成版本接口：request={}", request);

        Long regeneratedVersionId = Long.valueOf(request.get("regeneratedVersionId").toString());

        Boolean result = scriptOptimizationService.applyRegeneratedVersion(regeneratedVersionId, userId);

        return RESTResult.success(result);
    }

    /**
     * 审批重新生成的话术版本
     *
     * Request JSON:
     * {
     *   "regeneratedVersionId": 1001,
     *   "approvalStatus": "APPROVED",
     *   "notes": "内容质量不错"
     * }
     */
    @PostMapping("/approve-regenerated")
    @Operation(summary = "审批重新生成的话术", description = "管理员审批生成的话术版本")
    public RESTResult<Boolean> approveRegeneratedVersion(@RequestBody Map<String, Object> request,
            @CurrentUserId Long userId) {
        log.info("审批生成版本接口：request={}", request);

        Long regeneratedVersionId = Long.valueOf(request.get("regeneratedVersionId").toString());
        String approvalStatus = request.getOrDefault("approvalStatus", "APPROVED").toString();
        String notes = request.getOrDefault("notes", "").toString();
        Long approverUserId = userId;

        Boolean result;
        if ("APPROVED".equalsIgnoreCase(approvalStatus)) {
            result = scriptOptimizationService.approveRegeneratedVersion(regeneratedVersionId, approverUserId, notes);
        } else {
            result = scriptOptimizationService.rejectRegeneratedVersion(regeneratedVersionId, approverUserId, notes);
        }

        return RESTResult.success(result);
    }

    /**
     * 重新生成话术响应 VO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class RegeneratedScriptResponse {
        private String regenerationTaskId;
        private List<RegeneratedScriptVO> variants;
        private java.time.LocalDateTime generatedAt;
    }
}
