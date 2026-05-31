package cn.gaifan.douyinOperations.module.mcp.controller;

import cn.gaifan.douyinOperations.common.id.Ids;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationRequest;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationResponse;
import cn.gaifan.douyinOperations.contract.mcp.McpJsonRpcRequest;
import cn.gaifan.douyinOperations.contract.mcp.McpJsonRpcResponse;
import cn.gaifan.douyinOperations.contract.mcp.McpRuntimeDescriptor;
import cn.gaifan.douyinOperations.contract.mcp.McpToolDescriptor;
import cn.gaifan.douyinOperations.mcp.registry.McpToolRegistry;
import cn.gaifan.douyinOperations.module.mcp.McpInvokeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class McpRuntimeController {

    private static final String PROTOCOL_VERSION = "2025-06-18";

    private final McpToolRegistry mcpToolRegistry;
    private final McpInvokeService mcpInvokeService;
    private final ObjectMapper objectMapper;

    public McpRuntimeController(
            McpToolRegistry mcpToolRegistry,
            McpInvokeService mcpInvokeService,
            ObjectMapper objectMapper
    ) {
        this.mcpToolRegistry = mcpToolRegistry;
        this.mcpInvokeService = mcpInvokeService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/mcp/runtime")
    public RESTResult<McpRuntimeDescriptor> apiRuntime() {
        return RESTResult.success(runtimeDescriptor());
    }

    @GetMapping({"/.well-known/mcp", "/mcp"})
    public McpRuntimeDescriptor discovery() {
        return runtimeDescriptor();
    }

    @PostMapping("/mcp")
    public McpJsonRpcResponse handleJsonRpc(@RequestBody McpJsonRpcRequest request) {
        if (request == null || request.method() == null || request.method().isBlank()) {
            return McpJsonRpcResponse.error(null, -32600, "Invalid MCP JSON-RPC request", null);
        }
        return switch (request.method()) {
            case "initialize" -> McpJsonRpcResponse.ok(request.id(), initializeResult());
            case "tools/list" -> McpJsonRpcResponse.ok(request.id(), Map.of("tools", runtimeTools()));
            case "tools/call" -> toolsCall(request);
            default -> McpJsonRpcResponse.error(request.id(), -32601, "Unsupported MCP method: " + request.method(),
                    Map.of("supportedMethods", runtimeDescriptor().supportedMethods()));
        };
    }

    private McpRuntimeDescriptor runtimeDescriptor() {
        return new McpRuntimeDescriptor(
                "dy05-mcp",
                PROTOCOL_VERSION,
                "streamable-http-json-rpc",
                "/mcp",
                "/mcp",
                List.of("initialize", "tools/list", "tools/call"),
                Map.of(
                        "tools", Map.of("listChanged", false),
                        "logging", Map.of("enabled", false)
                ),
                List.of(
                        "Authorization: Bearer <token>",
                        "X-Gaifan-Channel: MCP",
                        "X-Gaifan-Trace-Id: <traceId>"
                ),
                List.of("gf_mcp_invocation", "gf_credit_ledger", "gf_credit_reservation"),
                List.of(
                        "工具调用需产品授权与积分台账",
                        "video.analyze 对接爆款拆解异步任务",
                        "协议层不保存用户输入原文"
                ),
                mcpToolRegistry.listTools().size()
        );
    }

    private Map<String, Object> initializeResult() {
        return Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", runtimeDescriptor().capabilities(),
                "serverInfo", Map.of("name", "dy05-mcp", "version", "0.1.0")
        );
    }

    private List<Map<String, Object>> runtimeTools() {
        return mcpToolRegistry.listTools().stream()
                .map(tool -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", tool.toolCode());
                    row.put("title", tool.name());
                    row.put("description", tool.name());
                    row.put("inputSchema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "viralVideoId", Map.of("type", "string"),
                                    "videoId", Map.of("type", "string"),
                                    "userId", Map.of("type", "string")
                            )
                    ));
                    return row;
                })
                .toList();
    }

    private McpJsonRpcResponse toolsCall(McpJsonRpcRequest request) {
        Map<String, Object> params = request.params() == null ? Map.of() : request.params();
        String toolName = firstNonBlank(asString(params.get("name")), asString(params.get("toolCode")));
        if (toolName.isBlank()) {
            return McpJsonRpcResponse.error(request.id(), -32602, "tools/call requires params.name", null);
        }
        McpToolDescriptor tool = mcpToolRegistry.findTool(toolName).orElse(null);
        if (tool == null) {
            return McpJsonRpcResponse.error(request.id(), -32602, "MCP tool not found: " + toolName, null);
        }

        Map<String, String> arguments = toStringMap(params.get("arguments"));
        String tenantId = firstNonBlank(asString(params.get("tenantId")), arguments.get("tenantId"), "demo-tenant");
        String userId = firstNonBlank(asString(params.get("userId")), arguments.get("userId"), "mcp-user");
        String channel = firstNonBlank(asString(params.get("channel")), arguments.get("channel"), "MCP");
        String traceId = firstNonBlank(asString(params.get("traceId")), arguments.get("traceId"), Ids.compactUuid("trace_mcp_runtime"));

        McpInvocationResponse invocation = mcpInvokeService.invoke(new McpInvocationRequest(
                tenantId,
                userId,
                tool.toolCode(),
                channel,
                arguments,
                traceId
        ));

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("allowed", invocation.allowed());
        evidence.put("decisionCode", invocation.decisionCode());
        evidence.put("message", invocation.message());
        evidence.put("traceId", traceId);
        evidence.put("invocationId", invocation.invocationId());
        evidence.put("toolResult", invocation.toolResult());

        return McpJsonRpcResponse.ok(request.id(), Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", invocation.allowed()
                                ? "MCP completed traceId=" + traceId
                                : "MCP rejected " + invocation.decisionCode()
                )),
                "structuredContent", evidence,
                "isError", !invocation.allowed()
        ));
    }

    private Map<String, String> toStringMap(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        Map<?, ?> raw = objectMapper.convertValue(value, Map.class);
        Map<String, String> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> result.put(String.valueOf(key), stringValue(item)));
        return result;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
