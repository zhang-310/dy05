package cn.gaifan.douyinOperations.module.openapi.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiDeveloperOverview;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiMcpQuickstart;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiMcpSandboxCallRequest;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiMcpSandboxCallResult;
import cn.gaifan.douyinOperations.module.openapi.OpenApiDeveloperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/openapi")
@Tag(name = "OpenAPI 开发者")
public class OpenApiDeveloperController {

    private final OpenApiDeveloperService openApiDeveloperService;

    public OpenApiDeveloperController(OpenApiDeveloperService openApiDeveloperService) {
        this.openApiDeveloperService = openApiDeveloperService;
    }

    @GetMapping("/overview")
    @Operation(summary = "OpenAPI 开发者总览")
    public RESTResult<OpenApiDeveloperOverview> overview(
            @RequestParam(name = "tenantId", defaultValue = "demo-tenant") String tenantId) {
        return RESTResult.success(openApiDeveloperService.overview(tenantId));
    }

    @GetMapping("/developer/overview")
    public RESTResult<OpenApiDeveloperOverview> developerOverview(
            @RequestParam(name = "tenantId", defaultValue = "demo-tenant") String tenantId) {
        return RESTResult.success(openApiDeveloperService.overview(tenantId));
    }

    @GetMapping("/developer/quickstart")
    public RESTResult<OpenApiMcpQuickstart> quickstart(
            @RequestParam(name = "tenantId", defaultValue = "demo-tenant") String tenantId) {
        return RESTResult.success(openApiDeveloperService.quickstart(tenantId));
    }

    @PostMapping("/sandbox/mcp-call")
    public RESTResult<OpenApiMcpSandboxCallResult> sandboxCall(@RequestBody OpenApiMcpSandboxCallRequest request) {
        return RESTResult.success(openApiDeveloperService.sandboxCall(request));
    }
}
