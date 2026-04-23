package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvContentCalendarService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/short-video/content-calendar")
@Tag(name = "内容日历计划", description = "LF-04 sv_content_calendar CRUD")
public class SvContentCalendarController {

    @Resource
    private SvContentCalendarService calendarService;

    @PostMapping("/list")
    @Operation(summary = "分页列表")
    public RESTResult<PageResultVO<SvContentCalendarVO>> list(@RequestBody(required = false) SvContentCalendarSearchVO searchVO,
            @CurrentUserId Long userId) {
        if (searchVO == null) searchVO = new SvContentCalendarSearchVO();
        PageResultVO<SvContentCalendarVO> data = calendarService.list(searchVO, userId);
        RESTResult<PageResultVO<SvContentCalendarVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "详情")
    public RESTResult<SvContentCalendarVO> get(@RequestBody Map<String, Long> body, @CurrentUserId Long userId) {
        Long id = body != null ? body.get("id") : null;
        if (id == null) {
            return RESTResult.validError(cn.gaifan.douyinOperations.common.constant.ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        SvContentCalendarVO data = calendarService.get(id, userId);
        RESTResult<SvContentCalendarVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存")
    public RESTResult<Long> save(@Valid @RequestBody SvContentCalendarSaveVO saveVO, @CurrentUserId Long userId) {
        Long id = calendarService.save(saveVO, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除")
    public RESTResult<Void> delete(@RequestBody Map<String, Long> body, @CurrentUserId Long userId) {
        Long id = body != null ? body.get("id") : null;
        if (id == null) {
            return RESTResult.validError(cn.gaifan.douyinOperations.common.constant.ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        calendarService.delete(id, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/date-range")
    @Operation(summary = "按日期范围")
    public RESTResult<List<SvContentCalendarVO>> dateRange(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        String from = body != null && body.get("from") instanceof String s ? s : null;
        String to = body != null && body.get("to") instanceof String s ? s : null;
        Long personaId = body != null && body.get("personaId") instanceof Number n ? n.longValue() : null;
        if (from == null || to == null) {
            return RESTResult.validError(ErrorCode.VALIDATION_FAIL, "缺少 from/to");
        }
        List<SvContentCalendarVO> data = calendarService.listByDateRange(from, to, personaId, userId);
        RESTResult<List<SvContentCalendarVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/auto-generate")
    @Operation(summary = "AI/规则自动排期")
    public RESTResult<Integer> autoGenerate(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long personaId = body != null && body.get("personaId") instanceof Number n ? n.longValue() : null;
        String from = body != null && body.get("from") instanceof String s ? s : null;
        String to = body != null && body.get("to") instanceof String s ? s : null;
        if (personaId == null || from == null || to == null) {
            return RESTResult.validError(ErrorCode.VALIDATION_FAIL, "缺少 personaId/from/to");
        }
        int n = calendarService.autoGenerate(personaId, from, to, userId);
        RESTResult<Integer> r = RESTResult.getSuccess(n);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
