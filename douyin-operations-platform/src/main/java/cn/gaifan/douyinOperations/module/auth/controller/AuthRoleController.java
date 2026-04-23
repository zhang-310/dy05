package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.auth.service.AuthRoleService;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleResourceSaveVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleSaveVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleSearchVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 角色管理（仅管理员）：列表、详情、保存、删除、角色-资源授权
 * Role Management (Admin Only): List, Get, Save, Delete, Role-Resource Authorization
 */
@RestController
@RequestMapping("/api/v1/auth/role")
@Tag(name = "角色管理 / Role Management", description = "角色和角色-资源权限的管理（仅管理员）")
public class AuthRoleController {

    private static final String ROLE_ADMIN = "admin";

    @Resource
    private AuthRoleService authRoleService;

    private boolean isAdmin(HttpServletRequest request) {
        return ROLE_ADMIN.equals(AuthTokenFilter.getRoleCode(request));
    }

    @PostMapping("/search")
    @Operation(
            summary = "查询角色列表 / Search Roles",
            description = "分页查询系统中的所有角色（仅管理员） / Search and paginate all roles (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<AuthRoleVO>> search(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "查询条件 / Search criteria",
            required = false
    ) @RequestBody(required = false) AuthRoleSearchVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        if (vo == null) vo = new AuthRoleSearchVO();
        PageResultVO<AuthRoleVO> data = authRoleService.search(vo);
        RESTResult<PageResultVO<AuthRoleVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/list")
    @Operation(
            summary = "获取所有角色 / Get All Roles",
            description = "获取系统中的所有角色（不分页）（仅管理员） / Get all roles without pagination (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<List<AuthRoleVO>> listAll(HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        List<AuthRoleVO> data = authRoleService.listAll();
        RESTResult<List<AuthRoleVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(
            summary = "获取角色详情 / Get Role Details",
            description = "根据角色 ID 获取角色详细信息（仅管理员） / Get role details by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "404", description = "角色不存在 / Role not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<AuthRoleVO> get(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "角色 ID / Role ID",
            required = true
    ) @RequestBody java.util.Map<String, Long> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long id = body != null ? body.get("id") : null;
        AuthRoleVO data = authRoleService.getById(id);
        RESTResult<AuthRoleVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(
            summary = "保存角色 / Save Role",
            description = "创建或更新角色（仅管理员） / Create or update role (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "保存成功 / Save successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "角色信息 / Role information",
            required = true
    ) @RequestBody AuthRoleSaveVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        try {
            long id = authRoleService.save(vo);
            RESTResult<Long> r = RESTResult.addSuccess(id);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (IllegalArgumentException e) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, e.getMessage());
        }
    }

    @PostMapping("/delete")
    @Operation(
            summary = "删除角色 / Delete Role",
            description = "根据角色 ID 删除角色（仅管理员） / Delete role by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "删除成功 / Delete successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "404", description = "角色不存在 / Role not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> delete(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "角色 ID / Role ID",
            required = true
    ) @RequestBody java.util.Map<String, Long> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long id = body != null ? body.get("id") : null;
        authRoleService.deleteById(id);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/resources")
    @Operation(
            summary = "获取角色的资源权限 / Get Role Resources",
            description = "获取指定角色拥有的资源权限 ID 列表（仅管理员） / Get resource IDs of specified role (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<List<Long>> getResources(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "角色 ID / Role ID",
            required = true
    ) @RequestBody java.util.Map<String, Long> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long roleId = body != null ? body.get("roleId") : null;
        List<Long> data = authRoleService.getResourceIdsByRoleId(roleId);
        RESTResult<List<Long>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/resources/save")
    @Operation(
            summary = "授予角色资源权限 / Grant Role Resources",
            description = "为指定角色授予资源权限（仅管理员） / Grant resource permissions to role (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "授权成功 / Grant successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> saveResources(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "角色及资源权限 / Role and resource permissions",
            required = true
    ) @RequestBody AuthRoleResourceSaveVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        authRoleService.saveRoleResources(vo.getRoleId(), vo.getResourceIds());
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
