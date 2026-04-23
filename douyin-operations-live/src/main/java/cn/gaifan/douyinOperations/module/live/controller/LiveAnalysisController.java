package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.live.service.LiveAnalysisService;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAnalysisVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

/**
 * 直播 AI 分析 Controller
 */
@RestController
@RequestMapping("/api/v1/live/analysis")
@Tag(name = "直播 AI 分析 / Live AI Analysis", description = "AI 复盘报告生成与获取（需登录）")
public class LiveAnalysisController {

    @Resource
    private LiveAnalysisService liveAnalysisService;
    @Resource
    private LiveSessionService liveSessionService;
    @Resource
    private DataScopeResolver dataScopeService;

    @Resource
    private AiQuotaService aiQuotaService;

    @Resource
    private AiCallLogService aiCallLogService;

    @PostMapping("/generate")
    @Operation(summary = "生成 AI 复盘报告 / Generate AI Analysis")
    public RESTResult<LiveAnalysisVO> generate(HttpServletRequest request,
                                                @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long sessionId = parseSessionId(body);
        if (sessionId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        Long userId = requireUserId(request);
        aiQuotaService.ensureQuota(userId);
        long start = System.currentTimeMillis();
        try {
            LiveAnalysisVO data = liveAnalysisService.generate(sessionId);
            aiQuotaService.consume(userId);
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_analysis", null, "llm", null, null, null, null, System.currentTimeMillis() - start, 1, null, false, null, null));
            return withTraceId(RESTResult.getSuccess(data));
        } catch (Exception e) {
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_analysis", null, "llm", null, null, null, null, System.currentTimeMillis() - start, 0, e.getMessage(), false, null, null));
            throw e;
        }
    }

    @PostMapping("/get")
    @Operation(summary = "获取 AI 分析报告 / Get AI Analysis")
    public RESTResult<LiveAnalysisVO> get(HttpServletRequest request,
                                          @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long sessionId = parseSessionId(body);
        if (sessionId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        Long userId = requireUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        liveSessionService.getByIdWithScope(sessionId, visibleIds); // 校验数据范围
        LiveAnalysisVO data = liveAnalysisService.get(sessionId);
        return withTraceId(RESTResult.getSuccess(data));
    }

    @PostMapping("/review")
    @Operation(summary = "获取 AI 复盘报告 / Get AI Live Review")
    public RESTResult<LiveReviewVO> getReview(HttpServletRequest request,
                                               @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long sessionId = parseSessionId(body);
        if (sessionId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        Long userId = requireUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        liveSessionService.getByIdWithScope(sessionId, visibleIds); // 校验数据范围
        LiveReviewVO data = liveAnalysisService.getReview(sessionId);
        return withTraceId(RESTResult.getSuccess(data));
    }

    private static Long parseSessionId(java.util.Map<String, Object> body) {
        if (body == null) return null;
        Object sid = body.get("sessionId");
        if (sid == null) return null;
        return sid instanceof Number ? ((Number) sid).longValue() : Long.parseLong(sid.toString());
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
