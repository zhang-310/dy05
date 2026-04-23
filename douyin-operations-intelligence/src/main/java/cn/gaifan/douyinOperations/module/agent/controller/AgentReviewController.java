package cn.gaifan.douyinOperations.module.agent.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.agent.service.AgentReviewService;
import cn.gaifan.douyinOperations.module.agent.vo.AgentReviewSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * 智能体评论管理
 */
@RestController
@RequestMapping("/api/v1/agent/review")
@Tag(name = "智能体评论 / Agent Review", description = "智能体评分与评论")
public class AgentReviewController {

    @Resource
    private AgentReviewService reviewService;

    @PostMapping("/stats")
    @Operation(summary = "获取智能体评分统计")
    public RESTResult<Map<String, Object>> getRatingStats(@RequestBody Map<String, Object> body) {
        Long agentId = toLong(body.get("agentId"));
        if (agentId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "agentId不能为空");
        return RESTResult.getSuccess(reviewService.getRatingStats(agentId));
    }

    @PostMapping("/list")
    @Operation(summary = "获取智能体评论列表")
    public RESTResult<PageResultVO<AgentReviewVO>> listReviews(@RequestBody Map<String, Object> body) {
        Long agentId = toLong(body.get("agentId"));
        Integer page = body.get("page") != null ? toInt(body.get("page")) : 0;
        Integer rows = body.get("rows") != null ? toInt(body.get("rows")) : 10;
        if (agentId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "agentId不能为空");
        return RESTResult.getSuccess(reviewService.listReviews(agentId, page, rows));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交或更新评论")
    public RESTResult<Long> submitReview(HttpServletRequest request, @Valid @RequestBody AgentReviewSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Long> r = RESTResult.getSuccess(reviewService.submitReview(userId, vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/my")
    @Operation(summary = "获取当前用户对某智能体的评论")
    public RESTResult<AgentReviewVO> getMyReview(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long agentId = toLong(body.get("agentId"));
        if (agentId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "agentId不能为空");
        return RESTResult.getSuccess(reviewService.getUserReview(agentId, userId));
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Integer) return (Integer) v;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }
}
