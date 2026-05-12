package cn.gaifan.douyinOperations.module.abtest.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.abtest.service.AbTestService;
import cn.gaifan.douyinOperations.module.abtest.service.ScriptStyleAbService;
import cn.gaifan.douyinOperations.module.abtest.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/abtest")
@Tag(name = "A/B 测试 / ABTest", description = "A/B 实验管理（需登录）")
public class AbTestController {

    @Resource
    private AbTestService abTestService;
    @Resource
    private ScriptStyleAbService scriptStyleAbService;
    @Resource
    private DataScopeResolver dataScopeService;

    // ==================== 实验管理 ====================

    @PostMapping("/experiment/list")
    @Operation(summary = "实验列表（分页）")
    public RESTResult<PageResultVO<AbExperimentVO>> list(HttpServletRequest request,
            @RequestBody(required = false) AbExperimentSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new AbExperimentSearchVO();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleIds != null) vo.setOwnerIds(visibleIds);
        RESTResult<PageResultVO<AbExperimentVO>> r = RESTResult.getSuccess(abTestService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/experiment/get")
    @Operation(summary = "实验详情（含变体）")
    public RESTResult<AbExperimentVO> get(HttpServletRequest request, @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<AbExperimentVO> r = RESTResult.getSuccess(abTestService.getById(id, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/experiment/save")
    @Operation(summary = "新增/更新实验（含变体）")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody AbExperimentSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setOwnerId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(abTestService.save(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/experiment/delete")
    @Operation(summary = "删除实验（级联删除变体）")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        abTestService.delete(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/experiment/update-status")
    @Operation(summary = "更新实验状态（0=草稿 1=运行中 2=已完成 3=已暂停）")
    public RESTResult<Void> updateStatus(HttpServletRequest request,
            @RequestParam Long id, @RequestParam Integer status) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        abTestService.updateStatus(id, status, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/experiment/set-winner")
    @Operation(summary = "设置获胜变体（结束实验）")
    public RESTResult<Void> setWinner(HttpServletRequest request, @Valid @RequestBody AbSetWinnerVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        abTestService.setWinner(vo, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 变体管理 ====================

    @PostMapping("/variant/save")
    @Operation(summary = "新增/更新变体")
    public RESTResult<Long> saveVariant(HttpServletRequest request, @Valid @RequestBody AbVariantSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Long> r = RESTResult.addSuccess(abTestService.saveVariant(vo, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/variant/delete")
    @Operation(summary = "删除变体")
    public RESTResult<Void> deleteVariant(HttpServletRequest request, @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        abTestService.deleteVariant(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 事件记录 ====================

    @PostMapping("/event/record")
    @Operation(summary = "记录事件（view/click/conversion，自动去重）")
    public RESTResult<Void> recordEvent(HttpServletRequest request, @Valid @RequestBody AbEventSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        abTestService.recordEvent(vo, userId);
        RESTResult<Void> r = RESTResult.addSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 统计分析 ====================

    @PostMapping("/experiment/result")
    @Operation(summary = "获取实验统计结果（变体数据、卡方检验、日趋势）")
    public RESTResult<AbExperimentStatisticsVO> getExperimentResult(HttpServletRequest request, @RequestParam Long experimentId) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        AbExperimentStatisticsVO result = abTestService.getExperimentStatistics(experimentId);
        RESTResult<AbExperimentStatisticsVO> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/experiment/daily-trend")
    @Operation(summary = "获取实验日趋势数据（可选时间范围）")
    public RESTResult<java.util.List<AbDailyTrendVO>> getDailyTrend(HttpServletRequest request,
            @RequestParam Long experimentId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        java.time.LocalDate start = startDate != null ? java.time.LocalDate.parse(startDate, java.time.format.DateTimeFormatter.ISO_DATE) : java.time.LocalDate.now().minusDays(30);
        java.time.LocalDate end = endDate != null ? java.time.LocalDate.parse(endDate, java.time.format.DateTimeFormatter.ISO_DATE) : java.time.LocalDate.now();

        java.util.List<AbDailyTrendVO> trends = abTestService.getDailyTrend(experimentId, start, end);
        RESTResult<java.util.List<AbDailyTrendVO>> r = RESTResult.getSuccess(trends);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 话术风格 A/B（#28） ====================

    @PostMapping("/script-style/assign")
    @Operation(summary = "分配话术风格（随机 A/B，用于生成时选用风格）")
    public RESTResult<ScriptStyleAssignVO> assignStyle(HttpServletRequest request,
            @RequestBody ScriptStyleAssignRequest requestBody) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (requestBody == null || requestBody.getTargetEntityType() == null || requestBody.getTargetEntityId() == null) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "targetEntityType、targetEntityId 必填");
        }
        String fingerprint = requestBody.getUserFingerprint() != null ? requestBody.getUserFingerprint() : "u:" + userId;
        ScriptStyleAssignVO vo = scriptStyleAbService.assignStyle(
                userId, requestBody.getTargetEntityType(), requestBody.getTargetEntityId(), fingerprint);
        RESTResult<ScriptStyleAssignVO> r = RESTResult.getSuccess(vo);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/script-style/record-conversion")
    @Operation(summary = "记录话术风格实验的转化（用于对比 A/B 转化率）")
    public RESTResult<Void> recordConversion(HttpServletRequest request,
            @RequestBody ScriptStyleConversionRequest requestBody) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (requestBody == null || requestBody.getExperimentId() == null || requestBody.getVariantId() == null) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "experimentId、variantId 必填");
        }
        String fingerprint = requestBody.getUserFingerprint() != null ? requestBody.getUserFingerprint() : "unknown";
        scriptStyleAbService.recordConversion(
                requestBody.getExperimentId(), requestBody.getVariantId(), fingerprint);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
