package cn.gaifan.douyinOperations.module.config.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSaveVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSearchVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * 系统配置管理（仅管理员）：列表、按 key 获取、保存、删除
 * System Config Management (Admin Only): List, Get by Key, Save, Delete
 */
@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "系统配置 / System Configuration", description = "系统配置项的管理（仅管理员）")
public class ConfigController {

    private static final String ROLE_ADMIN = "admin";

    @Resource
    private ConfigService configService;

    private boolean isAdmin(HttpServletRequest request) {
        return ROLE_ADMIN.equals(AuthTokenFilter.getRoleCode(request));
    }

    @PostMapping("/list")
    @Operation(
            summary = "查询配置列表 / Search Configurations",
            description = "分页查询系统配置项（仅管理员） / Search and paginate system configuration items (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<ConfigVO>> list(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "查询条件 / Search criteria",
            required = false
    ) @RequestBody(required = false) ConfigSearchVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        if (vo == null) vo = new ConfigSearchVO();
        PageResultVO<ConfigVO> data = configService.search(vo);
        RESTResult<PageResultVO<ConfigVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(
            summary = "按 Key 获取配置 / Get Configuration by Key",
            description = "根据配置 Key 获取配置值（仅管理员） / Get configuration value by key (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "404", description = "配置不存在 / Configuration not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<ConfigVO> get(HttpServletRequest request,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求体，含 key 字段", required = false)
            @RequestBody(required = false) java.util.Map<String, String> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        String key = body != null ? body.get("key") : null;
        ConfigVO data = configService.getByKey(key);
        RESTResult<ConfigVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(
            summary = "保存配置 / Save Configuration",
            description = "创建或更新系统配置项（仅管理员） / Create or update system configuration (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "保存成功 / Save successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "配置信息 / Configuration information",
            required = true
    ) @RequestBody ConfigSaveVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        long id = configService.save(vo, AuthTokenFilter.getUserId(request));
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(
            summary = "删除配置 / Delete Configuration",
            description = "根据配置 ID 删除系统配置项（仅管理员） / Delete configuration by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "删除成功 / Delete successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "404", description = "配置不存在 / Configuration not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> delete(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "配置 ID / Configuration ID",
            required = false
    ) @RequestBody(required = false) Map<String, Long> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long id = body != null ? body.get("id") : null;
        configService.deleteById(id);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
