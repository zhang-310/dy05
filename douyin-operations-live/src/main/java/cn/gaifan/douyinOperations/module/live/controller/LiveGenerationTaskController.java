package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationTask;
import cn.gaifan.douyinOperations.module.live.service.LiveGenerationTaskService;
import cn.gaifan.douyinOperations.module.live.vo.LiveGenerationTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 话术生成任务 Controller
 */
@RestController
@RequestMapping("/api/v1/live/generation-task")
@Tag(name = "话术生成任务 / Generation Task", description = "话术批量生成任务管理（需登录）")
public class LiveGenerationTaskController {

    @Resource
    private LiveGenerationTaskService liveGenerationTaskService;

    @PostMapping("/latest")
    @Operation(summary = "获取场次最新生成任务 / Get latest generation task for session")
    public RESTResult<LiveGenerationTaskVO> latest(HttpServletRequest request,
                                                    @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long sessionId = parseLong(body, "sessionId");
        if (sessionId == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        }
        LiveGenerationTaskVO vo = liveGenerationTaskService.getLatestBySession(sessionId);
        RESTResult<LiveGenerationTaskVO> r = RESTResult.getSuccess(vo);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/create")
    @Operation(summary = "创建生成任务 / Create generation task")
    public RESTResult<LiveGenerationTaskVO> create(HttpServletRequest request,
                                                    @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long sessionId = parseLong(body, "sessionId");
        if (sessionId == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        }
        Integer totalSlots = parseInteger(body, "totalSlots");
        String style = (String) body.get("style");
        Long modelId = parseLong(body, "modelId");
        Boolean useKbRef = (Boolean) body.get("useKbRef");
        @SuppressWarnings("unchecked")
        List<String> hotKeywords = (List<String>) body.get("hotKeywords");

        LiveGenerationTask task = liveGenerationTaskService.createTask(
                sessionId, userId, totalSlots, style, modelId, useKbRef, hotKeywords);
        RESTResult<LiveGenerationTaskVO> r = RESTResult.getSuccess(LiveGenerationTaskVO.fromEntity(task));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update-progress")
    @Operation(summary = "更新生成任务进度 / Update generation task progress")
    public RESTResult<Void> updateProgress(HttpServletRequest request,
                                            @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long taskId = parseLong(body, "taskId");
        if (taskId == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 taskId");
        }
        Integer completedSlots = parseInteger(body, "completedSlots");
        Integer failedSlots = parseInteger(body, "failedSlots");
        liveGenerationTaskService.updateProgress(taskId, completedSlots, failedSlots);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private Long parseLong(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key)) return null;
        Object val = body.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key)) return null;
        Object val = body.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
