package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveRhythmOptimizer;
import cn.gaifan.douyinOperations.module.live.service.ProductStrategyRecommender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "直播节奏优化 / Live Rhythm")
@RestController
@RequestMapping("/api/v1/live/rhythm")
public class LiveRhythmController {

    private static final Logger log = LoggerFactory.getLogger(LiveRhythmController.class);

    @Resource
    private LiveRhythmOptimizer liveRhythmOptimizer;

    @Resource
    private ProductStrategyRecommender productStrategyRecommender;

    @Resource
    private LiveScriptRepository liveScriptRepository;

    @PostMapping("/optimize")
    @Operation(summary = "优化直播节奏")
    public RESTResult<Map<String, Object>> optimize(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body.get("sessionId") != null ? Long.valueOf(body.get("sessionId").toString()) : null;
        if (sessionId == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "sessionId 不能为空");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(liveRhythmOptimizer.optimizeSchedule(userId, sessionId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/product-strategy")
    @Operation(summary = "商品讲解策略推荐")
    public RESTResult<Map<String, Object>> productStrategy(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Long productId = body.get("productId") != null ? Long.valueOf(body.get("productId").toString()) : null;
        if (productId == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "productId 不能为空");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(productStrategyRecommender.recommend(userId, productId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/batch-order")
    @Operation(summary = "排品顺序推荐")
    public RESTResult<Map<String, Object>> batchOrder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body.get("sessionId") != null ? Long.valueOf(body.get("sessionId").toString()) : null;
        if (sessionId == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "sessionId 不能为空");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(productStrategyRecommender.recommendBatchOrder(userId, sessionId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save-rhythm")
    @Operation(summary = "保存节奏方案（P1-02 可视化节奏编排器）",
            description = "将前端可视化节奏编排器的排期保存到场次话术槽位（更新 sequence_no 和 duration_limit_sec）")
    public RESTResult<Map<String, Object>> saveRhythm(@RequestBody Map<String, Object> body,
                                                       HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body.get("sessionId") != null ? Long.valueOf(body.get("sessionId").toString()) : null;
        if (sessionId == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "sessionId 不能为空");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slots = (List<Map<String, Object>>) body.get("slots");
        if (slots == null || slots.isEmpty()) {
            return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "slots 不能为空"));
        }
        int updated = 0;
        for (Map<String, Object> slot : slots) {
            Object scriptIdObj = slot.get("scriptId");
            Object seqNoObj = slot.get("sequenceNo");
            Object durationObj = slot.get("durationLimitSec");
            if (scriptIdObj == null) continue;
            Long scriptId = scriptIdObj instanceof Number n ? n.longValue() : Long.parseLong(scriptIdObj.toString());
            liveScriptRepository.findById(scriptId).ifPresent(script -> {
                if (!sessionId.equals(script.getSessionId())) return;
                if (seqNoObj instanceof Number n) script.setSequenceNo(n.intValue());
                if (durationObj instanceof Number n) script.setDurationLimitSec(n.intValue());
                liveScriptRepository.save(script);
            });
            updated++;
        }
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of(
                "sessionId", sessionId, "updatedSlots", updated, "status", "ok"));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
