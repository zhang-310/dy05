package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.DanmakuAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "弹幕分析 / Danmaku Analysis")
@RestController
@RequestMapping("/api/v1/live/danmaku")
public class DanmakuAnalysisController {

    @Resource
    private DanmakuAnalysisService danmakuAnalysisService;

    @PostMapping("/analyze")
    @Operation(summary = "分析弹幕意图")
    public RESTResult<Map<String, Object>> analyze(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body.get("sessionId") != null ? Long.valueOf(body.get("sessionId").toString()) : null;
        if (sessionId == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "sessionId 必填");
        @SuppressWarnings("unchecked")
        List<String> texts = (List<String>) body.getOrDefault("texts", List.of());
        Map<String, Object> result = danmakuAnalysisService.analyzeIntents(sessionId, texts);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/suggest")
    @Operation(summary = "根据弹幕意图推荐话术调整")
    public RESTResult<List<String>> suggest(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body.get("sessionId") != null ? Long.valueOf(body.get("sessionId").toString()) : null;
        if (sessionId == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "sessionId 必填");
        @SuppressWarnings("unchecked")
        Map<String, Object> intents = (Map<String, Object>) body.getOrDefault("intents", Map.of());
        List<String> suggestions = danmakuAnalysisService.suggestScriptAdjustments(sessionId, intents);
        RESTResult<List<String>> r = RESTResult.getSuccess(suggestions);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
