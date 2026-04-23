package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.constant.RoleCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.CinematicGenerationLogReportService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SceneCameraMappingAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * H-1：运镜知识库运营 API（场景映射 CRUD、生成日志汇总）
 */
@RestController
@RequestMapping("/api/v1/short-video/cinematic")
@Tag(name = "短视频 / CinematicOps", description = "运镜映射运营与生成日志统计")
public class CinematicOpsController {

    @Resource
    private SceneCameraMappingAdminService sceneCameraMappingAdminService;
    @Resource
    private CinematicGenerationLogReportService cinematicGenerationLogReportService;

    @PostMapping("/scene-camera-mapping/list")
    @Operation(summary = "场景-运镜映射列表（只读，登录即可）")
    public RESTResult<List<Map<String, Object>>> mappingList(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(sceneCameraMappingAdminService.listAll());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/scene-camera-mapping/save")
    @Operation(summary = "保存场景-运镜映射（仅 admin）")
    public RESTResult<Long> mappingSave(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!RoleCode.ADMIN.equals(AuthTokenFilter.getRoleCode(request))) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "仅管理员可维护映射");
        }
        RESTResult<Long> r = RESTResult.getSuccess(sceneCameraMappingAdminService.save(body));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/scene-camera-mapping/delete")
    @Operation(summary = "删除场景-运镜映射（仅 admin）")
    public RESTResult<Void> mappingDelete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!RoleCode.ADMIN.equals(AuthTokenFilter.getRoleCode(request))) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "仅管理员可维护映射");
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        sceneCameraMappingAdminService.delete(id);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generation-log/summary")
    @Operation(summary = "生成日志汇总（按当前用户 project 隔离；admin 可查 body.global=1 看全库）")
    public RESTResult<Map<String, Object>> generationLogSummary(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        ZoneId z = ZoneId.systemDefault();
        long startMs;
        long endMs;
        if (body != null && body.get("yearMonth") instanceof String ym && !ym.isBlank()) {
            YearMonth yM = YearMonth.parse(ym.trim());
            LocalDate first = yM.atDay(1);
            LocalDate last = yM.atEndOfMonth();
            startMs = first.atStartOfDay(z).toInstant().toEpochMilli();
            endMs = last.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli();
        } else if (body != null && body.get("from") instanceof String from && body.get("to") instanceof String to
                && !from.isBlank() && !to.isBlank()) {
            LocalDate d0 = LocalDate.parse(from.trim());
            LocalDate d1 = LocalDate.parse(to.trim());
            startMs = d0.atStartOfDay(z).toInstant().toEpochMilli();
            endMs = d1.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli();
        } else {
            YearMonth cur = YearMonth.now();
            LocalDate first = cur.atDay(1);
            LocalDate last = cur.atEndOfMonth();
            startMs = first.atStartOfDay(z).toInstant().toEpochMilli();
            endMs = last.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli();
        }

        boolean global = Boolean.TRUE.equals(body != null ? body.get("global") : null)
                && RoleCode.ADMIN.equals(AuthTokenFilter.getRoleCode(request));
        Long ownerFilter = global ? null : userId;
        Map<String, Object> data = cinematicGenerationLogReportService.summarize(ownerFilter, startMs, endMs);
        data.put("startMs", startMs);
        data.put("endMs", endMs);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
