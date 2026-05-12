package cn.gaifan.douyinOperations.module.dashboard.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.dashboard.service.DashboardGmvService;
import cn.gaifan.douyinOperations.module.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Dashboard 统计控制器
 */
@RestController("dashboardStatsController")
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard / 数据看板", description = "Dashboard 统计数据")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @Resource
    private DashboardGmvService dashboardGmvService;

    /**
     * 获取管理员 Dashboard 统计
     */
    @PostMapping("/admin/stats")
    @Operation(summary = "管理员统计 / Admin Stats")
    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "dashboard", fallbackMethod = "rateLimitFallback")
    public RESTResult<Map<String, Object>> getAdminStats(HttpServletRequest request) {
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (!"admin".equals(roleCode)) {
            return RESTResult.error(ErrorCode.PERMISSION_DENIED, "仅管理员可访问");
        }

        Map<String, Object> stats = dashboardService.getAdminStats();
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(stats);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 获取机构 Dashboard 统计
     */
    @PostMapping("/org/stats")
    @Operation(summary = "机构统计 / Organization Stats")
    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "dashboard", fallbackMethod = "rateLimitFallback")
    public RESTResult<Map<String, Object>> getOrgStats(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        Map<String, Object> stats = dashboardService.getOrgStats(userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(stats);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // P1-1: 限流降级方法
    private RESTResult<Map<String, Object>> rateLimitFallback(HttpServletRequest request,
                                                               io.github.resilience4j.ratelimiter.RequestNotPermitted ex) {
        return RESTResult.error(ErrorCode.RATE_LIMIT, "Dashboard 请求过于频繁，请稍后再试");
    }

    // ===================== D5 升级：KPI / GMV / 驾驶舱端点 =====================

    @PostMapping("/kpi-unified")
    @Operation(summary = "统一 KPI 综合指标")
    public RESTResult<Map<String, Object>> getUnifiedKpi(
            @RequestBody(required = false) Map<String, Object> body,
            @CurrentUserId Long userId) {
        int lookbackDays = body != null && body.get("lookbackDays") instanceof Number n ? n.intValue() : 30;
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(dashboardGmvService.getUnifiedKpi(userId, lookbackDays));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/live-format-gmv")
    @Operation(summary = "直播形式 GMV 分布")
    public RESTResult<Map<String, Object>> getLiveFormatGmv(
            @RequestBody(required = false) Map<String, Object> body,
            @CurrentUserId Long userId) {
        int lookbackDays = body != null && body.get("lookbackDays") instanceof Number n ? n.intValue() : 30;
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(dashboardGmvService.getLiveFormatGmv(userId, lookbackDays));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/product-gmv-summary")
    @Operation(summary = "商品 GMV 汇总")
    public RESTResult<Map<String, Object>> getProductGmvSummary(
            @RequestBody(required = false) Map<String, Object> body,
            @CurrentUserId Long userId) {
        int lookbackDays = body != null && body.get("lookbackDays") instanceof Number n ? n.intValue() : 30;
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(dashboardGmvService.getProductGmvSummary(userId, lookbackDays));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/cockpit-preview")
    @Operation(summary = "驾驶舱场次预览")
    public RESTResult<Map<String, Object>> getCockpitPreview(
            @RequestBody(required = false) Map<String, Object> body,
            @CurrentUserId Long userId) {
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(dashboardGmvService.getCockpitPreview(userId, body));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/profit-matrix-preview")
    @Operation(summary = "利润矩阵预览")
    public RESTResult<Map<String, Object>> getProfitMatrixPreview(
            @RequestBody(required = false) Map<String, Object> body,
            @CurrentUserId Long userId) {
        int lookbackDays = body != null && body.get("lookbackDays") instanceof Number n ? n.intValue() : 30;
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(dashboardGmvService.getProfitMatrixPreview(userId, lookbackDays));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/conversion-funnel")
    @Operation(summary = "转化漏斗")
    public RESTResult<Map<String, Object>> getConversionFunnel(
            @RequestBody(required = false) Map<String, Object> body,
            @CurrentUserId Long userId) {
        int lookbackDays = body != null && body.get("lookbackDays") instanceof Number n ? n.intValue() : 30;
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(dashboardGmvService.getConversionFunnel(userId, lookbackDays));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/cockpit-export")
    @Operation(summary = "驾驶舱 CSV 导出")
    public RESTResult<Map<String, Object>> exportCockpitCsv(
            @RequestBody(required = false) Map<String, Object> body,
            @CurrentUserId Long userId) {
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(dashboardGmvService.exportCockpitCsv(userId, body));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
