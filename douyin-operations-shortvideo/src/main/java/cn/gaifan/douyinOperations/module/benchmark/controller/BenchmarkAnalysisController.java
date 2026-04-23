package cn.gaifan.douyinOperations.module.benchmark.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkAnalysisService;
import cn.gaifan.douyinOperations.module.benchmark.vo.AnalyzeVideoVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BatchAnalyzeVideosVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkAnalysisVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 深度分析Controller
 */
@Tag(name = "深度分析")
@RestController
@RequestMapping("/api/v1/benchmark/analysis")
@RequiredArgsConstructor
public class BenchmarkAnalysisController {

    private final BenchmarkAnalysisService analysisService;

    @Operation(summary = "分析单个视频")
    @PostMapping("/analyze")
    public RESTResult<BenchmarkAnalysisVO> analyze(@Valid @RequestBody AnalyzeVideoVO analyzeVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        BenchmarkAnalysisVO result = analysisService.analyzeVideo(analyzeVO, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "批量分析视频")
    @PostMapping("/batch-analyze")
    public RESTResult<Long> batchAnalyze(@Valid @RequestBody BatchAnalyzeVideosVO batchVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        Long taskId = analysisService.batchAnalyzeVideos(batchVO, ownerId);
        return RESTResult.success(taskId);
    }

    @Operation(summary = "获取分析结果")
    @PostMapping("/get-by-video")
    public RESTResult<BenchmarkAnalysisVO> getByVideo(@RequestBody Long videoId, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        BenchmarkAnalysisVO result = analysisService.getByVideoId(videoId, ownerId);
        return RESTResult.success(result);
    }

    private Long requireCurrentUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }
}
