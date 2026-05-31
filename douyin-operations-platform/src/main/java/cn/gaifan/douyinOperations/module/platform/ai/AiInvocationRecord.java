package cn.gaifan.douyinOperations.module.platform.ai;

import java.math.BigDecimal;

/**
 * AI 调用台账写入参数（gf_ai_invocation + gf_usage_ledger）。
 */
public record AiInvocationRecord(
        String requestId,
        String tenantId,
        String userId,
        String productCode,
        String featureCode,
        String providerCode,
        String modelCode,
        String promptCode,
        BigDecimal totalTokens,
        BigDecimal estimatedCostCny,
        BigDecimal retailRevenueCny,
        String traceId,
        String channel,
        boolean success
) {
}
