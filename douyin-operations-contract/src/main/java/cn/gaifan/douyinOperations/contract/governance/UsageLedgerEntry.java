package cn.gaifan.douyinOperations.contract.governance;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UsageLedgerEntry(
        String usageId,
        String traceId,
        String tenantId,
        String userId,
        String agentId,
        String productCode,
        String featureCode,
        String channel,
        String unit,
        BigDecimal amount,
        BigDecimal retailRevenueCny,
        BigDecimal providerCostCny,
        BigDecimal grossProfitCny,
        OffsetDateTime createdAt
) {
}
