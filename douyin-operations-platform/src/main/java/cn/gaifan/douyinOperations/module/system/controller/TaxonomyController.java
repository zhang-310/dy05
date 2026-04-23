package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.constant.RoleCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.system.service.TaxonomyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 全站分类 taxonomy（A-2 Phase0）
 */
@RestController
@RequestMapping("/api/v1/system/taxonomy")
@Tag(name = "系统 / Taxonomy", description = "全站分类字典（登录可读；写删需 admin）")
public class TaxonomyController {

    @Resource
    private TaxonomyService taxonomyService;

    @PostMapping("/list")
    @Operation(summary = "按模块与父节点列出分类")
    public RESTResult<List<Map<String, Object>>> list(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        String moduleScope = body != null && body.get("moduleScope") instanceof String s ? s : null;
        Long parentId = body != null && body.get("parentId") instanceof Number n ? n.longValue() : null;
        if (body != null && !body.containsKey("parentId")) {
            parentId = null;
        }
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(
                taxonomyService.list(userId, moduleScope, parentId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存分类节点（仅 admin，平台 ownerId=0）")
    public RESTResult<Long> save(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        boolean admin = RoleCode.ADMIN.equals(AuthTokenFilter.getRoleCode(request));
        RESTResult<Long> r = RESTResult.getSuccess(taxonomyService.save(userId, admin, body));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除分类节点（仅 admin）")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        boolean admin = RoleCode.ADMIN.equals(AuthTokenFilter.getRoleCode(request));
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        taxonomyService.delete(userId, admin, id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
