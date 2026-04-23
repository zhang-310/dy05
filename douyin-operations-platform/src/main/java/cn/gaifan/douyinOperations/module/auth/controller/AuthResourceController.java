package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.auth.service.AuthResourceService;
import cn.gaifan.douyinOperations.module.auth.vo.AuthResourceSaveVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthResourceSearchVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthResourceVO;
import cn.gaifan.douyinOperations.module.auth.vo.MenuItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * 资源管理（仅管理员）：列表、菜单树、详情、保存、删除
 * Resource Management (Admin Only): List, Menu Tree, Get, Save, Delete
 */
@RestController
@RequestMapping("/api/v1/auth/resource")
@Tag(name = "资源管理 / Resource Management", description = "权限资源的管理（仅管理员）")
public class AuthResourceController {

    private static final String ROLE_ADMIN = "admin";

    @Resource
    private AuthResourceService authResourceService;

    private boolean isAdmin(HttpServletRequest request) {
        return ROLE_ADMIN.equals(AuthTokenFilter.getRoleCode(request));
    }

    /** 管理端分页列表（与 /auth/resource/search 当前用户资源编码 区分） */
    @PostMapping("/list")
    @Operation(
            summary = "查询资源列表 / Search Resources",
            description = "分页查询系统中的所有权限资源（仅管理员） / Search and paginate all permission resources (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<AuthResourceVO>> list(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "查询条件 / Search criteria",
            required = false
    ) @RequestBody(required = false) AuthResourceSearchVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        if (vo == null) vo = new AuthResourceSearchVO();
        PageResultVO<AuthResourceVO> data = authResourceService.search(vo);
        RESTResult<PageResultVO<AuthResourceVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/tree")
    @Operation(
            summary = "获取资源树结构 / Get Resource Tree",
            description = "获取资源的树形结构用于菜单展示（仅管理员） / Get resource tree structure for menu display (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<List<MenuItemVO>> tree(HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        List<MenuItemVO> data = authResourceService.listMenuTree();
        RESTResult<List<MenuItemVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/tree-full")
    @Operation(
            summary = "获取完整资源树 / Get Full Resource Tree",
            description = "获取所有类型资源的树形结构，用于角色资源分配（仅管理员）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public RESTResult<List<MenuItemVO>> treeFull(HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        List<MenuItemVO> data = authResourceService.listResourceTree();
        RESTResult<List<MenuItemVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(
            summary = "获取资源详情 / Get Resource Details",
            description = "根据资源 ID 获取资源详细信息（仅管理员） / Get resource details by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "404", description = "资源不存在 / Resource not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<AuthResourceVO> get(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "资源 ID / Resource ID",
            required = true
    ) @RequestBody java.util.Map<String, Long> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long id = body != null ? body.get("id") : null;
        AuthResourceVO data = authResourceService.getById(id);
        RESTResult<AuthResourceVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(
            summary = "保存资源 / Save Resource",
            description = "创建或更新权限资源（仅管理员） / Create or update permission resource (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "保存成功 / Save successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "资源信息 / Resource information",
            required = true
    ) @RequestBody AuthResourceSaveVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        long id = authResourceService.save(vo);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(
            summary = "删除资源 / Delete Resource",
            description = "根据资源 ID 删除权限资源（仅管理员） / Delete permission resource by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "删除成功 / Delete successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "404", description = "资源不存在 / Resource not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> delete(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "资源 ID / Resource ID",
            required = true
    ) @RequestBody java.util.Map<String, Long> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long id = body != null ? body.get("id") : null;
        authResourceService.deleteById(id);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
