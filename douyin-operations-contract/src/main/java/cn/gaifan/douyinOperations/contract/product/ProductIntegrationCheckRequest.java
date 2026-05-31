package cn.gaifan.douyinOperations.contract.product;

import java.math.BigDecimal;

/**
 * 产品间能力调用检查请求。
 *
 * <p>发起产品和目标产品都必须显式传入。平台会分别检查 sourceProductCode/sourceFeatureCode
 * 与 targetProductCode/targetFeatureCode 的授权，确保“产品可相互调用”不等于“绕过目标产品购买”。</p>
 */
public record ProductIntegrationCheckRequest(
        String tenantId,
        String userId,
        String sourceProductCode,
        String sourceFeatureCode,
        String targetProductCode,
        String targetFeatureCode,
        String channel,
        BigDecimal requestedAmount,
        String traceId
) {
}
