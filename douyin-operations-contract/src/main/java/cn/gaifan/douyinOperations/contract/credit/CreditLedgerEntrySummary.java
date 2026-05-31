package cn.gaifan.douyinOperations.contract.credit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 积分账本流水摘要。
 *
 * <p>每一笔流水都保留 traceId、productCode、featureCode 和 channel，保证开放 API、
 * MCP、Web、App、小程序、企业智能体的扣费可以被统一审计和追溯。</p>
 */
public record CreditLedgerEntrySummary(
        String entryId,
        String tenantId,
        String userId,
        String agentId,
        String productCode,
        String featureCode,
        String channel,
        CreditTransactionType transactionType,
        BigDecimal credits,
        BigDecimal balanceAfter,
        String traceId,
        String reason,
        OffsetDateTime createdAt
) {
}
