package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 直播话术管理 Controller
 * Live Script Management Controller
 */
@RestController
@RequestMapping("/api/v1/live/script")
@Tag(name = "直播话术 / Live Script", description = "直播话术的管理（需登录）")
public class LiveScriptController {

    @Resource
    private LiveScriptService liveScriptService;
    @Resource
    private DataScopeResolver dataScopeService;
    @Resource
    private LiveSessionRepository liveSessionRepository;

    @PostMapping("/search")
    @Operation(
            summary = "查询直播话术 / Search Live Scripts",
            description = "分页查询直播话术（需登录，非管理员仅能查看自己场次的话术） / Search and paginate live scripts (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<LiveScriptVO>> search(HttpServletRequest request,
                                                         @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                 description = "查询条件 / Search criteria",
                                                                 required = false
                                                         )
                                                         @RequestBody(required = false) LiveScriptSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo == null) vo = new LiveScriptSearchVO();
        // 数据范围：按角色限制可见场次
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleUserIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleUserIds != null && vo.getSessionId() == null) {
            List<Long> visibleSessionIds = liveSessionRepository.findIdsByUserIdIn(visibleUserIds);
            vo.setSessionIds(visibleSessionIds);
        }
        PageResultVO<LiveScriptVO> data = liveScriptService.search(vo);
        RESTResult<PageResultVO<LiveScriptVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(
            summary = "获取话术详情 / Get Script Details",
            description = "根据话术 ID 获取话术详细信息（需登录） / Get script details by ID (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "话术不存在 / Script not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<LiveScriptVO> get(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body);
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        LiveScriptVO data = liveScriptService.getById(id);
        RESTResult<LiveScriptVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(
            summary = "保存话术 / Save Script",
            description = "创建或更新直播话术信息（需登录） / Create or update live script (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "保存成功 / Save successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "话术信息 / Script information",
            required = true
    ) @RequestBody LiveScriptSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        long id = liveScriptService.save(vo);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(
            summary = "删除话术 / Delete Script",
            description = "根据话术 ID 删除直播话术（需登录） / Delete live script by ID (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "删除成功 / Delete successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "话术不存在 / Script not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> delete(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body);
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        liveScriptService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/by-session")
    @Operation(
            summary = "获取场次的话术列表 / Get Scripts by Session",
            description = "获取指定直播场次下的所有话术列表（需登录） / Get all scripts in specified live session (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "场次不存在 / Session not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<List<LiveScriptVO>> getBySessionId(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = parseSessionId(body);
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        List<LiveScriptVO> data = liveScriptService.getBySessionId(sessionId);
        RESTResult<List<LiveScriptVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/effectiveness")
    @Operation(
            summary = "话术效果排行 / Get Script Effectiveness Ranking",
            description = "获取指定场次内话术按顺序的效果排行（需登录）")
    public RESTResult<List<LiveScriptVO>> getEffectiveness(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = parseSessionId(body);
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        List<LiveScriptVO> data = liveScriptService.getEffectiveness(sessionId);
        RESTResult<List<LiveScriptVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/executed")
    @Operation(
            summary = "更新话术执行状态 / Update Script Execution Status",
            description = "更新话术的执行状态（需登录） / Update script execution status (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "更新成功 / Update successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "话术不存在 / Script not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> updateExecuted(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body);
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Integer executed = body != null && body.get("executed") != null ? ((Number) body.get("executed")).intValue() : null;
        if (executed == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 executed");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        liveScriptService.updateExecuted(id, executed);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save-to-library")
    @Operation(summary = "保存话术到话术库 / Save Script to Library")
    public RESTResult<Long> saveToLibrary(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long scriptId = body != null && body.get("scriptId") != null ? ((Number) body.get("scriptId")).longValue() : null;
        if (scriptId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 scriptId");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        long id = liveScriptService.saveToLibrary(scriptId, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save-batch-to-library")
    @Operation(summary = "批量保存话术到话术库 / Save Batch Scripts to Library")
    public RESTResult<Integer> saveBatchToLibrary(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = parseSessionId(body);
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        java.util.List<Long> scriptIds = java.util.Collections.emptyList();
        if (body != null && body.get("scriptIds") instanceof java.util.List<?> raw) {
            scriptIds = raw.stream().filter(Number.class::isInstance).map(n -> ((Number) n).longValue()).toList();
        }
        int count = liveScriptService.saveBatchToLibrary(sessionId, scriptIds, userId);
        RESTResult<Integer> r = RESTResult.getSuccess(count);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/export")
    @Operation(summary = "导出场次话术 / Export Session Scripts")
    public RESTResult<String> exportScripts(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = parseSessionId(body);
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        String text = liveScriptService.exportScripts(sessionId);
        RESTResult<String> r = RESTResult.getSuccess(text);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseId(java.util.Map<String, Object> body) {
        if (body == null) return null;
        Object v = body.get("id");
        if (v == null) return null;
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }

    private static Long parseSessionId(java.util.Map<String, Object> body) {
        if (body == null) return null;
        Object sid = body.get("sessionId");
        if (sid == null) return null;
        return sid instanceof Number ? ((Number) sid).longValue() : Long.parseLong(sid.toString());
    }
}
