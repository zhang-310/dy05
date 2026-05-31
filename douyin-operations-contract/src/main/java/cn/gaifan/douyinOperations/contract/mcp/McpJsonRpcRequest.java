package cn.gaifan.douyinOperations.contract.mcp;

import java.util.Map;

/**
 * MCP Streamable HTTP JSON-RPC 请求。
 *
 * <p>远程 MCP 客户端通过同一个 /mcp HTTP 端点发送 initialize、tools/list、tools/call。
 * params 保持泛型 Map，是为了兼容不同 MCP 客户端在工具参数里扩展 cursor、meta 或业务参数。</p>
 */
public record McpJsonRpcRequest(
        String jsonrpc,
        Object id,
        String method,
        Map<String, Object> params
) {
}
