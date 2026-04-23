package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptPipeline;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 一键全自动流水线 Controller
 * Generate → QC → Auto-Refine → Save
 */
@RestController
@RequestMapping("/api/v1/live/pipeline")
@Tag(name = "话术流水线 / Script Pipeline", description = "一键全自动生成流水线（需登录）")
public class LiveScriptPipelineController {

    @Resource
    private LiveScriptPipelineService pipelineService;

    @PostMapping("/start")
    @Operation(summary = "启动流水线 / Start Pipeline",
            description = "启动一键全自动流水线：Generate → QC → Auto-Refine → Save")
    public RESTResult<LiveScriptPipeline> start(HttpServletRequest request,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        Long userId = requireUserId(request);
        if (body == null) body = Map.of();

        Long sessionId = parseLong(body.get("sessionId"));
        if (sessionId == null) {
            return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId"));
        }

        Long modelId = parseLong(body.get("modelId"));
        String style = body.get("style") != null ? body.get("style").toString() : null;
        Boolean useKbRef = body.get("useKbRef") != null ? Boolean.valueOf(body.get("useKbRef").toString()) : null;

        LiveScriptPipeline pipeline = pipelineService.startPipeline(sessionId, modelId, style, useKbRef, userId);
        return withTraceId(RESTResult.success(pipeline));
    }

    @PostMapping("/status")
    @Operation(summary = "查询流水线状态 / Get Pipeline Status")
    public RESTResult<LiveScriptPipeline> status(HttpServletRequest request,
                                                  @RequestBody(required = false) Map<String, Object> body) {
        Long userId = requireUserId(request);
        Long pipelineId = parseId(body);
        if (pipelineId == null) {
            return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id"));
        }

        LiveScriptPipeline pipeline = pipelineService.getPipelineStatus(pipelineId, userId);
        return withTraceId(RESTResult.getSuccess(pipeline));
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消流水线 / Cancel Pipeline")
    public RESTResult<Void> cancel(HttpServletRequest request,
                                   @RequestBody(required = false) Map<String, Object> body) {
        Long userId = requireUserId(request);
        Long pipelineId = parseId(body);
        if (pipelineId == null) {
            return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id"));
        }

        pipelineService.cancelPipeline(pipelineId, userId);
        return withTraceId(RESTResult.success());
    }

    // ==================== 工具方法 ====================

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseId(Map<String, Object> body) {
        if (body == null) return null;
        return parseLong(body.get("id"));
    }

    private static Long parseLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
