package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.WorkflowExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Short-video workflow runtime APIs.
 */
@RestController
@RequestMapping("/api/v1/short-video/workflow")
@Tag(name = "短视频工作流执行", description = "执行短视频异步生产流水线")
public class ShortVideoWorkflowController {

    @Resource
    private WorkflowExecutionService workflowExecutionService;

    @PostMapping("/digital-human-commerce/start")
    @Operation(summary = "启动数字人口播带货一键成片")
    public RESTResult<Map<String, Object>> startDigitalHumanCommerce(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long projectId = readLong(body.get("projectId"));
        if (projectId == null || projectId <= 0) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "projectId 必填");
        }
        String taskId = workflowExecutionService.executeDigitalHumanCommercePipeline(projectId, body, userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("projectId", projectId);
        data.put("status", "processing");
        data.put("currentStep", "pipeline:init");
        data.put("progress", 5);
        RESTResult<Map<String, Object>> result = RESTResult.getSuccess(data);
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    @PostMapping("/status")
    @Operation(summary = "查询工作流执行状态")
    public RESTResult<Map<String, Object>> status(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        String taskId = body.get("taskId") instanceof String s ? s.trim() : "";
        if (taskId.isBlank()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "taskId 必填");
        }
        RESTResult<Map<String, Object>> result = RESTResult.getSuccess(workflowExecutionService.getStatus(taskId));
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    private static Long readLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
