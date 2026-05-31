package cn.gaifan.douyinOperations.contract.openapi;

import java.util.List;

/**
 * OpenAPI/MCP SDK 示例包。
 *
 * <p>该合同面向 docs.gaifan.cn、开发者控制台和交付人员，统一输出 curl、TypeScript、Python
 * 和 MCP JSON-RPC 示例。真实 SDK 发布前，先用这个示例包降低客户接入成本。</p>
 */
public record OpenApiSdkExampleBundle(
        String tenantId,
        String docsDomain,
        String openApiBaseUrl,
        String mcpEndpoint,
        String signatureAlgorithm,
        List<String> environmentVariables,
        List<OpenApiSdkExampleSnippet> snippets,
        List<String> verificationSteps,
        List<String> productionWarnings
) {
}
