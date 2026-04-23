/**
 * 订单状态枚举
 */

package cn.gaifan.douyinOperations.module.payment.entity;

/**
 * 订单状态
 */
public enum OrderStatus {
    PENDING_PAYMENT("待支付"),
    PAID("已支付"),
    SHIPPED("已发货"),
    COMPLETED("已完成"),
    REFUNDED("已退款"),
    CANCELLED("已取消");

    public final String description;

    OrderStatus(String description) {
        this.description = description;
    }
}
