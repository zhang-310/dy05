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

import java.util.List;

/**
 * 话术模板管理 Controller
 */
@RestController
@RequestMapping("/api/v1/script/template")
@Tag(name = "话术模板 / Script Template", description = "话术模板管理（需登录）")
public class ScriptTemplateController {

    @Resource
    private ScriptTemplateService scriptTemplateService;

    @PostMapping("/search")
    @Operation(summary = "查询话术模板 / Search Script Templates")
    public RESTResult<PageResultVO<ScriptTemplateVO>> search(HttpServletRequest request,
                                                              @RequestBody(required = false) ScriptTemplateSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new ScriptTemplateSearchVO();
        PageResultVO<ScriptTemplateVO> data = scriptTemplateService.search(vo);
        RESTResult<PageResultVO<ScriptTemplateVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取模板详情 / Get Template Details")
    public RESTResult<ScriptTemplateVO> get(HttpServletRequest request,
                                             @Parameter(description = "模板 ID", required = true) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        ScriptTemplateVO data = scriptTemplateService.getById(id);
        RESTResult<ScriptTemplateVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存话术模板 / Save Script Template")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody ScriptTemplateSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        long id = scriptTemplateService.save(vo);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除话术模板 / Delete Script Template")
    public RESTResult<Void> delete(HttpServletRequest request,
                                    @Parameter(description = "模板 ID", required = true) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        scriptTemplateService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/use-count")
    @Operation(summary = "增加模板使用次数 / Increment Template Use Count")
    public RESTResult<Void> incrementUseCount(HttpServletRequest request,
                                               @Parameter(description = "模板 ID", required = true) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        scriptTemplateService.incrementUseCount(id);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/by-scene")
    @Operation(summary = "按场景获取模板列表 / List Templates by Scene")
    public RESTResult<List<ScriptTemplateVO>> listByScene(HttpServletRequest request,
                                                           @RequestBody(required = false) java.util.Map<String, String> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String scene = body != null ? body.get("scene") : null;
        if (scene == null || scene.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 scene");
        List<ScriptTemplateVO> data = scriptTemplateService.listByScene(scene);
        RESTResult<List<ScriptTemplateVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
