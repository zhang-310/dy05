package cn.gaifan.douyinOperations.contract.auth;

import java.math.BigDecimal;

public record QuotaSnapshot(
        String unit,
        BigDecimal remaining,
        BigDecimal monthlyLimit,
        BigDecimal requestedAmount,
        String period
) {
}
