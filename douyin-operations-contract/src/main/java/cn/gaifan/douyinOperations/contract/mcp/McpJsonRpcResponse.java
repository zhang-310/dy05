package cn.gaifan.douyinOperations.contract.mcp;

/**
 * MCP Streamable HTTP JSON-RPC 响应。
 *
 * <p>成功时 result 有值，失败时 error 有值。公网协议层不包 ApiResponse，避免 MCP 客户端无法识别
 * 平台私有响应格式；真正的授权、积分、用量和审计证据会放在 result 或 error.data 的结构化摘要里。</p>
 */
public record McpJsonRpcResponse(
        String jsonrpc,
        Object id,
        Object result,
        McpJsonRpcError error
) {
    public static McpJsonRpcResponse ok(Object id, Object result) {
        return new McpJsonRpcResponse("2.0", id, result, null);
    }

    public static McpJsonRpcResponse error(Object id, int code, String message, Object data) {
        return new McpJsonRpcResponse("2.0", id, null, new McpJsonRpcError(code, message, data));
    }
}
