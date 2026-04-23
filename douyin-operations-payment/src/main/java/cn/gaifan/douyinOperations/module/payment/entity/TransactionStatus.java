/**
 * 交易状态枚举
 */

package cn.gaifan.douyinOperations.module.payment.entity;

/**
 * 交易状态
 */
public enum TransactionStatus {
    PENDING("待处理"),
    SUCCESS("成功"),
    FAILED("失败"),
    CANCELLED("已取消");

    public final String description;

    TransactionStatus(String description) {
        this.description = description;
    }
}
