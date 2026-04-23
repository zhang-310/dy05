package cn.gaifan.douyinOperations.module.benchmark.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkScriptRecommendationService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkScriptSimilarityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 质量脚本推荐引擎 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/benchmark/script-recommendation")
@RequiredArgsConstructor
@Tag(name = "Benchmark - 脚本推荐引擎", description = "基于语义相似度和质量评分的智能脚本推荐")
public class BenchmarkScriptRecommendationController {

    private final BenchmarkScriptRecommendationService recommendationService;

    @PostMapping("/recommend-by-requirement")
    @Operation(summary = "根据用户需求推荐脚本")
    public RESTResult<List<BenchmarkScriptSimilarityVO>> recommendByRequirement(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        String requirement = (String) request.get("requirement");
        Integer topK = request.containsKey("topK") ? Integer.parseInt(request.get("topK").toString()) : 10;

        List<BenchmarkScriptSimilarityVO> result = recommendationService.recommendByRequirement(requirement, ownerId, topK);
        return RESTResult.success(result);
    }

    @PostMapping("/recommend-by-industry-scene")
    @Operation(summary = "根据行业和场景推荐脚本")
    public RESTResult<List<BenchmarkScriptSimilarityVO>> recommendByIndustryAndScene(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        String industry = (String) request.get("industry");
        String sceneType = (String) request.get("sceneType");
        Integer topK = request.containsKey("topK") ? Integer.parseInt(request.get("topK").toString()) : 10;

        List<BenchmarkScriptSimilarityVO> result = recommendationService.recommendByIndustryAndScene(industry, sceneType, ownerId, topK);
        return RESTResult.success(result);
    }

    @PostMapping("/recommend-by-script-type")
    @Operation(summary = "根据脚本类型推荐相似脚本")
    public RESTResult<List<BenchmarkScriptSimilarityVO>> recommendByScriptType(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        String scriptType = (String) request.get("scriptType");
        Long referenceScriptId = request.containsKey("referenceScriptId")
                ? Long.parseLong(request.get("referenceScriptId").toString()) : null;
        Integer topK = request.containsKey("topK") ? Integer.parseInt(request.get("topK").toString()) : 10;

        List<BenchmarkScriptSimilarityVO> result = recommendationService.recommendByScriptType(scriptType, referenceScriptId, ownerId, topK);
        return RESTResult.success(result);
    }

    @PostMapping("/smart-recommend")
    @Operation(summary = "智能推荐：综合多维度推荐")
    public RESTResult<List<BenchmarkScriptSimilarityVO>> smartRecommend(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());

        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) request.getOrDefault("filters", Map.of());
        String referenceText = (String) request.get("referenceText");
        Integer topK = request.containsKey("topK") ? Integer.parseInt(request.get("topK").toString()) : 10;

        List<BenchmarkScriptSimilarityVO> result = recommendationService.smartRecommend(filters, referenceText, ownerId, topK);
        return RESTResult.success(result);
    }

    @PostMapping("/get-popular-scripts")
    @Operation(summary = "获取热门脚本推荐")
    public RESTResult<List<BenchmarkScriptSimilarityVO>> getPopularScripts(
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Integer topK = request.getOrDefault("topK", 10);

        List<BenchmarkScriptSimilarityVO> result = recommendationService.getPopularScripts(ownerId, topK);
        return RESTResult.success(result);
    }

    @PostMapping("/get-latest-quality-scripts")
    @Operation(summary = "获取最新高质量脚本推荐")
    public RESTResult<List<BenchmarkScriptSimilarityVO>> getLatestQualityScripts(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Integer topK = request.containsKey("topK") ? Integer.parseInt(request.get("topK").toString()) : 10;
        Double minQualityScore = request.containsKey("minQualityScore")
                ? Double.parseDouble(request.get("minQualityScore").toString()) : null;

        List<BenchmarkScriptSimilarityVO> result = recommendationService.getLatestQualityScripts(ownerId, topK, minQualityScore);
        return RESTResult.success(result);
    }

    @PostMapping("/recommend-improvement-scripts")
    @Operation(summary = "根据视频分析结果推荐改进脚本")
    public RESTResult<List<BenchmarkScriptSimilarityVO>> recommendImprovementScripts(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long analysisId = Long.parseLong(request.get("analysisId").toString());
        Integer topK = request.containsKey("topK") ? Integer.parseInt(request.get("topK").toString()) : 10;

        List<BenchmarkScriptSimilarityVO> result = recommendationService.recommendImprovementScripts(analysisId, ownerId, topK);
        return RESTResult.success(result);
    }
}
