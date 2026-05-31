package cn.gaifan.douyinOperations.contract.credit;

import java.math.BigDecimal;

/**
 * 积分消费结果。
 *
 * <p>该对象会被 MCP、OpenAPI、AI 网关和业务产品复用，调用方只需要判断 allowed，
 * 不需要理解底层账本如何冻结和扣减。</p>
 */
public record CreditConsumeResult(
        String transactionId,
        boolean allowed,
        String decisionCode,
        String message,
        CreditQuoteResult quote,
        BigDecimal consumedCredits,
        BigDecimal balanceAfter,
        CreditLedgerEntrySummary reserveEntry,
        CreditLedgerEntrySummary commitEntry
) {
}
