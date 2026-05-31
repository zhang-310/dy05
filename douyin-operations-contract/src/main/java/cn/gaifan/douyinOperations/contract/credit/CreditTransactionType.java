package cn.gaifan.douyinOperations.contract.credit;

/**
 * 积分账本交易类型。
 *
 * <p>积分中心必须区分发放、冻结、扣减、释放和退款，后续接入微信支付、支付宝、
 * 苹果内购、企业订阅或人工赠送时，都可以归一到同一套账本流水。</p>
 */
public enum CreditTransactionType {
    /** 订阅、购买、活动或人工调整带来的积分发放。 */
    GRANT,
    /** 调用前预冻结，避免并发请求把同一份余额重复消费。 */
    RESERVE,
    /** 能力成功执行后把冻结积分正式扣减。 */
    COMMIT,
    /** 能力未执行、取消或失败时释放冻结积分。 */
    RELEASE,
    /** 售后、异常补偿或支付退款产生的积分退回。 */
    REFUND,
    /** 积分超过有效期后的过期扣减。 */
    EXPIRE
}
