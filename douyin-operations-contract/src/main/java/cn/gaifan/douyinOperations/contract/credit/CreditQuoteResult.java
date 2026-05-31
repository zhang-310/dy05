package cn.gaifan.douyinOperations.contract.credit;

import java.math.BigDecimal;

/**
 * 积分报价结果。
 *
 * <p>结果同时返回积分、人民币零售金额和预估供应商成本，便于后续毛利率、预算预警、
 * 企业客户账单和用户付费提示使用同一套口径。</p>
 */
public record CreditQuoteResult(
        String quoteId,
        String tenantId,
        String productCode,
        String featureCode,
        String channel,
        BigDecimal requestedAmount,
        BigDecimal unitCredits,
        BigDecimal totalCredits,
        BigDecimal retailAmountCny,
        BigDecimal estimatedProviderCostCny,
        BigDecimal availableBefore,
        boolean sufficient,
        String pricingRule,
        String traceId
) {
}
