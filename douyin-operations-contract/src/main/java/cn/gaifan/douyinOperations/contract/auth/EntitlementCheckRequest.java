package cn.gaifan.douyinOperations.contract.auth;

import java.math.BigDecimal;

public record EntitlementCheckRequest(
        String tenantId,
        String userId,
        String productCode,
        String featureCode,
        String channel,
        BigDecimal requestedAmount,
        String idempotencyKey,
        String traceId
) {
}
