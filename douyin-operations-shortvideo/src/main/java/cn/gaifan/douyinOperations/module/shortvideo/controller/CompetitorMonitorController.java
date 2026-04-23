package cn.gaifan.douyinOperations.module.shortvideo.controller;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "竞品监控 / Competitor Monitor")
@RestController
@RequestMapping("/api/v1/short-video/competitor")
public class CompetitorMonitorController {

    @Resource
    private CompetitorMonitorService competitorMonitorService;

    @PostMapping("/add")
    @Operation(summary = "添加竞品账号")
    public RESTResult<String> add(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        competitorMonitorService.addCompetitor(userId, body.get("accountId"), body.get("accountName"), body.getOrDefault("platform", "douyin"));
        RESTResult<String> r = RESTResult.getSuccess("添加成功");
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/list")
    @Operation(summary = "竞品列表")
    public RESTResult<List<Map<String, Object>>> list(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(competitorMonitorService.listCompetitors(userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/analyze")
    @Operation(summary = "分析竞品")
    public RESTResult<Map<String, Object>> analyze(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(competitorMonitorService.analyzeCompetitor(userId, body.get("competitorId")));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/weekly-report")
    @Operation(summary = "生成周报")
    public RESTResult<String> weeklyReport(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<String> r = RESTResult.getSuccess(competitorMonitorService.generateWeeklyReport(userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 删除竞品账号（补齐 CompetitorMonitorService.removeCompetitor）
     * POST /api/v1/short-video/competitor/remove
     * body: { competitorId }
     */
    @PostMapping("/remove")
    @Operation(summary = "删除竞品账号")
    public RESTResult<Void> remove(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Long competitorId = body.get("competitorId");
        if (competitorId == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "competitorId 不能为空");
        competitorMonitorService.removeCompetitor(userId, competitorId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
