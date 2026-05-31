package cn.gaifan.douyinOperations.contract.mcp;

/**
 * MCP JSON-RPC 错误对象。
 *
 * <p>公网 MCP 客户端通常只认识 JSON-RPC 2.0 的错误结构，因此这里不复用平台内部
 * ApiResponse。错误内容只放协议错误、方法错误或业务拒绝摘要，不暴露 API Key、Agent Token、
 * 用户输入原文或模型完整输出。</p>
 */
public record McpJsonRpcError(
        int code,
        String message,
        Object data
) {
}
