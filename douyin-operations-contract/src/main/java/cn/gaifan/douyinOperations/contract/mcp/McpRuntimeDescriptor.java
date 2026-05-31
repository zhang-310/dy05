package cn.gaifan.douyinOperations.contract.mcp;

import java.util.List;
import java.util.Map;

/**
 * MCP 公网运行时描述。
 *
 * <p>该对象同时服务 docs.gaifan.cn、控制台 MCP 页面和 /.well-known/mcp 发现入口。它只描述协议、
 * 端点、认证头和治理账本，不携带任何真实密钥；开发者需要到 OpenAPI 应用里申请 API Key 或
 * 企业 Agent Token 后才能真正调用 tools/call。</p>
 */
public record McpRuntimeDescriptor(
        String serverName,
        String protocolVersion,
        String transport,
        String publicBaseUrl,
        String endpoint,
        List<String> supportedMethods,
        Map<String, Object> capabilities,
        List<String> authenticationHeaders,
        List<String> ledgerCheckpoints,
        List<String> securityNotes,
        int toolCount
) {
}
