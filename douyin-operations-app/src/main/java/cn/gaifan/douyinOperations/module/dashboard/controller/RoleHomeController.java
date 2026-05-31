package cn.gaifan.douyinOperations.module.dashboard.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.constant.RoleCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.dashboard.service.RoleHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Role-specific home aggregation endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Role Home / 角色首页", description = "角色后台首页聚合数据")
public class RoleHomeController {

    @Resource
    private RoleHomeService roleHomeService;

    @PostMapping("/admin/home")
    @Operation(summary = "管理员首页聚合")
    public RESTResult<Map<String, Object>> adminHome(HttpServletRequest request) {
        return home("admin", RoleCode.ADMIN, request);
    }

    @PostMapping("/org/home")
    @Operation(summary = "机构首页聚合")
    public RESTResult<Map<String, Object>> orgHome(HttpServletRequest request) {
        return home("org", RoleCode.INSTITUTION, request);
    }

    @PostMapping("/talent/home")
    @Operation(summary = "达人首页聚合")
    public RESTResult<Map<String, Object>> talentHome(HttpServletRequest request) {
        return home("talent", RoleCode.TALENT, request);
    }

    @PostMapping("/user/home")
    @Operation(summary = "个人用户首页聚合")
    public RESTResult<Map<String, Object>> userHome(HttpServletRequest request) {
        return home("user", RoleCode.USER, request);
    }

    private RESTResult<Map<String, Object>> home(String role, String requiredRoleCode, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return withTraceId(RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录"));
        }
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (!requiredRoleCode.equals(roleCode)) {
            return withTraceId(RESTResult.error(ErrorCode.FORBIDDEN, "当前角色不能访问该首页"));
        }
        Long organizationId = AuthTokenFilter.getOrganizationId(request);
        return withTraceId(RESTResult.getSuccess(roleHomeService.buildHome(role, userId, roleCode, organizationId)));
    }

    private static <T> RESTResult<T> withTraceId(RESTResult<T> result) {
        result.setTraceId(MDC.get("traceId"));
        return result;
    }
}
