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
    public RESTResult<Long> trigger(HttpServletRequest request, @Valid @RequestBody AttributionTriggerVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        long id = attributionService.triggerAttribution(vo, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/session")
    @Operation(summary = "获取场次归因数据 / Get Session Attribution")
    public RESTResult<List<Map<String, Object>>> getBySession(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body != null ? body.get("sessionId") : null;
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        List<Map<String, Object>> data = attributionService.getBySessionId(sessionId);
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/summary")
    @Operation(summary = "获取归因汇总 / Get Attribution Summary")
    public RESTResult<Map<String, Object>> getSummary(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body != null ? body.get("sessionId") : null;
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        Map<String, Object> data = attributionService.getSummary(sessionId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取归因详情 / Get Attribution Detail")
    public RESTResult<Map<String, Object>> getById(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null ? body.get("id") : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Map<String, Object> data = attributionService.getById(id);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除场次归因数据 / Delete Session Attribution")
    public RESTResult<Void> deleteBySession(HttpServletRequest request, @PathVariable Long sessionId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        attributionService.deleteBySessionId(sessionId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
