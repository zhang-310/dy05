package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.VideoGenerationTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 图生视频异步任务 API (Phase 2.2)
 * 路径：/api/v1/short-video/video-task
 */
@RestController
@RequestMapping("/api/v1/short-video/video-task")
@Tag(name = "图生视频异步任务", description = "提交任务、查询状态、取消、重试")
public class VideoGenerationTaskController {

    @Resource
    private VideoGenerationTaskService taskService;

    @PostMapping("/list")
    @Operation(summary = "任务分页列表")
    public RESTResult<PageResultVO<Map<String, Object>>> list(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        int page = body != null && body.get("page") instanceof Number n ? n.intValue() : 0;
        int rows = body != null && body.get("rows") instanceof Number n ? n.intValue() : 20;
        Long projectId = body != null && body.get("projectId") instanceof Number n ? n.longValue() : null;
        if (projectId != null && projectId <= 0) projectId = null;
        PageResultVO<Map<String, Object>> data = taskService.listTasks(userId, page, rows, projectId);
        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/submit")
    @Operation(summary = "提交异步任务")
    public RESTResult<Map<String, Object>> submit(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long shotListId = body.get("shotListId") instanceof Number n ? n.longValue() : null;
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        String quality = body.get("quality") instanceof String s ? s : null;
        String aspectRatio = body.get("aspectRatio") instanceof String s ? s : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyframes = (List<Map<String, Object>>) body.get("keyframes");
        if (keyframes == null || keyframes.isEmpty()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "keyframes 不能为空");

        Long taskId = taskService.submitTask(userId, projectId, shotListId, keyframes, quality, aspectRatio);
        RESTResult<Map<String, Object>> r = RESTResult.addSuccess(Map.of("taskId", taskId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/status")
    @Operation(summary = "查询任务状态")
    public RESTResult<VideoGenerationTaskService.TaskStatusVO> status(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long taskId = body.get("taskId") instanceof Number n ? n.longValue() : null;
        if (taskId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");

        VideoGenerationTaskService.TaskStatusVO status = taskService.getStatus(taskId, userId);
        RESTResult<VideoGenerationTaskService.TaskStatusVO> r = RESTResult.getSuccess(status);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消任务")
    public RESTResult<Void> cancel(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long taskId = body.get("taskId") instanceof Number n ? n.longValue() : null;
        if (taskId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");

        taskService.cancelTask(taskId, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/retry")
    @Operation(summary = "重试失败任务")
    public RESTResult<Map<String, Object>> retry(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long taskId = body.get("taskId") instanceof Number n ? n.longValue() : null;
        if (taskId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");

        Long newTaskId = taskService.retryTask(taskId, userId);
        RESTResult<Map<String, Object>> r = RESTResult.addSuccess(Map.of("taskId", newTaskId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
