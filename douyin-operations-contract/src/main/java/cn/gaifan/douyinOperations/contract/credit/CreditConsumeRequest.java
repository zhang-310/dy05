package cn.gaifan.douyinOperations.contract.credit;

import java.math.BigDecimal;

/**
 * 积分消费请求。
 *
 * <p>MVP 中 reserve 和 commit 可以在一次接口里完成；真实上线后应拆成冻结、执行、
 * 成功扣减或失败释放，避免长任务、视频生成、批量 MCP 调用出现扣费不一致。</p>
 */
public record CreditConsumeRequest(
        String tenantId,
        String userId,
        String agentId,
        String productCode,
        String featureCode,
        String channel,
        BigDecimal requestedAmount,
        String pricingTier,
        String traceId,
        String reason
) {
}
