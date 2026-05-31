package cn.gaifan.douyinOperations.contract.port;

/**
 * 支付 Port — 支付订单创建 + 回调
 */
public interface PaymentPort {
    record OrderResult(String orderNo, long amount, String status) {}
    OrderResult createOrder(String productCode, long amount, Long userId);
    boolean processCallback(String orderNo, String callbackData);
}
