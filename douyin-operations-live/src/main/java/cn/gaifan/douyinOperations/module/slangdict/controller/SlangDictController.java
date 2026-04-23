package cn.gaifan.douyinOperations.module.slangdict.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.slangdict.service.SlangDictService;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntrySearchVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntrySaveVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/slangdict/entry")
@Tag(name = "话术梗库 / SlangDict", description = "产品暗语/梗管理（需登录）")
public class SlangDictController {

    @Resource private SlangDictService slangDictService;
    @Resource private DataScopeResolver dataScopeService;

    @PostMapping("/search")
    @Operation(summary = "分页搜索梗条目")
    public RESTResult<PageResultVO<SdEntryVO>> search(HttpServletRequest request,
            @RequestBody(required = false) SdEntrySearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new SdEntrySearchVO();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleIds != null) vo.setUserIds(visibleIds);
        else vo.setUserId(userId);
        RESTResult<PageResultVO<SdEntryVO>> r = RESTResult.getSuccess(slangDictService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取梗条目详情")
    public RESTResult<SdEntryVO> get(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<SdEntryVO> r = RESTResult.getSuccess(slangDictService.getById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "新增/更新梗条目")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody SdEntrySaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Long> r = RESTResult.addSuccess(slangDictService.save(vo, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除梗条目")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        slangDictService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/by-product")
    @Operation(summary = "按产品ID查关联梗")
    public RESTResult<List<SdEntryVO>> byProduct(HttpServletRequest request, @RequestParam Long productId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<List<SdEntryVO>> r = RESTResult.getSuccess(slangDictService.getByProductId(productId, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/bind-product")
    @Operation(summary = "绑定梗到产品")
    public RESTResult<Void> bindProduct(HttpServletRequest request,
            @RequestParam Long entryId, @RequestParam Long productId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        slangDictService.bindProduct(entryId, productId, userId);
        RESTResult<Void> r = RESTResult.addSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/unbind-product")
    @Operation(summary = "解绑梗与产品")
    public RESTResult<Void> unbindProduct(HttpServletRequest request,
            @RequestParam Long entryId, @RequestParam Long productId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        slangDictService.unbindProduct(entryId, productId, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/ai-generate")
    @Operation(summary = "AI 为产品生成候选梗")
    public RESTResult<List<String>> aiGenerate(HttpServletRequest request,
            @RequestParam Long productId, @RequestParam(defaultValue = "5") int count) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<List<String>> r = RESTResult.getSuccess(slangDictService.aiGeneratePhrases(productId, userId, count));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
