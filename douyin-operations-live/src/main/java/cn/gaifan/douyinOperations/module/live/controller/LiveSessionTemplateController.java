package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionTemplateService;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 直播场次槽位模板（M-1/M-2）
 */
@RestController
@RequestMapping("/api/v1/live/session-template")
@Tag(name = "直播场次模板", description = "自定义槽位结构 JSON + 场次 template_id 引用")
public class LiveSessionTemplateController {

    @Resource
    private LiveSessionTemplateService templateService;

    @PostMapping("/search")
    @Operation(summary = "分页查询模板（含 owner_id=0 系统预置）")
    public RESTResult<PageResultVO<LiveSessionTemplateVO>> search(HttpServletRequest request,
            @RequestBody(required = false) LiveSessionTemplateSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo == null) {
            vo = new LiveSessionTemplateSearchVO();
        }
        PageResultVO<LiveSessionTemplateVO> data = templateService.search(vo, userId);
        RESTResult<PageResultVO<LiveSessionTemplateVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取模板详情")
    public RESTResult<LiveSessionTemplateVO> get(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        RESTResult<LiveSessionTemplateVO> r = RESTResult.getSuccess(templateService.getById(id, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存模板（系统预置不可改删）")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody LiveSessionTemplateSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        long id = templateService.save(vo, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除模板")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        templateService.delete(id, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
