package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.script.service.ScriptTemplateService;
import cn.gaifan.douyinOperations.module.script.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端话术模板 Controller（仅系统模板）
 */
@RestController
@RequestMapping("/api/v1/script/admin/template")
@Tag(name = "话术模板管理（Admin）", description = "系统话术模板管理，仅管理员可用")
public class ScriptTemplateAdminController {

    private static final String TEMPLATE_TYPE_SYSTEM = "system";

    @Resource
    private ScriptTemplateService scriptTemplateService;

    @PostMapping("/list")
    @Operation(summary = "系统话术模板列表")
    public RESTResult<PageResultVO<ScriptTemplateVO>> list(HttpServletRequest request,
            @RequestBody(required = false) ScriptTemplateSearchVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new ScriptTemplateSearchVO();
        vo.setTemplateType(TEMPLATE_TYPE_SYSTEM);
        RESTResult<PageResultVO<ScriptTemplateVO>> r = RESTResult.getSuccess(scriptTemplateService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "新增/编辑系统话术模板")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody ScriptTemplateSaveVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        vo.setTemplateType(TEMPLATE_TYPE_SYSTEM);
        vo.setUserId(null);
        long id = scriptTemplateService.saveSystemTemplate(vo);
        RESTResult<Long> result = RESTResult.addSuccess(id);
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除系统话术模板")
    public RESTResult<Void> delete(HttpServletRequest request,
            @Parameter(description = "模板 ID", required = true) @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        scriptTemplateService.deleteSystemTemplate(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
