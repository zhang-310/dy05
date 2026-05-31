package cn.gaifan.douyinOperations.contract.ai;

import java.math.BigDecimal;

public record AiUsageTrace(
        BigDecimal totalTokens,
        BigDecimal estimatedCostCny
) {
}
