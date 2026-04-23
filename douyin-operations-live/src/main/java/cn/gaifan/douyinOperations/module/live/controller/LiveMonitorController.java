package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveMonitorService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 直播监控数据管理 Controller
 * Live Monitor Data Management Controller
 */
@RestController
@RequestMapping("/api/v1/live/monitor")
@Tag(name = "直播监控 / Live Monitor", description = "直播监控数据的管理（需登录）")
public class LiveMonitorController {

    @Resource
    private LiveMonitorService liveMonitorService;
    @Resource
    private DataScopeResolver dataScopeService;
    @Resource
    private LiveSessionRepository liveSessionRepository;

    @PostMapping("/search")
    @Operation(
            summary = "查询监控数据 / Search Monitor Data",
            description = "分页查询直播监控数据（需登录，非管理员仅能查看自己场次的数据） / Search and paginate live monitor data (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<LiveMonitorVO>> search(HttpServletRequest request,
                                                          @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                  description = "查询条件 / Search criteria",
                                                                  required = false
                                                          )
                                                          @RequestBody(required = false) LiveMonitorSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo == null) vo = new LiveMonitorSearchVO();
        // 数据范围：按角色限制可见场次
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleUserIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleUserIds != null && vo.getSessionId() == null) {
            List<Long> visibleSessionIds = liveSessionRepository.findIdsByUserIdIn(visibleUserIds);
            vo.setSessionIds(visibleSessionIds);
        }
        PageResultVO<LiveMonitorVO> data = liveMonitorService.search(vo);
        RESTResult<PageResultVO<LiveMonitorVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(
            summary = "保存监控数据 / Save Monitor Data",
            description = "创建或更新直播监控数据记录（需登录） / Create or update live monitor data (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "保存成功 / Save successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Long> save(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "监控数据 / Monitor data",
            required = true
    ) @RequestBody LiveMonitorVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        long id = liveMonitorService.save(vo);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/by-session")
    @Operation(
            summary = "获取场次的监控数据 / Get Monitor Data by Session",
            description = "获取指定直播场次的监控数据列表（需登录） / Get monitor data list for specified live session (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "场次不存在 / Session not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<List<LiveMonitorVO>> getBySessionId(HttpServletRequest request,
            @RequestBody java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Object sid = body != null ? body.get("sessionId") : null;
        if (sid == null) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "缺少 sessionId");
        }
        Long sessionId = sid instanceof Number ? ((Number) sid).longValue() : Long.parseLong(sid.toString());
        List<LiveMonitorVO> data = liveMonitorService.getBySessionId(sessionId);
        RESTResult<List<LiveMonitorVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}

