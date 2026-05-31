package cn.gaifan.douyinOperations.contract.credit;

import java.math.BigDecimal;

/**
 * 积分报价请求。
 *
 * <p>报价只计算本次调用预计消耗，不真正扣减余额。所有商业化调用在授权通过后、
 * 真正执行 AI/MCP/API 能力前都应先报价，前端也可以用它展示预估成本。</p>
 */
public record CreditQuoteRequest(
        String tenantId,
        String userId,
        String agentId,
        String productCode,
        String featureCode,
        String channel,
        BigDecimal requestedAmount,
        String pricingTier,
        String traceId
) {
}
