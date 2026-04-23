package cn.gaifan.douyinOperations.module.copy.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.copy.service.CopyLibraryService;
import cn.gaifan.douyinOperations.module.copy.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/copy/library")
@Tag(name = "文案库 / Copy Library", description = "文案库管理（需登录）")
public class CopyLibraryController {

    @Resource
    private CopyLibraryService copyLibraryService;
    @Resource
    private DataScopeResolver dataScopeService;

    @PostMapping("/search")
    @Operation(summary = "分页搜索文案库")
    public RESTResult<PageResultVO<CopyLibraryVO>> search(HttpServletRequest request,
            @RequestBody(required = false) CopyLibrarySearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new CopyLibrarySearchVO();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleIds != null) vo.setUserIds(visibleIds);
        RESTResult<PageResultVO<CopyLibraryVO>> r = RESTResult.getSuccess(copyLibraryService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取文案详情")
    public RESTResult<CopyLibraryVO> get(HttpServletRequest request,
            @Parameter(description = "文案 ID") @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<CopyLibraryVO> r = RESTResult.getSuccess(copyLibraryService.getById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "新建/更新文案")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody CopyLibrarySaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setUserId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(copyLibraryService.save(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除文案")
    public RESTResult<Void> delete(HttpServletRequest request,
            @Parameter(description = "文案 ID") @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        copyLibraryService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update-status")
    @Operation(summary = "更新文案状态（admin）")
    public RESTResult<Void> updateStatus(HttpServletRequest request,
            @RequestParam Long id, @RequestParam Integer status) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        copyLibraryService.updateStatus(id, status);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/increment-use-count")
    @Operation(summary = "递增使用次数")
    public RESTResult<Void> incrementUseCount(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        copyLibraryService.incrementUseCount(id);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
