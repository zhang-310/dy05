package cn.gaifan.douyinOperations.module.mcp.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.mcp.McpGatewayOverview;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationRequest;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationResponse;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationSummary;
import cn.gaifan.douyinOperations.contract.mcp.McpToolDescriptor;
import cn.gaifan.douyinOperations.mcp.registry.McpToolRegistry;
import cn.gaifan.douyinOperations.module.mcp.McpInvokeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mcp")
@Tag(name = "MCP 网关 / MCP")
public class McpController {

    private final McpToolRegistry mcpToolRegistry;
    private final McpInvokeService mcpInvokeService;

    public McpController(McpToolRegistry mcpToolRegistry, McpInvokeService mcpInvokeService) {
        this.mcpToolRegistry = mcpToolRegistry;
        this.mcpInvokeService = mcpInvokeService;
    }

    @GetMapping("/tools")
    @Operation(summary = "MCP 工具列表")
    public RESTResult<List<McpToolDescriptor>> tools() {
        return RESTResult.success(mcpToolRegistry.listTools());
    }

    @GetMapping("/overview")
    @Operation(summary = "MCP 网关总览")
    public RESTResult<McpGatewayOverview> overview() {
        return RESTResult.success(mcpToolRegistry.overview());
    }

    @GetMapping("/invocations")
    @Operation(summary = "MCP 调用记录")
    public RESTResult<List<McpInvocationSummary>> invocations(
            @RequestParam(name = "tenantId", defaultValue = "demo-tenant") String tenantId) {
        return RESTResult.success(mcpInvokeService.listInvocations(tenantId));
    }

    @PostMapping("/invoke")
    @Operation(summary = "MCP 工具调用")
    public ResponseEntity<RESTResult<McpInvocationResponse>> invoke(@RequestBody McpInvocationRequest request) {
        McpInvocationResponse response = mcpInvokeService.invoke(request);
        HttpStatus status = response.allowed() ? HttpStatus.OK : HttpStatus.PAYMENT_REQUIRED;
        return ResponseEntity.status(status).body(RESTResult.success(response));
    }
}
