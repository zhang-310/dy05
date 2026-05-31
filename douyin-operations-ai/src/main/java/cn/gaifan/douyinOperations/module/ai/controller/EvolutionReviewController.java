package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/evolution-review")
@Tag(name = "进化审核管理")
public class EvolutionReviewController {

    @Resource
    private EvolutionReviewService reviewService;

    @PostMapping("/list")
    @Operation(summary = "审核任务列表")
    public RESTResult<PageResultVO<Map<String, Object>>> list(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        String status = resolveReviewListStatus(body);
        int page = body != null && body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int rows = body != null && body.get("rows") != null ? ((Number) body.get("rows")).intValue() : 20;

        var result = reviewService.listReviewTasks(status, page, rows);
        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/approve")
    @Operation(summary = "通过审核")
    public RESTResult<Void> approve(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        Long taskId = extractReviewTaskId(body);
        String comment = (String) body.get("comment");

        if (taskId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "taskId 不能为空");

        reviewService.approve(taskId, userId, comment);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/reject")
    @Operation(summary = "拒绝审核")
    public RESTResult<Void> reject(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        Long taskId = extractReviewTaskId(body);
        String comment = body.get("reason") instanceof String r ? r : (String) body.get("comment");

        if (taskId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "taskId 不能为空");

        reviewService.reject(taskId, userId, comment);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/revise")
    @Operation(summary = "修订后通过")
    public RESTResult<Void> revise(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        Long taskId = extractReviewTaskId(body);
        String revisedContent = (String) body.get("revisedContent");
        String comment = (String) body.get("comment");

        if (taskId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "taskId 不能为空");
        if (revisedContent == null || revisedContent.isBlank()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "修订内容不能为空");
        }

        reviewService.revise(taskId, userId, revisedContent, comment);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/stats")
    @Operation(summary = "审核统计")
    public RESTResult<Map<String, Object>> stats(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        Map<String, Object> stats = reviewService.getReviewStats();
        stats.putIfAbsent("pending", stats.get("pendingCount"));
        stats.putIfAbsent("approved", stats.get("approvedCount"));
        stats.putIfAbsent("rejected", stats.get("rejectedCount"));
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(stats);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static String resolveReviewListStatus(Map<String, Object> body) {
        if (body == null || body.get("status") == null) {
            return null;
        }
        Object s = body.get("status");
        if (s instanceof String str && !str.isBlank()) {
            return str;
        }
        if (s instanceof Number n) {
            return switch (n.intValue()) {
                case 0 -> "PENDING";
                case 1 -> "APPROVED";
                case 2 -> "REJECTED";
                default -> null;
            };
        }
        return null;
    }

    private static Long extractReviewTaskId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        if (body.get("taskId") instanceof Number n) {
            return n.longValue();
        }
        if (body.get("id") instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
