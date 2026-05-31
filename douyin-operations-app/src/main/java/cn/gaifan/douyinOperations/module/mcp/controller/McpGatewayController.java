package cn.gaifan.douyinOperations.module.mcp.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.mcp.McpGatewayOverview;
import cn.gaifan.douyinOperations.contract.mcp.McpToolDescriptor;
import cn.gaifan.douyinOperations.mcp.registry.McpToolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mcp")
@Tag(name = "MCP 网关")
public class McpGatewayController {

    private final McpToolRegistry mcpToolRegistry;

    public McpGatewayController(McpToolRegistry mcpToolRegistry) {
        this.mcpToolRegistry = mcpToolRegistry;
    }

    @GetMapping("/overview")
    @Operation(summary = "MCP 工具总览")
    public RESTResult<McpGatewayOverview> overview() {
        return RESTResult.success(mcpToolRegistry.overview());
    }

    @GetMapping("/tools")
    @Operation(summary = "MCP 工具列表")
    public RESTResult<List<McpToolDescriptor>> tools() {
        return RESTResult.success(mcpToolRegistry.listTools());
    }
}
