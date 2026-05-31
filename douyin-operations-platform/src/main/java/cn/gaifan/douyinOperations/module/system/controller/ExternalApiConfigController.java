package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.system.config.ExternalApiHealthCheckScheduler;
import cn.gaifan.douyinOperations.module.system.entity.ExternalApiConfig;
import cn.gaifan.douyinOperations.module.system.service.ExternalApiConfigService;
import cn.gaifan.douyinOperations.module.system.vo.ExternalApiConfigSaveVO;
import cn.gaifan.douyinOperations.module.system.vo.ExternalApiConfigSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 外部 API 配置管理控制器
 */
@RestController
@RequestMapping("/api/v1/system/external-api")
@Tag(name = "外部API配置 / External API Config", description = "外部API供应商配置管理（需管理员）")
public class ExternalApiConfigController {

    private static final String ROLE_ADMIN = "admin";

    @Resource
    private ExternalApiConfigService externalApiConfigService;

    @Resource
    private ExternalApiHealthCheckScheduler externalApiHealthCheckScheduler;

    private boolean isAdmin(HttpServletRequest request) {
        return ROLE_ADMIN.equals(AuthTokenFilter.getRoleCode(request));
    }

    /**
     * 分页查询外部 API 配置列表
     */
    @PostMapping("/list")
    @Operation(summary = "外部API配置列表")
    public RESTResult<PageResultVO<ExternalApiConfig>> list(
            HttpServletRequest request,
            @RequestBody(required = false) ExternalApiConfigSearchVO searchVO) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        if (searchVO == null) searchVO = new ExternalApiConfigSearchVO();
        return RESTResult.getSuccess(externalApiConfigService.search(searchVO));
    }

    /**
     * 根据供应商编码获取配置详情
     */
    @PostMapping("/get")
    @Operation(summary = "获取外部API配置详情")
    public RESTResult<ExternalApiConfig> get(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        String providerCode = body != null ? body.get("providerCode") : null;
        if (providerCode == null || providerCode.isBlank())
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "providerCode 不能为空");

        return RESTResult.getSuccess(externalApiConfigService.getByProviderCode(providerCode));
    }

    /**
     * 新增或更新外部 API 配置
     */
    @PostMapping("/save")
    @Operation(summary = "保存外部API配置")
    public RESTResult<ExternalApiConfig> save(
            HttpServletRequest request,
            @Valid @RequestBody ExternalApiConfigSaveVO saveVO) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        return RESTResult.addSuccess(externalApiConfigService.save(saveVO));
    }

    /**
     * 逻辑删除外部 API 配置
     */
    @PostMapping("/delete")
    @Operation(summary = "删除外部API配置")
    public RESTResult<Void> delete(
            HttpServletRequest request,
            @RequestBody Map<String, Long> body) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        Long id = body != null ? body.get("id") : null;
        if (id == null)
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "id 不能为空");

        externalApiConfigService.delete(id);
        return RESTResult.success();
    }

    /**
     * 更新供应商健康检查状态
     */
    @PostMapping("/health-status")
    @Operation(summary = "更新健康检查状态")
    public RESTResult<Void> updateHealthStatus(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        String providerCode = (String) body.get("providerCode");
        String status = (String) body.get("status");
        Integer latencyMs = body.get("latencyMs") != null ? ((Number) body.get("latencyMs")).intValue() : null;
        Float successRate = body.get("successRate") != null ? ((Number) body.get("successRate")).floatValue() : null;

        if (providerCode == null || providerCode.isBlank())
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "providerCode 不能为空");

        externalApiConfigService.updateHealthStatus(providerCode, status, latencyMs, successRate);
        return RESTResult.success();
    }

    /**
     * 立即触发单个供应商后端探测，不在浏览器直连第三方。
     */
    @PostMapping("/probe")
    @Operation(summary = "立即探测外部API健康状态")
    public RESTResult<Map<String, Object>> probe(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!isAdmin(request))
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");

        String providerCode = body != null ? body.get("providerCode") : null;
        if (providerCode == null || providerCode.isBlank())
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "providerCode 不能为空");

        String status = externalApiHealthCheckScheduler.checkProvider(providerCode);
        return RESTResult.getSuccess(Map.of("providerCode", providerCode, "status", status));
    }

    /**
     * 获取指定分类下已启用的配置
     */
    @PostMapping("/by-category")
    @Operation(summary = "按分类获取已启用配置")
    public RESTResult<List<ExternalApiConfig>> byCategory(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {

        if (AuthTokenFilter.getUserId(request) == null)
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        String category = body != null ? body.get("category") : null;
        if (category == null || category.isBlank())
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "category 不能为空");

        return RESTResult.getSuccess(externalApiConfigService.getEnabledByCategory(category));
    }
}
