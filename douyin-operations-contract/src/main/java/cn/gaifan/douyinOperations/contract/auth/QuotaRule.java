package cn.gaifan.douyinOperations.contract.auth;

import java.math.BigDecimal;

public record QuotaRule(
        String unit,
        BigDecimal monthlyLimit,
        String period
) {
}
