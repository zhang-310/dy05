package cn.gaifan.douyinOperations.contract.product;

import java.math.BigDecimal;

/**
 * 产品间商业化调用请求。
 *
 * <p>该请求用于把“产品互调”从单纯授权检查升级为真实商业闭环：先检查发起产品和目标产品授权，
 * 再按目标产品功能完成积分扣费、用量入账和审计记录。业务产品只提交调用意图，不直接写账本，
 * 避免六个独立产品各自实现一套扣费逻辑。</p>
 */
public record ProductIntegrationInvocationRequest(
        String tenantId,
        String userId,
        String agentId,
        String sourceProductCode,
        String sourceFeatureCode,
        String targetProductCode,
        String targetFeatureCode,
        String channel,
        BigDecimal requestedAmount,
        String pricingTier,
        String traceId,
        String invocationInputSummary,
        boolean createWorkflowTask,
        String workflowCode,
        String workflowTitle,
        String sourceAssetId,
        String resultAssetId,
        boolean reviewRequired
) {
}
