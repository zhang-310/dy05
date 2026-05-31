package cn.gaifan.douyinOperations.contract.credit;

import java.math.BigDecimal;

/**
 * 积分冻结请求。
 *
 * <p>长任务、跨产品互调、批量报告、数字人和短剧生成都可能在供应商执行阶段失败。
 * 因此它们必须先冻结积分，等业务任务确认成功后再提交扣减，失败或取消时释放冻结额度。</p>
 */
public record CreditReserveRequest(
        String tenantId,
        String userId,
        String agentId,
        String productCode,
        String featureCode,
        String channel,
        BigDecimal requestedAmount,
        String pricingTier,
        String traceId,
        String businessKey,
        String reason
) {
}
