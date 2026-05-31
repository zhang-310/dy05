package cn.gaifan.douyinOperations.contract.openapi;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * API Key 解析结果。
 *
 * <p>开放 API、MCP 和企业智能体后续都会先解析调用身份，再进入统一授权、扣费、限流和审计。
 * 该结果只返回脱敏摘要和租户归属，不返回真实密钥。</p>
 */
public record ApiKeyResolveResult(
        // API Key 主键。
        String keyId,
        // 开发者应用 ID。
        String appId,
        // 租户 ID，后续所有授权、积分和审计都以它为商业归属。
        String tenantId,
        // 应用名称，用于控制台和审计展示。
        String appName,
        // 脱敏 key。
        String maskedKey,
        // 当前 key 是否可用。
        boolean active,
        // 是否命中 Redis 缓存。
        boolean cacheHit,
        // 过期时间。
        OffsetDateTime expiresAt,
        // 开发者应用允许访问的产品范围，用于 OpenAPI/MCP 二次校验。
        List<String> allowedProductCodes,
        // 开发者应用允许访问的能力范围，用于 OpenAPI/MCP 二次校验。
        List<String> allowedCapabilityCodes,
        // 解析说明。
        String message
) {
}
