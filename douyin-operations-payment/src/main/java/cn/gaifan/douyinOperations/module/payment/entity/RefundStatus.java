/**
 * 退款状态枚举
 */

package cn.gaifan.douyinOperations.module.payment.entity;

/**
 * 退款状态
 */
public enum RefundStatus {
    PENDING("待审核"),
    APPROVED("已批准"),
    PROCESSING("处理中"),
    COMPLETED("已完成"),
    REJECTED("已拒绝");

    public final String description;

    RefundStatus(String description) {
        this.description = description;
    }
}
