package cn.gaifan.douyinOperations.contract.credit;

import java.math.BigDecimal;

/**
 * 租户积分账户摘要。
 *
 * <p>账户余额按租户聚合，用户、端、产品和 Agent 的消费都必须回写到该租户账户。
 * MVP 阶段先使用内存 mock，后续应落数据库并加行级锁或账本事件流。</p>
 */
public record CreditAccountSummary(
        String tenantId,
        BigDecimal totalGranted,
        BigDecimal availableCredits,
        BigDecimal frozenCredits,
        BigDecimal usedCredits,
        BigDecimal expiredCredits,
        String currency,
        String period
) {
}
