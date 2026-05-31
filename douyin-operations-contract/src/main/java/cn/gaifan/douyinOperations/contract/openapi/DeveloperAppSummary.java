package cn.gaifan.douyinOperations.contract.openapi;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 开发者应用摘要。
 *
 * <p>开发者应用归属于租户，是开放 API、MCP 能力和外部 Agent 调用的商业主体。
 * 每个应用明确允许的产品、能力和回调地址，后续才能做授权裁剪、限流和审计。</p>
 */
public record DeveloperAppSummary(
        String appId,
        String tenantId,
        String appName,
        DeveloperAppStatus status,
        List<String> allowedProductCodes,
        List<String> allowedCapabilityCodes,
        List<String> callbackUrls,
        List<String> ipAllowlist,
        boolean launchReady,
        String reviewNote,
        String reviewedByUserId,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt
) {
}
