/**
 * 交易类型枚举
 */

package cn.gaifan.douyinOperations.module.payment.entity;

/**
 * 交易类型
 */
public enum TransactionType {
    PAYMENT("支付"),
    REFUND("退款"),
    ADJUSTMENT("调整");

    public final String description;

    TransactionType(String description) {
        this.description = description;
    }
}
