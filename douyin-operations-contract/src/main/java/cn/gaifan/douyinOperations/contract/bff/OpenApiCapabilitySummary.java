package cn.gaifan.douyinOperations.contract.bff;

/**
 * 开放 API 能力摘要。
 *
 * <p>这里描述可对外开放的能力，不包含 API Key、签名规则、真实调用地址等敏感配置。
 * 后续开放平台上线时应接入开发者应用、密钥、签名、限流和审计。</p>
 */
public record OpenApiCapabilitySummary(
        // 能力编码，通常对应 MCP 工具或平台 API 能力。
        String capabilityCode,
        // 能力名称。
        String name,
        // 所属产品。
        String productCode,
        // 所需功能授权。
        String requiredFeatureCode,
        // 限流提示。
        String rateLimitHint,
        // 是否需要审计。
        boolean auditRequired,
        // 当前开放状态。
        String status
) {
}
