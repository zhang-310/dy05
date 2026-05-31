package cn.gaifan.douyinOperations.contract.credit;

import java.math.BigDecimal;

/**
 * 积分冻结单结果。
 *
 * <p>调用方通过 reservationId 继续执行 commit 或 release；前端和治理台通过账本流水
 * 判断本次冻结是否已经形成可审计的商业闭环。</p>
 */
public record CreditReservationResult(
        String reservationId,
        CreditReservationStatus status,
        boolean allowed,
        String decisionCode,
        String message,
        CreditQuoteResult quote,
        BigDecimal reservedCredits,
        BigDecimal balanceAfter,
        CreditLedgerEntrySummary reserveEntry,
        CreditLedgerEntrySummary commitEntry,
        CreditLedgerEntrySummary releaseEntry
) {
}
