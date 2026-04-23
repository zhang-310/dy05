package cn.gaifan.douyinOperations.module.benchmark.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkQualityScriptService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 质量脚本知识库 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/benchmark/quality-script")
@RequiredArgsConstructor
@Tag(name = "Benchmark - 质量脚本知识库", description = "质量脚本的管理和推荐功能")
public class BenchmarkQualityScriptController {

    private final BenchmarkQualityScriptService qualityScriptService;

    @PostMapping("/search")
    @Operation(summary = "分页查询质量脚本列表")
    public RESTResult<PageResultVO<BenchmarkQualityScriptVO>> search(
            @RequestBody @Valid BenchmarkQualityScriptSearchVO searchVO,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        PageResultVO<BenchmarkQualityScriptVO> result = qualityScriptService.search(searchVO, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/get")
    @Operation(summary = "根据 ID 查询质量脚本详情")
    public RESTResult<BenchmarkQualityScriptVO> getById(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long id = request.get("id");
        BenchmarkQualityScriptVO result = qualityScriptService.getById(id, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/save")
    @Operation(summary = "保存质量脚本（新增或更新）")
    public RESTResult<BenchmarkQualityScriptVO> save(
            @RequestBody @Valid BenchmarkQualityScriptSaveVO saveVO,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        BenchmarkQualityScriptVO result = qualityScriptService.save(saveVO, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除质量脚本")
    public RESTResult<Void> delete(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long id = request.get("id");
        qualityScriptService.delete(id, ownerId);
        return RESTResult.success(null);
    }

    @PostMapping("/get-by-video")
    @Operation(summary = "根据视频 ID 查询质量脚本")
    public RESTResult<BenchmarkQualityScriptVO> getByVideoId(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long videoId = request.get("videoId");
        BenchmarkQualityScriptVO result = qualityScriptService.getByVideoId(videoId, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/get-by-analysis")
    @Operation(summary = "根据分析 ID 查询质量脚本")
    public RESTResult<BenchmarkQualityScriptVO> getByAnalysisId(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Long analysisId = request.get("analysisId");
        BenchmarkQualityScriptVO result = qualityScriptService.getByAnalysisId(analysisId, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/get-high-quality-by-industry")
    @Operation(summary = "获取指定行业的高质量脚本")
    public RESTResult<List<BenchmarkQualityScriptVO>> getHighQualityScriptsByIndustry(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        String industry = (String) request.get("industry");
        BigDecimal minScore = new BigDecimal(request.get("minScore").toString());
        List<BenchmarkQualityScriptVO> result = qualityScriptService.getHighQualityScriptsByIndustry(industry, minScore, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/get-high-quality-by-scene")
    @Operation(summary = "获取指定场景类型的高质量脚本")
    public RESTResult<List<BenchmarkQualityScriptVO>> getHighQualityScriptsByScene(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        String sceneType = (String) request.get("sceneType");
        BigDecimal minScore = new BigDecimal(request.get("minScore").toString());
        List<BenchmarkQualityScriptVO> result = qualityScriptService.getHighQualityScriptsByScene(sceneType, minScore, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/get-top-engagement")
    @Operation(summary = "获取互动率最高的脚本")
    public RESTResult<List<BenchmarkQualityScriptVO>> getTopEngagementScripts(
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Integer limit = request.getOrDefault("limit", 10);
        List<BenchmarkQualityScriptVO> result = qualityScriptService.getTopEngagementScripts(limit, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/get-top-viral")
    @Operation(summary = "获取传播力最高的脚本")
    public RESTResult<List<BenchmarkQualityScriptVO>> getTopViralScripts(
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        Integer limit = request.getOrDefault("limit", 10);
        List<BenchmarkQualityScriptVO> result = qualityScriptService.getTopViralScripts(limit, ownerId);
        return RESTResult.success(result);
    }

    @PostMapping("/calculate-quality-score")
    @Operation(summary = "计算质量评分")
    public RESTResult<BigDecimal> calculateQualityScore(
            @RequestBody @Valid BenchmarkQualityScriptSaveVO saveVO,
            Authentication authentication) {
        BigDecimal score = qualityScriptService.calculateQualityScore(saveVO);
        return RESTResult.success(score);
    }
}
