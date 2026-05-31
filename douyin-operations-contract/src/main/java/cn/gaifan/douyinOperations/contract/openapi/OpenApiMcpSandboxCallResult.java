package cn.gaifan.douyinOperations.contract.openapi;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI/MCP 沙箱签名调用结果。
 *
 * <p>结果保留签名请求形态、脱敏签名预览和落账证明，但不返回 API Secret、明文 API Key
 * 或任何可重放的完整凭据。</p>
 */
public record OpenApiMcpSandboxCallResult(
        // OpenAPI 公网基准地址。
        String openApiBaseUrl,
        // MCP 公网基准地址。
        String mcpBaseUrl,
        // 本次沙箱调用路径。
        String invokePath,
        // 请求方法。
        String method,
        // 脱敏 API Key。
        String maskedApiKey,
        // 脱敏签名请求头；签名只给预览，不能用于重放。
        Map<String, String> signedHeaders,
        // 脱敏后的签名原文，方便开发者核对字段顺序。
        String sanitizedCanonicalString,
        // 签名摘要预览，不包含完整签名。
        String signaturePreview,
        // 本次沙箱请求体。
        OpenApiSignedCallRequest requestBody,
        // 真实 OpenAPI 调用结果，包含 usageLedgerId、auditId 和 traceId。
        OpenApiSignedCallResult<Object> callResult,
        // 落账证明，用于页面和 smoke 脚本展示。
        List<String> ledgerProof,
        // 下一步接入动作。
        List<String> nextSteps
) {
}
