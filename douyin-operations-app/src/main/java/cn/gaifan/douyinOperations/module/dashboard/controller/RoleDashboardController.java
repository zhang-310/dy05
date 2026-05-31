package cn.gaifan.douyinOperations.module.dashboard.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.role.RoleDashboardProvider;
import cn.gaifan.douyinOperations.module.live.service.GmvTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 角色工作台 — 6 角色专属首页
 *
 * Vision: 不同角色看到不同首页数据
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "角色工作台 / Role Dashboard", description = "6 角色专属首页数据")
public class RoleDashboardController {

    @Resource
    private RoleDashboardProvider roleDashboard;

    @Resource
    private GmvTrackingService gmvTrackingService;

    @PostMapping("/role-dashboard")
    @Operation(summary = "角色专属首页")
    public RESTResult<Object> dashboard(HttpServletRequest request,
                                         @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (userId == null) userId = 0L;
        if (roleCode == null) roleCode = "admin";

        Object data = roleDashboard.buildDashboard(roleCode, userId);
        RESTResult<Object> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/gmv/session")
    @Operation(summary = "单场 GMV")
    public RESTResult<Object> sessionGmv(@RequestBody Map<String, Object> body) {
        Long sessionId = body.get("sessionId") instanceof Number n ? n.longValue() : null;
        if (sessionId == null) return RESTResult.error(400, "缺少 sessionId");

        var gmv = gmvTrackingService.getSessionGmv(sessionId);
        var completion = gmvTrackingService.getCompletionRate(sessionId);
        Map<String, Object> data = Map.of(
                "gmv", gmv,
                "gmvInWan", gmv.netGmvInWan(),
                "completionRate", completion
        );
        RESTResult<Object> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/gmv/top")
    @Operation(summary = "GMV 排行")
    public RESTResult<Object> topGmv(@RequestBody(required = false) Map<String, Object> body) {
        int limit = body != null && body.get("limit") instanceof Number n ? n.intValue() : 10;
        var sessions = gmvTrackingService.getTopSessionsByGmv(limit);
        RESTResult<Object> r = RESTResult.getSuccess(Map.of("sessions", sessions));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/briefing")
    @Operation(summary = "每日晨报")
    public RESTResult<Object> briefing() {
        var briefing = roleDashboard.buildDailyBriefing();
        RESTResult<Object> r = RESTResult.getSuccess(briefing);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
