package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptTemplateService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
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
 * 短视频脚本模板 Controller
 */
@RestController
@RequestMapping("/api/v1/short-video/script-template")
@Tag(name = "短视频脚本模板 / SV Script Template", description = "短视频脚本模板管理（需登录）")
public class SvScriptTemplateController {

    @Resource
    private SvScriptTemplateService svScriptTemplateService;

    @PostMapping("/search")
    @Operation(summary = "查询脚本模板 / Search Script Templates")
    public RESTResult<PageResultVO<SvScriptTemplateVO>> search(HttpServletRequest request,
                                                                @RequestBody(required = false) SvScriptTemplateSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new SvScriptTemplateSearchVO();
        PageResultVO<SvScriptTemplateVO> data = svScriptTemplateService.search(vo, userId);
        RESTResult<PageResultVO<SvScriptTemplateVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取模板详情 / Get Template Details")
    public RESTResult<SvScriptTemplateVO> get(HttpServletRequest request,
                                               @Parameter(description = "模板 ID", required = true) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        SvScriptTemplateVO data = svScriptTemplateService.getById(id, userId);
        RESTResult<SvScriptTemplateVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存脚本模板 / Save Script Template")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody SvScriptTemplateSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getOwnerId() == null) vo.setOwnerId(userId);
        long id = svScriptTemplateService.save(vo, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除脚本模板 / Delete Script Template")
    public RESTResult<Void> delete(HttpServletRequest request,
                                    @Parameter(description = "模板 ID", required = true) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        svScriptTemplateService.delete(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/use-count")
    @Operation(summary = "增加使用次数 / Increment Use Count")
    public RESTResult<Void> incrementUseCount(HttpServletRequest request,
                                               @Parameter(description = "模板 ID", required = true) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        svScriptTemplateService.incrementUseCount(id, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/by-scene")
    @Operation(summary = "按场景获取模板 / List Templates by Scene")
    public RESTResult<List<SvScriptTemplateVO>> listByScene(HttpServletRequest request,
                                                             @RequestBody(required = false) java.util.Map<String, String> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String scene = body != null ? body.get("scene") : null;
        if (scene == null || scene.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 scene");
        List<SvScriptTemplateVO> data = svScriptTemplateService.listByScene(scene, userId);
        RESTResult<List<SvScriptTemplateVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
