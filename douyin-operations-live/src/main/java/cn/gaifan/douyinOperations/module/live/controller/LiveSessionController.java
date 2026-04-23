package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionService;
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
import java.util.Map;

/**
 * 直播场次管理 Controller
 * Live Session Management Controller
 */
@RestController
@RequestMapping("/api/v1/live/session")
@Tag(name = "直播场次 / Live Session", description = "直播场次的管理（需登录）")
public class LiveSessionController {

    @Resource
    private LiveSessionService liveSessionService;
    @Resource
    private DataScopeResolver dataScopeService;

    @PostMapping("/search")
    @Operation(
            summary = "查询直播场次 / Search Live Sessions",
            description = "分页查询直播场次（需登录） / Search and paginate live sessions (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<LiveSessionVO>> search(HttpServletRequest request,
                                                          @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                  description = "查询条件 / Search criteria",
                                                                  required = false
                                                          )
                                                          @RequestBody(required = false) LiveSessionSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo == null) vo = new LiveSessionSearchVO();
        // 数据范围：按角色限制可见用户
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleIds != null) {
            vo.setUserIds(visibleIds);
        }
        PageResultVO<LiveSessionVO> data = liveSessionService.search(vo);
        RESTResult<PageResultVO<LiveSessionVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(
            summary = "获取直播场次详情 / Get Live Session Details",
            description = "根据场次 ID 获取直播场次详细信息（需登录） / Get live session details by ID (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "场次不存在 / Session not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<LiveSessionVO> get(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        LiveSessionVO data = liveSessionService.getByIdWithScope(id, visibleIds);
        RESTResult<LiveSessionVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(
            summary = "保存直播场次 / Save Live Session",
            description = "创建或更新直播场次信息（需登录） / Create or update live session (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "保存成功 / Save successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "场次信息 / Session information",
            required = true
    ) @RequestBody LiveSessionSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo.getUserId() == null) {
            vo.setUserId(userId);
        }
        long id = liveSessionService.save(vo);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(
            summary = "删除直播场次 / Delete Live Session",
            description = "根据场次 ID 删除直播场次（需登录） / Delete live session by ID (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "删除成功 / Delete successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "场次不存在 / Session not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> delete(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        liveSessionService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/overview")
    @Operation(summary = "场次数据概览 / Get Session Overview")
    public RESTResult<LiveSessionOverviewVO> getOverview(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        liveSessionService.getByIdWithScope(id, visibleIds); // 校验数据范围
        LiveSessionOverviewVO data = liveSessionService.getOverview(id);
        RESTResult<LiveSessionOverviewVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/readiness")
    @Operation(
            summary = "开播准备清单 / Get Readiness Check",
            description = "获取场次开播准备清单（产品/人设/话术/合规）（需登录） / Get live session readiness check")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "场次不存在"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public RESTResult<LiveReadinessVO> getReadiness(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body, "id");
        if (id == null) id = parseId(body, "sessionId");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id 或 sessionId");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        LiveReadinessVO data = liveSessionService.getReadiness(id);
        RESTResult<LiveReadinessVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/status")
    @Operation(
            summary = "更新直播场次状态 / Update Session Status",
            description = "更新直播场次的状态（含状态机校验：preparing→live→ended）（需登录） / Update live session status with state machine validation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "更新成功 / Update successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "场次不存在 / Session not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> updateStatus(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Integer status = body != null && body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        if (status == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 status");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        liveSessionService.updateStatus(id, status);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/viewers")
    @Operation(
            summary = "更新观看人数 / Update Viewer Count",
            description = "更新直播场次的观看人数（需登录） / Update live session viewer count (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "更新成功 / Update successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "场次不存在 / Session not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> updateViewers(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Integer viewers = body != null && body.get("viewers") != null ? ((Number) body.get("viewers")).intValue() : null;
        if (viewers == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 viewers");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        liveSessionService.updateViewers(id, viewers);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/likes")
    @Operation(
            summary = "更新点赞数 / Update Like Count",
            description = "更新直播场次的点赞数（需登录） / Update live session like count (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "更新成功 / Update successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "场次不存在 / Session not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> updateLikes(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long likes = body != null && body.get("likes") != null ? ((Number) body.get("likes")).longValue() : null;
        if (likes == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 likes");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        liveSessionService.updateLikes(id, likes);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseId(java.util.Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }

    @PostMapping("/trend")
    @Operation(
            summary = "直播历史趋势分析 / Live Trend Analysis",
            description = "分析直播历史数据趋势（需登录） / Analyze live session historical trends (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "分析成功 / Analysis successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "无数据 / No data found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<LiveTrendResultVO> analyzeTrend(HttpServletRequest request,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "趋势分析请求参数 / Trend analysis request",
                    required = true
            )
            @Valid @RequestBody LiveTrendRequestVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        RESTResult<LiveTrendResultVO> r = RESTResult.getSuccess(liveSessionService.analyzeTrend(vo, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 克隆直播场次（含商品和话术）
     */
    @PostMapping("/clone")
    @Operation(summary = "克隆场次 / Clone Session")
    public RESTResult<Long> clone(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body.get("sessionId") != null ? Long.parseLong(body.get("sessionId").toString()) : null;
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "sessionId 不能为空");
        String newTitle = body.get("newTitle") != null ? body.get("newTitle").toString() : null;
        long newId = liveSessionService.cloneSession(sessionId, userId, newTitle);
        RESTResult<Long> r = RESTResult.addSuccess(newId);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 导出场次到短视频项目
     */
    @PostMapping("/export-to-short-video")
    @Operation(summary = "场次导出短视频 / Export to Short Video")
    public RESTResult<Long> exportToShortVideo(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body.get("sessionId") != null ? Long.parseLong(body.get("sessionId").toString()) : null;
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "sessionId 不能为空");
        long projectId = liveSessionService.exportToShortVideo(sessionId, userId);
        RESTResult<Long> r = RESTResult.addSuccess(projectId);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
