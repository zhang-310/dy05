package cn.gaifan.douyinOperations.contract.commerce;

import java.math.BigDecimal;

/**
 * 支付履约后向 gf 积分账户发放额度（由 platform 实现，payment 可选注入）。
 */
public interface PaymentCreditGrantPort {

    void grantOnPayment(String tenantId, String userId, BigDecimal credits, String traceId, String reason);
}
