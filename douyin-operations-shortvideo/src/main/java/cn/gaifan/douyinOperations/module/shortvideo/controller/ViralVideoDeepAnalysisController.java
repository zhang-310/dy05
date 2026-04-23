package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoDeepAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LF-05 爆款深度拆解 HTTP 层：与 {@code front/src/api/viral-analysis.ts} 路径一致。
 */
@RestController
@RequestMapping("/api/v1/short-video/viral")
@Tag(name = "爆款深度拆解 / Viral Deep Analyze", description = "下载→ASR→抽帧→LLM 综合拆解（与 /viral/analyze 触发的服务相同）")
public class ViralVideoDeepAnalysisController {

    @Resource
    private ViralVideoDeepAnalysisService viralVideoDeepAnalysisService;

    private static Long longFromBody(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        Object v = body.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Long> idsFromBody(Map<String, Object> body) {
        if (body == null || body.get("ids") == null) {
            return List.of();
        }
        Object raw = body.get("ids");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Number n) {
                out.add(n.longValue());
            } else if (o != null) {
                try {
                    out.add(Long.parseLong(o.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    @PostMapping("/deep-analyze")
    @Operation(summary = "提交深度拆解（异步）")
    public RESTResult<Map<String, Object>> deepAnalyze(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = longFromBody(body, "id");
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        Map<String, Object> data = viralVideoDeepAnalysisService.startDeepAnalyze(id, userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/deep-analyze/status")
    @Operation(summary = "查询深度拆解状态与结果")
    public RESTResult<Map<String, Object>> deepAnalyzeStatus(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = longFromBody(body, "id");
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        Map<String, Object> data = viralVideoDeepAnalysisService.getDeepAnalyzeStatus(id, userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 与账号采集 {@code /account-collect/status-stream} 一致：路径含 {@code -stream}，便于绕过日志缓冲。
     */
    @PostMapping(value = "/deep-analyze-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "深度拆解 SSE（progress/done/error）")
    public SseEmitter deepAnalyzeStream(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            SseEmitter err = new SseEmitter(1000L);
            err.completeWithError(new IllegalStateException("未登录"));
            return err;
        }
        Long id = longFromBody(body, "id");
        if (id == null) {
            SseEmitter err = new SseEmitter(1000L);
            err.completeWithError(new IllegalArgumentException("缺少 id"));
            return err;
        }
        SseEmitter emitter = new SseEmitter(35L * 60 * 1000L);
        viralVideoDeepAnalysisService.startDeepAnalyzeStream(id, userId, emitter);
        return emitter;
    }

    @PostMapping("/deep-analyze/batch")
    @Operation(summary = "批量提交深度拆解")
    public RESTResult<List<Map<String, Object>>> batchDeepAnalyze(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        List<Long> ids = idsFromBody(body);
        if (ids.isEmpty()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 ids");
        }
        List<Map<String, Object>> data = viralVideoDeepAnalysisService.batchStartDeepAnalyze(ids, userId);
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/deep-analyze/batch/status")
    @Operation(summary = "批量查询深度拆解状态")
    public RESTResult<List<Map<String, Object>>> batchDeepAnalyzeStatus(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        List<Long> ids = idsFromBody(body);
        if (ids.isEmpty()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 ids");
        }
        List<Map<String, Object>> data = viralVideoDeepAnalysisService.batchGetDeepAnalyzeStatus(ids, userId);
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/deep-analyze/retry-round")
    @Operation(summary = "重跑多轮拆解中的某一文本轮次")
    public RESTResult<Map<String, Object>> retryRound(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = longFromBody(body, "id");
        String roundKey = "";
        if (body != null && body.get("round") != null) {
            roundKey = body.get("round").toString().trim();
        }
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        if (!StringUtils.hasText(roundKey)) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 round");
        }
        Map<String, Object> data = viralVideoDeepAnalysisService.retryDeepAnalyzeRound(id, userId, roundKey);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/extract-transcript")
    @Operation(summary = "仅 ASR 提取口播")
    public RESTResult<String> extractTranscript(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = longFromBody(body, "id");
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        String t = viralVideoDeepAnalysisService.extractTranscript(id, userId);
        RESTResult<String> r = RESTResult.getSuccess(t);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/extract-scene-descriptions")
    @Operation(summary = "仅抽帧场景描述")
    public RESTResult<String> extractSceneDescriptions(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = longFromBody(body, "id");
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        String t = viralVideoDeepAnalysisService.extractSceneDescriptions(id, userId);
        RESTResult<String> r = RESTResult.getSuccess(t);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
