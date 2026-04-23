package cn.gaifan.douyinOperations.module.douyin.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.douyin.entity.DyFanProfileStats;
import cn.gaifan.douyinOperations.module.douyin.service.FanProfileService;
import cn.gaifan.douyinOperations.module.douyin.vo.FanProfileQueryVO;
import cn.gaifan.douyinOperations.module.douyin.vo.FanProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "粉丝画像管理")
@RestController
@RequestMapping("/api/v1/douyin/fan-profile")
public class FanProfileController {

    @Resource
    private FanProfileService fanProfileService;

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
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        fanProfileService.manualSync(accountId, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
