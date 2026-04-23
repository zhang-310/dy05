package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 运营看板：调用量/额度趋势（管理员）
 */
@Tag(name = "AI 运营看板")
@RestController
@RequestMapping("/api/v1/ai/admin/dashboard")
public class AiDashboardController {

    @Resource
    private AiDashboardService aiDashboardService;

    @Operation(summary = "仪表盘概览统计")
    @PostMapping("/stats")
    public RESTResult<Map<String, Object>> dashboardStats(HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(aiDashboardService.getDashboardStats());
    }

    @Operation(summary = "调用量趋势（按日或按小时）")
    @PostMapping("/call-volume-trend")
    public RESTResult<List<Map<String, Object>>> callVolumeTrend(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        String callType = body != null && body.get("callType") instanceof String s ? s : null;
        if (body != null && body.get("hours") != null) {
            int hours = ((Number) body.get("hours")).intValue();
            hours = Math.min(Math.max(hours, 1), 168);
            return RESTResult.getSuccess(aiDashboardService.getCallVolumeTrendByHour(hours, callType));
        }
        int days = body != null && body.get("days") != null ? ((Number) body.get("days")).intValue() : 30;
        days = Math.min(Math.max(days, 1), 90);
        return RESTResult.getSuccess(aiDashboardService.getCallVolumeTrend(days, callType));
    }

    @Operation(summary = "额度使用趋势（按日）")
    @PostMapping("/quota-trend")
    public RESTResult<List<Map<String, Object>>> quotaTrend(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        int days = body != null && body.get("days") != null ? ((Number) body.get("days")).intValue() : 30;
        days = Math.min(Math.max(days, 1), 90);
        return RESTResult.getSuccess(aiDashboardService.getQuotaTrend(days));
    }

    @Operation(summary = "调用类型分布")
    @PostMapping("/call-type-distribution")
    public RESTResult<List<Map<String, Object>>> callTypeDistribution(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        int days = body != null && body.get("days") != null ? ((Number) body.get("days")).intValue() : 30;
        days = Math.min(Math.max(days, 1), 90);
        return RESTResult.getSuccess(aiDashboardService.getCallTypeDistribution(days));
    }

    @Operation(summary = "用量/Token 拆解（按 call_type，最近 N 天）")
    @PostMapping("/cost-breakdown")
    public RESTResult<List<Map<String, Object>>> costBreakdown(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        int days = body != null && body.get("days") != null ? ((Number) body.get("days")).intValue() : 30;
        days = Math.min(Math.max(days, 1), 90);
        return RESTResult.getSuccess(aiDashboardService.getCostBreakdown(days));
    }

    private void requireAdmin(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (roleCode == null || !"admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }
}
