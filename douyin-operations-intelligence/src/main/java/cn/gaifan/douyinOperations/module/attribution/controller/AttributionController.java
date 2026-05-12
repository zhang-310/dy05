package cn.gaifan.douyinOperations.module.attribution.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.attribution.service.AttributionService;
import cn.gaifan.douyinOperations.module.attribution.vo.AttributionTriggerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/attribution")
@Tag(name = "AI 效果归因 / AI Attribution", description = "直播效果归因分析（AI 驱动）")
public class AttributionController {

    @Resource
    private AttributionService attributionService;

    @PostMapping("/trigger")
    @Operation(summary = "触发归因分析 / Trigger Attribution Analysis")
    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "attributionTrigger", fallbackMethod = "triggerFallback")
    public RESTResult<Long> trigger(HttpServletRequest request, @Valid @RequestBody AttributionTriggerVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        long id = attributionService.triggerAttribution(vo, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // P1-1: 限流降级方法
    private RESTResult<Long> triggerFallback(HttpServletRequest request,
                                              AttributionTriggerVO vo,
                                              io.github.resilience4j.ratelimiter.RequestNotPermitted ex) {
        return RESTResult.error(ErrorCode.RATE_LIMIT, "归因分析请求过于频繁，请稍后再试");
    }

    @PostMapping("/session")
    @Operation(summary = "获取场次归因数据 / Get Session Attribution")
    public RESTResult<List<Map<String, Object>>> getBySession(HttpServletRequest request,
            @Valid @RequestBody cn.gaifan.douyinOperations.module.attribution.vo.AttributionQueryVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        List<Map<String, Object>> data = attributionService.getBySessionId(vo.getSessionId(), userId);
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/summary")
    @Operation(summary = "获取归因汇总 / Get Attribution Summary")
    public RESTResult<Map<String, Object>> getSummary(HttpServletRequest request,
            @Valid @RequestBody cn.gaifan.douyinOperations.module.attribution.vo.AttributionQueryVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Map<String, Object> data = attributionService.getSummary(vo.getSessionId(), userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取归因详情 / Get Attribution Detail")
    public RESTResult<Map<String, Object>> getById(HttpServletRequest request,
            @Valid @RequestBody cn.gaifan.douyinOperations.module.attribution.vo.AttributionDetailQueryVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Map<String, Object> data = attributionService.getById(vo.getId(), userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除场次归因数据 / Delete Session Attribution")
    public RESTResult<Void> deleteBySession(HttpServletRequest request, @PathVariable Long sessionId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        attributionService.deleteBySessionId(sessionId, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
