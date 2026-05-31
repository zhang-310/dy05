package cn.gaifan.douyinOperations.contract.product;

import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationResult;

import java.math.BigDecimal;

/**
 * 产品间商业化调用结果。
 *
 * <p>结果同时返回互调决策、扣费结果、用量台账、审计事件和互调流水 ID。
 * 前端、OpenAPI、MCP 或企业 Agent 只需要看 allowed 和这些 ID，就能确认一次跨产品调用是否已经纳入商业治理。</p>
 */
public record ProductIntegrationInvocationResult(
        String invocationId,
        boolean allowed,
        String status,
        String message,
        ProductIntegrationDecision decision,
        CreditConsumeResult creditConsumeResult,
        CreditReservationResult creditReservationResult,
        String usageLedgerId,
        String auditId,
        ProductIntegrationBillingMode billingMode,
        String workflowTaskId,
        String reservationId,
        BigDecimal retailRevenueCny,
        BigDecimal providerCostCny,
        BigDecimal grossProfitCny,
        String traceId
) {
}
