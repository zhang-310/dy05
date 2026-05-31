package cn.gaifan.douyinOperations.contract.ai;

import java.time.OffsetDateTime;

public record AiGatewayResponse(
        String requestId,
        String providerCode,
        String modelCode,
        String promptCode,
        String productCode,
        String featureCode,
        String output,
        AiUsageTrace usage,
        boolean mock,
        String providerDecision,
        String providerDecisionReason,
        boolean auditRequired,
        OffsetDateTime createdAt
) {
}
