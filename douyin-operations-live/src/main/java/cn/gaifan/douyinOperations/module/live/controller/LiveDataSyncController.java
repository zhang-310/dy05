package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.DouyinLiveDataSyncService;
import cn.gaifan.douyinOperations.module.live.service.LiveDataSyncService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 直播数据同步 Controller
 */
@RestController
@RequestMapping("/api/v1/live/data")
@Tag(name = "直播数据同步 / Live Data Sync", description = "直播场次和商品数据汇总（需登录）")
public class LiveDataSyncController {

    @Resource
    private LiveDataSyncService liveDataSyncService;
    @Resource
    private DouyinLiveDataSyncService douyinLiveDataSyncService;

    @PostMapping("/session")
    @Operation(summary = "获取场次汇总数据 / Get Session Data")
    public RESTResult<LiveSessionDataVO> getSessionData(HttpServletRequest request,
                                                         @RequestBody(required = false) java.util.Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body != null ? body.get("sessionId") : null;
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        LiveSessionDataVO data = liveDataSyncService.getSessionData(sessionId);
        RESTResult<LiveSessionDataVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/session/with-compare")
    @Operation(summary = "获取场次数据含环比 / Get Session Data with Previous for Compare")
    public RESTResult<SessionDataWithCompareVO> getSessionDataWithCompare(HttpServletRequest request,
                                                                          @RequestBody(required = false) java.util.Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body != null ? body.get("sessionId") : null;
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        SessionDataWithCompareVO data = liveDataSyncService.getSessionDataWithCompare(sessionId);
        RESTResult<SessionDataWithCompareVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/session/save")
    @Operation(summary = "保存场次汇总数据 / Save Session Data")
    public RESTResult<LiveSessionDataVO> saveSessionData(HttpServletRequest request,
                                                          @Valid @RequestBody LiveSessionDataSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        LiveSessionDataVO data = liveDataSyncService.saveSessionData(vo);
        RESTResult<LiveSessionDataVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/session/sync")
    @Operation(summary = "从监控数据同步场次汇总 / Sync Session Data from Monitor")
    public RESTResult<LiveSessionDataVO> syncSessionData(HttpServletRequest request,
                                                          @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Object sid = body != null ? body.get("sessionId") : null;
        if (sid == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        Long sessionId = sid instanceof Number ? ((Number) sid).longValue() : Long.parseLong(sid.toString());
        LiveSessionDataVO data = liveDataSyncService.syncSessionData(sessionId);
        RESTResult<LiveSessionDataVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/session/sync-from-douyin")
    @Operation(summary = "从抖音 API 同步直播数据 / Sync from Douyin Open Platform API")
    public RESTResult<LiveSessionDataVO> syncFromDouyin(HttpServletRequest request,
                                                         @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (body == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId、roomId、accessToken");
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;
        String roomId = body.get("roomId") != null ? body.get("roomId").toString() : null;
        String accessToken = body.get("accessToken") != null ? body.get("accessToken").toString() : null;
        if (sessionId == null || roomId == null || roomId.isBlank() || accessToken == null || accessToken.isBlank()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId、roomId 或 accessToken");
        }
        LiveSessionDataVO data = douyinLiveDataSyncService.syncSessionDataFromDouyin(sessionId, roomId, accessToken);
        if (data == null) {
            return RESTResult.error(ErrorCode.LIVE_DATA_SYNC_FAIL, "抖音 API 同步失败，请检查 roomId 和 accessToken");
        }
        douyinLiveDataSyncService.syncProductDataFromDouyin(sessionId, roomId, accessToken);
        RESTResult<LiveSessionDataVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/product")
    @Operation(summary = "获取场次商品数据列表 / Get Product Data by Session")
    public RESTResult<List<LiveProductDataVO>> getProductData(HttpServletRequest request,
                                                               @RequestBody(required = false) java.util.Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body != null ? body.get("sessionId") : null;
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        List<LiveProductDataVO> data = liveDataSyncService.getProductDataBySession(sessionId);
        RESTResult<List<LiveProductDataVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/history")
    @Operation(summary = "历史数据对比 / Get History for Compare")
    public RESTResult<List<LiveHistoryItemVO>> getHistory(HttpServletRequest request,
                                                          @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long accountId = body != null && body.get("accountId") != null ? ((Number) body.get("accountId")).longValue() : null;
        Integer limit = body != null && body.get("limit") != null ? ((Number) body.get("limit")).intValue() : 10;
        Integer days = body != null && body.get("days") != null ? ((Number) body.get("days")).intValue() : null;
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<LiveHistoryItemVO> data = liveDataSyncService.getHistory(userId, roleCode, accountId, limit, days);
        RESTResult<List<LiveHistoryItemVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/product/save")
    @Operation(summary = "保存商品数据 / Save Product Data")
    public RESTResult<LiveProductDataVO> saveProductData(HttpServletRequest request,
                                                          @Valid @RequestBody LiveProductDataSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        LiveProductDataVO data = liveDataSyncService.saveProductData(vo);
        RESTResult<LiveProductDataVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
