package cn.gaifan.douyinOperations.module.copy.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.copy.service.CopyTemplateService;
import cn.gaifan.douyinOperations.module.copy.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/copy/template")
@Tag(name = "文案模板 / Copy Template", description = "文案模板管理（需登录）")
public class CopyTemplateController {

    @Resource
    private CopyTemplateService copyTemplateService;

    @PostMapping("/search")
    @Operation(summary = "分页搜索模板")
    public RESTResult<PageResultVO<CopyTemplateVO>> search(HttpServletRequest request,
            @RequestBody(required = false) CopyTemplateSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new CopyTemplateSearchVO();
        // P1-2: 数据隔离 - 强制设置 userId，防止绕过
        vo.setUserId(userId);
        RESTResult<PageResultVO<CopyTemplateVO>> r = RESTResult.getSuccess(copyTemplateService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取模板详情")
    public RESTResult<CopyTemplateVO> get(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<CopyTemplateVO> r = RESTResult.getSuccess(copyTemplateService.getById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "新建/更新模板")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody CopyTemplateSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setUserId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(copyTemplateService.save(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除模板")
    public RESTResult<Void> delete(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        // P1-4: IDOR 防护 - 传入 userId 校验所有权
        copyTemplateService.delete(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update-status")
    @Operation(summary = "更新模板状态（admin）")
    public RESTResult<Void> updateStatus(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        Integer status = body != null && body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        if (id == null || status == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id 或 status");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        // P1-4: IDOR 防护 - 传入 userId 校验所有权
        copyTemplateService.updateStatus(id, status, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseLong(java.util.Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }
}
