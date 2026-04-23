package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "系统监控 / System Monitor", description = "API调用日志、数据同步日志、健康检查（需管理员）")
public class SystemController {

    private static final String ROLE_ADMIN = "admin";

    @Resource
    private SystemService systemService;

    private boolean isAdmin(HttpServletRequest request) {
        return ROLE_ADMIN.equals(AuthTokenFilter.getRoleCode(request));
    }

    // ─── API 调用日志 ────────────────────────────────────────────

    @PostMapping("/api-log/list")
    @Operation(summary = "API调用日志列表")
    public RESTResult<PageResultVO<Map<String, Object>>> apiLogList(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        body = body == null ? Map.of() : body;
        String module    = (String) body.get("module");
        String apiName   = (String) body.get("apiName");
        Integer status   = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        String startTime = (String) body.get("startTime");
        String endTime   = (String) body.get("endTime");
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int rows = body.get("rows") != null ? ((Number) body.get("rows")).intValue() : 30;
        String sortName = (String) body.get("sortName");
        String sortOrder = (String) body.get("sortOrder");

        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(
                systemService.searchApiLogs(module, apiName, status, startTime, endTime, sortName, sortOrder, page, rows));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/api-log/stats")
    @Operation(summary = "API调用统计")
    public RESTResult<Map<String, Object>> apiLogStats(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        body = body == null ? Map.of() : body;
        String module    = (String) body.get("module");
        String startTime = (String) body.get("startTime");
        String endTime   = (String) body.get("endTime");

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(
                systemService.getApiLogStats(module, startTime, endTime));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/api-log/get")
    @Operation(summary = "API调用日志详情（含请求/响应体）")
    public RESTResult<Map<String, Object>> apiLogGet(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        Long id = body != null && body.get("id") != null ? ((Number) body.get("id")).longValue() : null;
        if (id == null)
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "id 不能为空");

        Map<String, Object> data = systemService.getApiLogById(id);
        if (data == null)
            return RESTResult.error(ErrorCode.DATA_NOT_FOUND, "记录不存在");

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 同步日志 ────────────────────────────────────────────────

    @PostMapping("/sync-log/list")
    @Operation(summary = "数据同步日志列表")
    public RESTResult<PageResultVO<Map<String, Object>>> syncLogList(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        body = body == null ? Map.of() : body;
        String syncType  = (String) body.get("syncType");
        String status    = (String) body.get("status");
        Long userId      = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
        String startTime = (String) body.get("startTime");
        String endTime   = (String) body.get("endTime");
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int rows = body.get("rows") != null ? ((Number) body.get("rows")).intValue() : 30;

        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(
                systemService.searchSyncLogs(syncType, status, userId, startTime, endTime, page, rows));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 系统监控 ────────────────────────────────────────────────

    @PostMapping("/health")
    @Operation(summary = "系统健康检查")
    public RESTResult<Map<String, Object>> health(HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(systemService.checkHealth());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/info")
    @Operation(summary = "系统运行信息")
    public RESTResult<Map<String, Object>> info(HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(systemService.getSystemInfo());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
