package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.PublishFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 发布后数据反馈 API
 * 路径：/api/v1/short-video/feedback
 */
@RestController
@RequestMapping("/api/v1/short-video/feedback")
@Tag(name = "短视频反馈", description = "发布效果分析、反思报告、周报")
public class ShortVideoFeedbackController {

    @Resource
    private PublishFeedbackService publishFeedbackService;
    @Resource
    private DataScopeResolver dataScopeService;

    @PostMapping("/analyze-performance")
    @Operation(summary = "分析视频发布效果")
    public RESTResult<Map<String, Object>> analyzePerformance(@RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long videoId = body != null && body.get("videoId") instanceof Number n ? n.longValue() : null;
        if (videoId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videoId 必填");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        var score = publishFeedbackService.analyzePerformance(videoId, visibleIds);
        Map<String, Object> data = Map.of(
                "overallScore", score.overallScore(),
                "completionRate", score.completionRate(),
                "engagementRate", score.engagementRate(),
                "viewsVsAvg", score.viewsVsAvg(),
                "performance", score.performance() != null ? score.performance() : "",
                "insights", score.insights() != null ? score.insights() : List.of()
        );
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/reflection-report")
    @Operation(summary = "生成视频反思报告")
    public RESTResult<Map<String, Object>> reflectionReport(@RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long videoId = body != null && body.get("videoId") instanceof Number n ? n.longValue() : null;
        if (videoId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videoId 必填");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        Map<String, Object> data = publishFeedbackService.generateReflectionReport(videoId, visibleIds);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/weekly-report")
    @Operation(summary = "生成本周发布周报")
    public RESTResult<Map<String, Object>> weeklyReport(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Map<String, Object> data = publishFeedbackService.generateWeeklyReport(userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
