package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.DailyShootService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

/**
 * 短视频项目管理 API
 * 路径：/api/v1/short-video/project
 */
@RestController
@RequestMapping("/api/v1/short-video/project")
@Tag(name = "短视频项目", description = "项目管理 CRUD")
public class ShortVideoProjectController {

    @Resource
    private SvProjectService projectService;

    @Resource
    private DailyShootService dailyShootService;

    @Resource
    private DataScopeResolver dataScopeService;

    @PostMapping("/list")
    @Operation(summary = "项目列表")
    public RESTResult<PageResultVO<SvProjectVO>> list(@RequestBody SvProjectSearchVO vo, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        PageResultVO<SvProjectVO> result = projectService.search(vo, userId, visibleIds);
        RESTResult<PageResultVO<SvProjectVO>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "项目详情")
    public RESTResult<SvProjectVO> get(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        SvProjectVO vo = projectService.get(id, userId, visibleIds);
        RESTResult<SvProjectVO> r = RESTResult.getSuccess(vo);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存项目")
    public RESTResult<Long> save(@RequestBody @Valid SvProjectSaveVO vo, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = projectService.save(vo, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除项目")
    public RESTResult<Void> delete(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        projectService.delete(id, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 每日拍摄脚本 ───────────────────────────────────────

    @PostMapping("/generate-daily")
    @Operation(summary = "AI 生成每日拍摄脚本")
    public RESTResult<java.util.Map<String, Object>> generateDaily(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long personaId = body != null && body.get("personaId") instanceof Number n ? n.longValue() : null;
        String scheduleDate = body != null && body.get("scheduleDate") instanceof String s ? s : null;
        if (personaId == null || !java.util.Objects.requireNonNullElse(scheduleDate, "").matches("\\d{4}-\\d{2}-\\d{2}"))
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "personaId 和 scheduleDate(yyyy-MM-dd) 必填");
        Integer count = body != null && body.get("count") instanceof Number n ? n.intValue() : 1;
        String style = body != null && body.get("style") instanceof String s ? s : null;
        String duration = body != null && body.get("duration") instanceof String s ? s : null;
        String topic = body != null && body.get("topic") instanceof String s ? s : null;
        java.util.Map<String, Object> result = dailyShootService.generateDaily(userId, personaId, scheduleDate, count, style, duration, topic);
        RESTResult<java.util.Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/daily-list")
    @Operation(summary = "每日脚本列表")
    public RESTResult<java.util.List<java.util.Map<String, Object>>> dailyList(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String scheduleDate = body != null && body.get("scheduleDate") instanceof String s ? s : null;
        Long personaId = body != null && body.get("personaId") instanceof Number n ? n.longValue() : null;
        java.util.List<java.util.Map<String, Object>> list = dailyShootService.dailyList(userId, scheduleDate, personaId);
        RESTResult<java.util.List<java.util.Map<String, Object>>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update-shoot-status")
    @Operation(summary = "更新拍摄状态")
    public RESTResult<Void> updateShootStatus(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long projectId = body != null && body.get("projectId") instanceof Number n ? n.longValue() : null;
        String shootStatus = body != null && body.get("shootStatus") instanceof String s ? s : null;
        if (projectId == null || !java.util.Objects.requireNonNullElse(shootStatus, "").matches("not_started|ready|shooting|shot_done"))
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "projectId 和 shootStatus 必填");
        dailyShootService.updateShootStatus(userId, projectId, shootStatus);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/export-script")
    @Operation(summary = "导出拍摄脚本")
    public RESTResult<String> exportScript(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long projectId = body != null && body.get("projectId") instanceof Number n ? n.longValue() : null;
        if (projectId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "projectId 必填");
        String text = dailyShootService.exportScript(userId, projectId);
        RESTResult<String> r = RESTResult.getSuccess(text);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
