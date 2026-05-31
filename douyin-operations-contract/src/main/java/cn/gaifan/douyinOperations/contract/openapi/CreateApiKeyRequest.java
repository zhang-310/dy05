package cn.gaifan.douyinOperations.contract.openapi;

import java.time.OffsetDateTime;

/**
 * 创建 OpenAPI API Key 请求。
 *
 * <p>前端和外部调用方不能指定真实 key 或 secret，避免弱密钥、重复密钥或人为泄漏。
 * 后端统一生成高强度随机凭据，并且明文只在创建响应中展示一次。</p>
 */
public record CreateApiKeyRequest(
        // 租户 ID，确保 API Key 不会跨租户创建。
        String tenantId,
        // 开发者应用 ID，API Key 必须挂在应用下，不能直接挂到租户裸调能力。
        String appId,
        // 创建人用户 ID，用于审计和后续追责。
        String createdByUserId,
        // 过期时间；为空时后端使用默认 90 天生命周期。
        OffsetDateTime expiresAt
) {
}
