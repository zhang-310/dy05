package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.auth.service.AuthUserService;
import cn.gaifan.douyinOperations.module.auth.vo.AuthUserGetVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthUserSearchVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthUserVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthUserSaveVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthUserBanVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginLogQueryVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginLogVO;
import cn.gaifan.douyinOperations.module.auth.vo.OnlineUserVO;
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
 * 用户管理（仅管理员）：列表、详情、保存、封禁、登录记录
 * User Management (Admin Only): List, Get, Save, Ban, Login Logs
 */
@RestController
@RequestMapping("/api/v1/auth/user")
@Tag(name = "用户管理 / User Management", description = "用户列表、详情、保存、禁用、登录日志、在线用户（仅管理员）")
public class AuthUserController {

    private static final String ROLE_ADMIN = "admin";

    @Resource
    private AuthUserService authUserService;

    private boolean isAdmin(HttpServletRequest request) {
        return ROLE_ADMIN.equals(AuthTokenFilter.getRoleCode(request));
    }

    @PostMapping("/search")
    @Operation(
            summary = "查询用户列表 / Search Users",
            description = "分页查询系统中的所有用户（仅管理员） / Search and paginate all users in system (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<AuthUserVO>> search(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "查询条件 / Search criteria",
            required = false
    ) @RequestBody(required = false) AuthUserSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        if (vo == null) vo = new AuthUserSearchVO();
        Long orgId = AuthTokenFilter.getOrganizationId(request);
        PageResultVO<AuthUserVO> data = authUserService.search(vo, orgId, isAdmin(request));
        RESTResult<PageResultVO<AuthUserVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(
            summary = "获取用户详情 / Get User Details",
            description = "根据用户 ID 获取用户详细信息（仅管理员） / Get user details by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "404", description = "用户不存在 / User not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<AuthUserVO> get(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "用户 ID / User ID",
            required = true
    ) @RequestBody AuthUserGetVO vo) {
        Long currentUserId = AuthTokenFilter.getUserId(request);
        if (currentUserId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long orgId = AuthTokenFilter.getOrganizationId(request);
        AuthUserVO data = authUserService.getById(vo.getId(), orgId, isAdmin(request));
        RESTResult<AuthUserVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(
            summary = "保存用户 / Save User",
            description = "创建或更新用户信息（仅管理员） / Create or update user information (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "保存成功 / Save successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "用户信息 / User information",
            required = true
    ) @RequestBody AuthUserSaveVO vo) {
        Long currentUserId = AuthTokenFilter.getUserId(request);
        if (currentUserId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long orgId = AuthTokenFilter.getOrganizationId(request);
        long id = authUserService.save(vo, orgId, isAdmin(request));
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/ban")
    @Operation(
            summary = "禁用/启用用户 / Ban/Unban User",
            description = "禁用或启用用户账号（仅管理员） / Ban or unban user account (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "禁用/启用成功 / Ban/Unban successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> ban(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "封禁/解封参数 / Ban/Unban parameters",
            required = true
    ) @RequestBody AuthUserBanVO vo) {
        Long currentUserId = AuthTokenFilter.getUserId(request);
        if (currentUserId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        authUserService.ban(vo.getUserId(), vo.getBan(), vo.getReason(),
                AuthTokenFilter.getOrganizationId(request), isAdmin(request));
        RESTResult<Void> r = RESTResult.success(vo.getBan() ? "封禁成功" : "解封成功", null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(
            summary = "删除用户 / Delete User",
            description = "逻辑删除用户（仅管理员） / Soft delete user (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "删除成功 / Delete successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> delete(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "用户 ID / User ID",
            required = true
    ) @RequestBody java.util.Map<String, Long> body) {
        Long currentUserId = AuthTokenFilter.getUserId(request);
        if (currentUserId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long id = body != null ? body.get("id") : null;
        authUserService.deleteById(id, AuthTokenFilter.getOrganizationId(request), isAdmin(request));
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/login-logs")
    @Operation(
            summary = "获取登录日志 / Get Login Logs",
            description = "管理员可查全部或指定用户；普通用户仅能查自己的 / Admin: all or by userId; User: own only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<LoginLogVO>> loginLogs(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "查询条件 / Query criteria",
            required = false
    ) @RequestBody(required = false) LoginLogQueryVO vo) {
        Long currentUserId = AuthTokenFilter.getUserId(request);
        if (currentUserId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        LoginLogQueryVO query = vo != null ? vo : new LoginLogQueryVO();
        if (isAdmin(request)) {
            if (query.getUserId() != null && query.getUserId() <= 0) {
                query.setUserId(null);
            }
        } else {
            query.setUserId(currentUserId);
        }
        PageResultVO<LoginLogVO> data = authUserService.getLoginLogs(query);
        RESTResult<PageResultVO<LoginLogVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/online")
    @Operation(
            summary = "获取在线用户列表 / Get Online Users",
            description = "获取指定时间内的在线用户列表（仅管理员） / Get list of online users within specified time (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<List<OnlineUserVO>> online(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "查询参数（withinMinutes: 多少分钟内, maxSize: 最多返回几条） / Query parameters",
            required = false
    ) @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long currentUserId = AuthTokenFilter.getUserId(request);
        if (currentUserId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        int withinMinutes = 30;
        int maxSize = 100;
        if (body != null) {
            if (body.get("withinMinutes") instanceof Number) withinMinutes = ((Number) body.get("withinMinutes")).intValue();
            if (body.get("maxSize") instanceof Number) maxSize = ((Number) body.get("maxSize")).intValue();
        }
        List<OnlineUserVO> data = authUserService.getOnlineUsers(withinMinutes, maxSize);
        RESTResult<List<OnlineUserVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
