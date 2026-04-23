package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 短视频 Dashboard API
 * 路径：/api/v1/short-video/dashboard
 */
@RestController
@RequestMapping("/api/v1/short-video/dashboard")
@Tag(name = "短视频 Dashboard", description = "数据概览、趋势、项目列表")
public class ShortVideoDashboardController {

    @Resource(name = "shortVideoDashboardServiceImpl")
    private ShortVideoDashboardService dashboardService;

    @PostMapping("/stats")
    @Operation(summary = "数据概览")
    public RESTResult<Map<String, Object>> stats(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Map<String, Object> data = dashboardService.getStats(userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/trend")
    @Operation(summary = "播放量趋势")
    public RESTResult<List<Map<String, Object>>> trend(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        int days = body != null && body.get("days") instanceof Number n ? n.intValue() : 7;
        if (days < 1 || days > 30) days = 7;
        List<Map<String, Object>> data = dashboardService.getTrend(userId, days);
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/projects")
    @Operation(summary = "我的项目（含进度）")
    public RESTResult<List<Map<String, Object>>> projects(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String status = body != null && body.get("status") instanceof String s ? s : null;
        int page = body != null && body.get("page") instanceof Number n ? n.intValue() : 0;
        int rows = body != null && body.get("rows") instanceof Number n ? n.intValue() : 10;
        if (rows < 1 || rows > 50) rows = 10;
        List<Map<String, Object>> data = dashboardService.getProjectsWithProgress(userId, status, page, rows);
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/cost-breakdown")
    @Operation(summary = "成本分解")
    public RESTResult<Map<String, Object>> costBreakdown(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long projectId = body != null && body.get("projectId") instanceof Number n ? n.longValue() : null;
        Map<String, Object> data = dashboardService.getCostBreakdown(userId, projectId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
