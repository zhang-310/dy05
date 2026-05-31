package cn.gaifan.douyinOperations.contract.openapi;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI/MCP 开发者接入示例。
 *
 * <p>示例只展示占位 API Key、请求头、请求体和安全说明，不承载真实密钥。
 * 这样 docs.gaifan.cn、api.gaifan.cn 和控制台可以复用同一份合同，避免文档、页面和后端签名口径漂移。</p>
 */
public record OpenApiMcpExample(
        // 示例标题，面向开发者说明当前示例解决什么接入场景。
        String title,
        // 对应的 OpenAPI 能力编码，必须来自 MCP 工具注册表开放白名单。
        String capabilityCode,
        // 请求方法，例如 POST。
        String method,
        // 对外端点，例如 https://api.gaifan.cn/api/openapi/invoke。
        String endpoint,
        // 示例请求头；API Key 和签名只使用占位或脱敏值。
        Map<String, String> headers,
        // 示例请求体 JSON，必须使用合规 mock/用户提交类参数。
        String requestBody,
        // curl 示例。
        String curlExample,
        // PowerShell 示例。
        String powershellExample,
        // JavaScript/Node 示例。
        String javascriptExample,
        // 调用注意事项，包含授权、积分、审计和合规边界。
        List<String> notes
) {
}
