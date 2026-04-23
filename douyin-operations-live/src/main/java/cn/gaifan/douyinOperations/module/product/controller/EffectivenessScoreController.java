package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.service.EffectivenessScoreService;
import cn.gaifan.douyinOperations.module.product.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 商品话术效果评分 Controller
 * API 路径：/api/v1/product/script-effectiveness/
 *
 * 功能：
 * - 评分计算、排行榜查询
 * - 版本对比、风格对比
 * - 历史趋势、效果分析
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@RestController("productEffectivenessScoreController")
@RequestMapping("/api/v1/product/script-effectiveness")
@Tag(name = "商品话术效果评分", description = "支持评分计算、排行榜、对比分析等")
public class EffectivenessScoreController {

    @Resource
    private EffectivenessScoreService scoreService;

    /**
     * 获取排行榜
     * POST /api/v1/product/script-effectiveness/ranking
     */
    @PostMapping("/ranking")
    @Operation(summary = "获取话术排行榜", description = "支持按评分、使用次数、转化率等排序")
    public RESTResult<PageResultVO<ScriptRankingVO>> getRanking(
            @Parameter(name = "productId", description = "产品 ID", required = true)
            @RequestParam Long productId,
            @Parameter(name = "topN", description = "排行前 N（0 表示所有）")
            @RequestParam(required = false, defaultValue = "0") Integer topN,
            @Parameter(name = "sortBy", description = "排序字段（score/usage/conversion/interaction）")
            @RequestParam(required = false, defaultValue = "score") String sortBy,
            @Parameter(name = "page", description = "页码", required = true)
            @RequestParam Integer page,
            @Parameter(name = "rows", description = "每页数量", required = true)
            @RequestParam Integer rows,
            @CurrentUserId Long userId) {
        PageResultVO<ScriptRankingVO> result = scoreService.getRanking(productId, topN, sortBy, page, rows, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 版本对比
     * POST /api/v1/product/script-effectiveness/compare
     */
    @PostMapping("/compare")
    @Operation(summary = "对比话术版本", description = "支持对比最多 5 个版本的各项指标")
    public RESTResult<ScriptComparisonVO> compareVersions(
            @Parameter(name = "versionIds", description = "版本 ID 列表（最多 5 个）", required = true)
            @RequestParam List<Long> versionIds,
            @CurrentUserId Long userId) {
        ScriptComparisonVO result = scoreService.compareVersions(versionIds, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 获取历史趋势
     * POST /api/v1/product/script-effectiveness/trend
     */
    @PostMapping("/trend")
    @Operation(summary = "获取话术趋势", description = "获取指定天数内的评分、使用、转化等趋势数据")
    public RESTResult<ScriptTrendVO> getTrend(
            @Parameter(name = "versionId", description = "版本 ID", required = true)
            @RequestParam Long versionId,
            @Parameter(name = "days", description = "查询天数")
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @CurrentUserId Long userId) {
        ScriptTrendVO result = scoreService.getTrend(versionId, days, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 重新计算评分
     * POST /api/v1/product/script-effectiveness/recalculate
     */
    @PostMapping("/recalculate")
    @Operation(summary = "重新计算评分", description = "触发产品所有版本的效果评分重新计算")
    public RESTResult<Integer> recalculate(
            @Parameter(name = "productId", description = "产品 ID", required = true)
            @RequestParam Long productId,
            @CurrentUserId Long userId) {
        Integer result = scoreService.recalculateAllScores(productId, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 风格对比
     * POST /api/v1/product/script-effectiveness/style-comparison
     */
    @PostMapping("/style-comparison")
    @Operation(summary = "风格对比分析", description = "对比产品内不同风格话术的表现")
    public RESTResult<ScriptComparisonVO> getStyleComparison(
            @Parameter(name = "productId", description = "产品 ID", required = true)
            @RequestParam Long productId,
            @CurrentUserId Long userId) {
        ScriptComparisonVO result = scoreService.getStyleComparison(productId, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 获取效果分析
     * POST /api/v1/product/script-effectiveness/analysis
     */
    @PostMapping("/analysis")
    @Operation(summary = "获取效果分析", description = "获取版本的综合效果分析汇总")
    public RESTResult<ScriptEffectivenessAnalysisVO> getAnalysis(
            @Parameter(name = "versionId", description = "版本 ID", required = true)
            @RequestParam Long versionId,
            @CurrentUserId Long userId) {
        ScriptEffectivenessAnalysisVO result = scoreService.getAnalysis(versionId, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 记录快照
     * POST /api/v1/product/script-effectiveness/record-snapshot
     */
    @PostMapping("/record-snapshot")
    @Operation(summary = "记录评分快照", description = "记录当前时刻版本的评分及各项指标快照")
    public RESTResult<Boolean> recordSnapshot(
            @Parameter(name = "versionId", description = "版本 ID", required = true)
            @RequestParam Long versionId,
            @CurrentUserId Long userId) {
        boolean result = scoreService.recordSnapshot(versionId, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 清除缓存
     * POST /api/v1/product/script-effectiveness/clear-cache
     */
    @PostMapping("/clear-cache")
    @Operation(summary = "清除对比缓存", description = "清除指定产品的所有对比缓存")
    public RESTResult<Integer> clearCache(
            @Parameter(name = "productId", description = "产品 ID", required = true)
            @RequestParam Long productId,
            @CurrentUserId Long userId) {
        Integer result = scoreService.clearComparisonCache(productId, userId);
        return RESTResult.getSuccess(result);
    }

}
