package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.system.service.SystemService;
import cn.gaifan.douyinOperations.module.system.vo.ApiLogIdVO;
import cn.gaifan.douyinOperations.module.system.vo.ApiLogSearchVO;
import cn.gaifan.douyinOperations.module.system.vo.ApiLogStatsVO;
import cn.gaifan.douyinOperations.module.system.vo.SyncLogSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
            @Valid @RequestBody(required = false) ApiLogSearchVO vo) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        // P1-9: 使用 BasicQueryDto 自动校验分页参数
        if (vo == null) vo = new ApiLogSearchVO();
        vo.validateParams();

        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(
                systemService.searchApiLogs(vo.getModule(), vo.getApiName(), vo.getStatus(),
                        vo.getStartTime(), vo.getEndTime(), vo.getSortName(), vo.getSortOrder(),
                        vo.getPage(), vo.getRows()));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/api-log/stats")
    @Operation(summary = "API调用统计")
    public RESTResult<Map<String, Object>> apiLogStats(
            HttpServletRequest request,
            @Valid @RequestBody(required = false) ApiLogStatsVO vo) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        // P1-9: 使用强类型 VO，避免 Map 手动转换
        if (vo == null) vo = new ApiLogStatsVO();

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(
                systemService.getApiLogStats(vo.getModule(), vo.getStartTime(), vo.getEndTime()));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/api-log/get")
    @Operation(summary = "API调用日志详情（含请求/响应体）")
    public RESTResult<Map<String, Object>> apiLogGet(
            HttpServletRequest request,
            @Valid @RequestBody ApiLogIdVO vo) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        // P1-9: 使用强类型 VO，@Valid 自动校验 @NotNull
        Map<String, Object> data = systemService.getApiLogById(vo.getId());
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
            @Valid @RequestBody(required = false) SyncLogSearchVO vo) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        // P1-9: 使用 BasicQueryDto 自动校验分页参数
        if (vo == null) vo = new SyncLogSearchVO();
        vo.validateParams();

        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(
                systemService.searchSyncLogs(vo.getSyncType(), vo.getStatus(), vo.getUserId(),
                        vo.getStartTime(), vo.getEndTime(), vo.getPage(), vo.getRows()));
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
