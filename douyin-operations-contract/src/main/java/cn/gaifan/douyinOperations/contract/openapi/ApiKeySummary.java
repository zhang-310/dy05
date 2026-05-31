package cn.gaifan.douyinOperations.contract.openapi;

import java.time.OffsetDateTime;

/**
 * API Key 脱敏摘要。
 *
 * <p>列表接口只能展示 maskedKey、状态和生命周期字段。
 * 明文 key 与明文 secret 只允许在创建或轮换响应里出现一次，不能进入列表、日志或审计。</p>
 */
public record ApiKeySummary(
        String keyId,
        String appId,
        String maskedKey,
        ApiKeyStatus status,
        String createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime lastUsedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        String rotatedFromKeyId
) {
}
