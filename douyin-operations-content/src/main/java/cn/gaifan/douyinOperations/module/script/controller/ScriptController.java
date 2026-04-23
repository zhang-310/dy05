package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.script.service.ScriptLibraryService;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/script")
@Tag(name = "话术库 / Script Library", description = "话术库管理（需登录）")
public class ScriptController {

    @Resource
    private ScriptLibraryService scriptLibraryService;
    @Resource
    private ViolationWordService violationWordService;
    @Resource
    private DataScopeResolver dataScopeService;

    @PostMapping("/list")
    @Operation(summary = "话术列表（分页搜索）")
    public RESTResult<PageResultVO<ScriptVO>> list(HttpServletRequest request,
            @RequestBody(required = false) ScriptSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new ScriptSearchVO();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleIds != null) vo.setUserIds(visibleIds);
        RESTResult<PageResultVO<ScriptVO>> r = RESTResult.getSuccess(scriptLibraryService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "话术详情")
    public RESTResult<ScriptVO> get(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<ScriptVO> r = RESTResult.getSuccess(scriptLibraryService.getById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "新增/编辑话术")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody ScriptSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setUserId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(scriptLibraryService.save(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除话术")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        scriptLibraryService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/use-count")
    @Operation(summary = "递增使用次数")
    public RESTResult<Void> useCount(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        scriptLibraryService.incrementUseCount(id);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/categories")
    @Operation(summary = "获取所有话术分类列表")
    public RESTResult<List<String>> categories(HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        List<String> categories = scriptLibraryService.listCategories();
        RESTResult<List<String>> r = RESTResult.getSuccess(categories);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 违规检测 ====================

    @PostMapping("/violation/check-batch")
    @Operation(summary = "批量文本违规检测")
    public RESTResult<ViolationCheckBatchResultVO> checkBatch(HttpServletRequest request,
            @RequestBody(required = false) ViolationCheckBatchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null || vo.getTexts() == null || vo.getTexts().isEmpty()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "待检测文本列表不能为空");
        }
        RESTResult<ViolationCheckBatchResultVO> r = RESTResult.getSuccess(violationWordService.checkBatch(vo, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/violation/check")
    @Operation(summary = "文本违规检测")
    public RESTResult<ViolationCheckResultVO> check(HttpServletRequest request,
            @RequestBody(required = false) ViolationCheckVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null || vo.getText() == null || vo.getText().isBlank()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "检测文本不能为空");
        }
        String scope = vo.getScope() != null ? vo.getScope() : "all";
        RESTResult<ViolationCheckResultVO> r = RESTResult.getSuccess(violationWordService.check(vo.getText(), scope, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/violation/public/list")
    @Operation(summary = "公共违规词列表（分页）")
    public RESTResult<PageResultVO<ViolationWordVO>> publicList(HttpServletRequest request,
            @RequestBody(required = false) ViolationWordSearchVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new ViolationWordSearchVO();
        vo.setStatus(1);
        RESTResult<PageResultVO<ViolationWordVO>> r = RESTResult.getSuccess(violationWordService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/violation/suggest-replacement")
    @Operation(summary = "AI 推荐违规词替换建议")
    public RESTResult<ViolationReplacementResultVO> suggestReplacement(HttpServletRequest request,
            @Valid @RequestBody ViolationReplacementRequestVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<ViolationReplacementResultVO> r = RESTResult.getSuccess(violationWordService.suggestReplacement(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
