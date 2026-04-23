package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveAbTestAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A/B 测试闭环反馈分析 Controller
 */
@RestController
@RequestMapping("/api/v1/live/ab-analysis")
@Tag(name = "A/B 测试分析 / AB Test Analysis", description = "A/B 测试闭环反馈（需登录）")
public class LiveAbTestAnalysisController {

    @Resource
    private LiveAbTestAnalysisService abTestAnalysisService;

    @PostMapping("/record")
    @Operation(summary = "记录 A/B 测试结果 / Record AB Test Result")
    public RESTResult<Void> record(HttpServletRequest request,
                                   @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(request);

        String experimentKey = getStr(body, "experimentKey");
        if (experimentKey == null || experimentKey.isBlank()) {
            return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 experimentKey"));
        }

        Long sessionId = parseLong(body.get("sessionId"));
        if (sessionId == null) {
            return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId"));
        }

        abTestAnalysisService.recordResult(
                sessionId,
                experimentKey,
                getStr(body, "variant"),
                getStr(body, "style"),
                getDecimal(body, "effectivenessScore"),
                getDecimal(body, "conversionRate"),
                getDecimal(body, "interactionRate"),
                getInt(body, "sampleSize"),
                getDecimal(body, "confidence"),
                userId
        );

        return withTraceId(RESTResult.success());
    }

    @PostMapping("/recommend")
    @Operation(summary = "获取推荐话术风格 / Get Recommended Style",
            description = "根据历史 A/B 测试数据推荐最优话术风格")
    public RESTResult<Map<String, Object>> recommend(HttpServletRequest request) {
        Long userId = requireUserId(request);
        Map<String, Object> result = abTestAnalysisService.getRecommendedStyle(userId);
        return withTraceId(RESTResult.getSuccess(result));
    }

    @PostMapping("/summary")
    @Operation(summary = "获取实验摘要 / Get Experiment Summary")
    public RESTResult<List<Map<String, Object>>> summary(HttpServletRequest request,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        requireUserId(request);
        String experimentKey = body != null ? getStr(body, "experimentKey") : null;
        if (experimentKey == null || experimentKey.isBlank()) {
            return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 experimentKey"));
        }

        List<Map<String, Object>> summary = abTestAnalysisService.getExperimentSummary(experimentKey);
        return withTraceId(RESTResult.getSuccess(summary));
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

    private static String getStr(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString() : null;
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

    private static Integer getInt(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal getDecimal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
