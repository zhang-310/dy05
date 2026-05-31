package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.product.service.EffectivenessScoreService;
import cn.gaifan.douyinOperations.module.product.vo.ScriptComparisonVO;
import cn.gaifan.douyinOperations.module.product.vo.ScriptEffectivenessAnalysisVO;
import cn.gaifan.douyinOperations.module.product.vo.ScriptRankingVO;
import cn.gaifan.douyinOperations.module.product.vo.ScriptTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品话术效果评分 Controller。
 */
@RestController("productEffectivenessScoreController")
@RequestMapping("/api/v1/product/script-effectiveness")
@Tag(name = "商品话术效果评分 / Product Script Effectiveness", description = "商品话术版本评分、趋势、对比和缓存管理")
public class EffectivenessScoreController {

    @Resource(name = "productEffectivenessScoreServiceImpl")
    private EffectivenessScoreService scoreService;

    @PostMapping("/ranking")
    @Operation(summary = "获取商品话术版本排行榜 / Get Product Script Ranking")
    public RESTResult<PageResultVO<ScriptRankingVO>> getRanking(
            HttpServletRequest request,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "10") Integer topN,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "30") Integer rows) {
        Long userId = requireUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (productId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");
        try {
            RESTResult<PageResultVO<ScriptRankingVO>> result = RESTResult.getSuccess(
                    scoreService.getRanking(productId, topN, sortBy, page, rows, userId));
            result.setTraceId(MDC.get("traceId"));
            return result;
        } catch (BusinessException e) {
            return RESTResult.error(e.getErrorCode(), e.getMessage());
        }
    }

    @PostMapping("/compare")
    @Operation(summary = "对比多个商品话术版本 / Compare Product Script Versions")
    public RESTResult<ScriptComparisonVO> compareVersions(
            HttpServletRequest request,
            @RequestParam List<Long> versionIds) {
        Long userId = requireUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        try {
            RESTResult<ScriptComparisonVO> result = RESTResult.getSuccess(scoreService.compareVersions(versionIds, userId));
            result.setTraceId(MDC.get("traceId"));
            return result;
        } catch (BusinessException e) {
            return RESTResult.error(e.getErrorCode(), e.getMessage());
        }
    }

    @PostMapping("/trend")
    @Operation(summary = "获取商品话术版本历史趋势 / Get Product Script Trend")
    public RESTResult<ScriptTrendVO> getTrend(
            HttpServletRequest request,
            @RequestParam Long versionId,
            @RequestParam(defaultValue = "30") Integer days) {
        Long userId = requireUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        try {
            RESTResult<ScriptTrendVO> result = RESTResult.getSuccess(scoreService.getTrend(versionId, days, userId));
            result.setTraceId(MDC.get("traceId"));
            return result;
        } catch (BusinessException e) {
            return RESTResult.error(e.getErrorCode(), e.getMessage());
        }
    }

    @PostMapping("/recalculate")
    @Operation(summary = "重新计算商品全部话术版本评分 / Recalculate Product Script Scores")
    public RESTResult<Integer> recalculate(
            HttpServletRequest request,
            @RequestParam Long productId) {
        Long userId = requireUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        try {
            RESTResult<Integer> result = RESTResult.updateSuccess(scoreService.recalculateAllScores(productId, userId));
            result.setTraceId(MDC.get("traceId"));
            return result;
        } catch (BusinessException e) {
            return RESTResult.error(e.getErrorCode(), e.getMessage());
        }
    }

    @PostMapping("/style-comparison")
    @Operation(summary = "获取商品话术风格对比 / Get Product Script Style Comparison")
    public RESTResult<ScriptComparisonVO> getStyleComparison(
            HttpServletRequest request,
            @RequestParam Long productId) {
        Long userId = requireUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        try {
            RESTResult<ScriptComparisonVO> result = RESTResult.getSuccess(scoreService.getStyleComparison(productId, userId));
            result.setTraceId(MDC.get("traceId"));
            return result;
        } catch (BusinessException e) {
            return RESTResult.error(e.getErrorCode(), e.getMessage());
        }
    }

    @PostMapping("/analysis")
    @Operation(summary = "获取商品话术效果分析 / Get Product Script Effectiveness Analysis")
    public RESTResult<ScriptEffectivenessAnalysisVO> getAnalysis(
            HttpServletRequest request,
            @RequestParam Long versionId) {
        Long userId = requireUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        try {
            RESTResult<ScriptEffectivenessAnalysisVO> result = RESTResult.getSuccess(scoreService.getAnalysis(versionId, userId));
            result.setTraceId(MDC.get("traceId"));
            return result;
        } catch (BusinessException e) {
            return RESTResult.error(e.getErrorCode(), e.getMessage());
        }
    }

    @PostMapping("/record-snapshot")
    @Operation(summary = "记录商品话术版本评分快照 / Record Product Script Score Snapshot")
    public RESTResult<Boolean> recordSnapshot(
            HttpServletRequest request,
            @RequestParam Long versionId) {
        Long userId = requireUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        try {
            RESTResult<Boolean> result = RESTResult.addSuccess(scoreService.recordSnapshot(versionId, userId));
            result.setTraceId(MDC.get("traceId"));
            return result;
        } catch (BusinessException e) {
            return RESTResult.error(e.getErrorCode(), e.getMessage());
        }
    }

    @PostMapping("/clear-cache")
    @Operation(summary = "清除商品话术效果对比缓存 / Clear Product Script Comparison Cache")
    public RESTResult<Integer> clearCache(
            HttpServletRequest request,
            @RequestParam Long productId) {
        Long userId = requireUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        try {
            RESTResult<Integer> result = RESTResult.deleteSuccess(scoreService.clearComparisonCache(productId, userId));
            result.setTraceId(MDC.get("traceId"));
            return result;
        } catch (BusinessException e) {
            return RESTResult.error(e.getErrorCode(), e.getMessage());
        }
    }

    private Long requireUserId(HttpServletRequest request) {
        return AuthTokenFilter.getUserId(request);
    }
}
