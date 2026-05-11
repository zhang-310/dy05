package cn.gaifan.douyinOperations.module.douyin.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.douyin.entity.DyFanProfileStats;
import cn.gaifan.douyinOperations.module.douyin.service.FanProfileService;
import cn.gaifan.douyinOperations.module.douyin.vo.FanProfileQueryVO;
import cn.gaifan.douyinOperations.module.douyin.vo.FanProfileVO;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "粉丝画像管理")
@RestController
@RequestMapping("/api/v1/douyin/fan-profile")
public class FanProfileController {

    private static final Logger log = LoggerFactory.getLogger(FanProfileController.class);

    @Resource
    private FanProfileService fanProfileService;

    @Resource
    private RateLimiter fanProfileSyncRateLimiter;

    @Operation(summary = "获取粉丝画像")
    @PostMapping("/get")
    public RESTResult<FanProfileVO> getFanProfile(HttpServletRequest request,
            @Valid @RequestBody FanProfileQueryVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        FanProfileVO profile = fanProfileService.getFanProfile(vo.getAccountId(), userId);
        RESTResult<FanProfileVO> r = RESTResult.getSuccess(profile);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @Operation(summary = "获取统计数据")
    @PostMapping("/stats")
    public RESTResult<List<DyFanProfileStats>> getStats(HttpServletRequest request,
            @Valid @RequestBody FanProfileQueryVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        List<DyFanProfileStats> stats = fanProfileService.getStats(vo.getAccountId(), vo.getStatType(), userId);
        RESTResult<List<DyFanProfileStats>> r = RESTResult.getSuccess(stats);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @Operation(summary = "手动同步粉丝画像")
    @PostMapping("/sync/{accountId}")
    public RESTResult<Void> manualSync(
            @PathVariable Long accountId,
            HttpServletRequest request) {
        // P1-3: 限流保护
        try {
            fanProfileSyncRateLimiter.acquirePermission();
        } catch (RequestNotPermitted e) {
            log.warn("粉丝画像同步接口触发限流: userId={}, accountId={}", AuthTokenFilter.getUserId(request), accountId);
            return RESTResult.error(ErrorCode.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
        }

        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        fanProfileService.manualSync(accountId, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
