package cn.gaifan.douyinOperations.module.storage.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import cn.gaifan.douyinOperations.module.storage.vo.StorageFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件存储管理（百度 BOS）：列表、上传、删除、获取 CDN 链接
 * 需登录，建议仅管理员可访问
 * File Storage Management (Baidu BOS): List, Upload, Delete, Get CDN URL
 * (Authentication required, Admin recommended)
 */
@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "文件存储 / File Storage", description = "百度 BOS 文件存储的管理（仅管理员）")
public class StorageController {

    private static final String ROLE_ADMIN = "admin";

    @Resource
    private BosStorageService bosStorageService;

    private boolean isAdmin(HttpServletRequest request) {
        return ROLE_ADMIN.equals(AuthTokenFilter.getRoleCode(request));
    }

    @PostMapping("/configured")
    @Operation(
            summary = "检查 BOS 配置 / Check BOS Configuration",
            description = "检查是否已配置百度 BOS（仅管理员） / Check if Baidu BOS is configured (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Query successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Boolean> configured(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        boolean ok = bosStorageService.isConfigured();
        RESTResult<Boolean> r = RESTResult.getSuccess(ok);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/list")
    @Operation(
            summary = "列出存储文件 / List Storage Files",
            description = "列出当前用户路径下的文件（用户只能查看自己路径下的素材） / List files under current user path (user can only view own materials)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "列出成功 / List successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<List<StorageFileVO>> list(HttpServletRequest request,
                                                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                        description = "查询条件（prefixSuffix: 路径后缀，如 2026-03-01/5001/keyframes） / Query parameters",
                                                        required = false
                                                )
                                                @RequestBody(required = false) Map<String, String> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        String prefixSuffix = body != null && body.containsKey("prefixSuffix") ? body.get("prefixSuffix") : "";
        List<StorageFileVO> list = bosStorageService.listUserFiles(userId, prefixSuffix);
        RESTResult<List<StorageFileVO>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/upload")
    @Operation(
            summary = "上传文件 / Upload File",
            description = "上传文件到百度 BOS 存储，可指定存储路径前缀（仅管理员） / Upload file to Baidu BOS with optional path prefix (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "上传成功 / Upload successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误或 BOS 未配置 / Invalid parameters or BOS not configured"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Map<String, String>> upload(HttpServletRequest request,
                                                   @Parameter(description = "上传文件 / File to upload", required = true)
                                                   @RequestParam("file") MultipartFile file,
                                                   @Parameter(description = "存储路径前缀 / Storage path prefix (e.g., uploads/2025/)", required = false)
                                                   @RequestParam(value = "prefix", required = false) String prefix) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        if (!bosStorageService.isConfigured()) {
            return RESTResult.error(ErrorCode.STORAGE_NOT_CONFIGURED, "请先在「系统配置」中配置 BOS（storage.bos.*）");
        }
        Long userId = AuthTokenFilter.getUserId(request);
        String rawPrefix = prefix != null ? prefix.trim().replaceFirst("^/+", "") : "";
        if (rawPrefix.contains("..")) {
            return RESTResult.error(ErrorCode.STORAGE_PATH_ACCESS_DENIED, "非法路径");
        }
        if (!rawPrefix.isEmpty() && !rawPrefix.endsWith("/")) rawPrefix += "/";
        // 路径由后端生成，强制以 {userId}/ 开头，不信任前端
        String keyPrefix = userId + "/" + rawPrefix;
        String originalName = file.getOriginalFilename();
        if (originalName == null) originalName = "file";
        String key = keyPrefix + originalName;
        String url = bosStorageService.upload(key, file);
        Map<String, String> data = new HashMap<>(2);
        data.put("key", key);
        data.put("url", url);
        RESTResult<Map<String, String>> r = RESTResult.addSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(
            summary = "删除存储文件 / Delete Storage File",
            description = "删除指定 key 的对象（仅管理员） / Delete object by key (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "删除成功 / Delete successful"),
            @ApiResponse(responseCode = "400", description = "缺少 key 或其他错误 / Missing key or other error"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> delete(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "删除条件（key: 文件 key） / Delete parameters",
            required = false
    ) @RequestBody(required = false) Map<String, String> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long userId = AuthTokenFilter.getUserId(request);
        String key = body != null ? body.get("key") : null;
        if (key == null || key.trim().isEmpty()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 key");
        }
        bosStorageService.deleteObject(key.trim(), userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/url")
    @Operation(
            summary = "获取文件 URL / Get File URL",
            description = "获取指定 key 的公网/CDN 访问 URL（仅管理员） / Get public/CDN access URL for specified key (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "400", description = "缺少 key / Missing key"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "403", description = "权限不足 / Insufficient permission"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Map<String, String>> getUrl(HttpServletRequest request,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求体，含 key 字段", required = false)
            @RequestBody(required = false) Map<String, String> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isAdmin(request)) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
        }
        Long userId = AuthTokenFilter.getUserId(request);
        String key = body != null ? body.get("key") : null;
        if (key == null || key.trim().isEmpty()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 key");
        }
        String url = bosStorageService.getPublicUrlForUser(key.trim(), userId);
        Map<String, String> data = new HashMap<>(2);
        data.put("key", key.trim());
        data.put("url", url);
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
