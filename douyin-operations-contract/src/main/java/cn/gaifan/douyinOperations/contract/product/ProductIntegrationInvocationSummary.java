package cn.gaifan.douyinOperations.contract.product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 产品互调调用流水摘要。
 *
 * <p>该摘要用于控制台展示，不保存用户输入的完整素材或提示词，只保留产品、功能、金额、状态和 traceId。
 * 这样可以支撑审计和毛利统计，同时降低敏感业务数据在平台账本里扩散的风险。</p>
 */
public record ProductIntegrationInvocationSummary(
        String invocationId,
        String tenantId,
        String userId,
        String agentId,
        String sourceProductCode,
        String sourceFeatureCode,
        String targetProductCode,
        String targetFeatureCode,
        String billingProductCode,
        String billingFeatureCode,
        String channel,
        String status,
        ProductIntegrationBillingMode billingMode,
        String workflowTaskId,
        String reservationId,
        BigDecimal requestedAmount,
        BigDecimal retailRevenueCny,
        BigDecimal providerCostCny,
        BigDecimal grossProfitCny,
        String traceId,
        OffsetDateTime createdAt
) {
}
