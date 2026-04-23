package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.QualityDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 质量仪表板 API (Phase 6.3)
 * 路径：/api/v1/short-video/quality-dashboard
 */
@RestController
@RequestMapping("/api/v1/short-video/quality-dashboard")
@Tag(name = "质量仪表板", description = "视频质量统计与排名")
public class QualityDashboardController {

    @Resource
    private QualityDashboardService qualityDashboardService;

    @PostMapping("/overview")
    @Operation(summary = "概览")
    public RESTResult<Map<String, Object>> overview(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Map<String, Object> data = qualityDashboardService.getOverview(userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/trend")
    @Operation(summary = "质量趋势")
    public RESTResult<List<Map<String, Object>>> trend(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        int days = body != null && body.get("days") instanceof Number n ? n.intValue() : 30;
        if (days <= 0 || days > 90) days = 30;
        List<Map<String, Object>> data = qualityDashboardService.getQualityTrend(userId, days);
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/model-ranking")
    @Operation(summary = "模型排名")
    public RESTResult<List<Map<String, Object>>> modelRanking(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        int days = body != null && body.get("days") instanceof Number n ? n.intValue() : 30;
        if (days <= 0 || days > 90) days = 30;
        List<Map<String, Object>> data = qualityDashboardService.getModelRanking(userId, days);
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/camera-ranking")
    @Operation(summary = "运镜排名")
    public RESTResult<List<Map<String, Object>>> cameraRanking(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        int days = body != null && body.get("days") instanceof Number n ? n.intValue() : 30;
        if (days <= 0 || days > 90) days = 30;
        List<Map<String, Object>> data = qualityDashboardService.getCameraRanking(userId, days);
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/ai-reflections")
    @Operation(summary = "AI 反思")
    public RESTResult<List<String>> aiReflections(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        List<String> data = qualityDashboardService.getAiReflections(userId);
        RESTResult<List<String>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
