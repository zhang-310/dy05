package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.CompetitorMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * C-1：直播中心同源竞品能力 — 复用短视频域 {@link CompetitorMonitorService}。
 */
@RestController
@RequestMapping("/api/v1/live/competitor-monitor")
@Tag(name = "直播 / CompetitorMonitorBridge", description = "与短视频竞品监控同源数据（C-1）")
public class LiveCompetitorMonitorBridgeController {

    @Resource
    private CompetitorMonitorService competitorMonitorService;

    @PostMapping("/list")
    @Operation(summary = "竞品账号列表（同 short-video/competitor/list）")
    public RESTResult<List<Map<String, Object>>> list(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(competitorMonitorService.listCompetitors(userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
