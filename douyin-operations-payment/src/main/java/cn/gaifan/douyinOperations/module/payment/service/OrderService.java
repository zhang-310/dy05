package cn.gaifan.douyinOperations.module.payment.service;

import cn.gaifan.douyinOperations.module.payment.vo.OrderSearchVO;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSaveVO;
import cn.gaifan.douyinOperations.module.payment.vo.OrderVO;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;

import java.math.BigDecimal;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单（幂等）
     */
    long createOrder(OrderSaveVO vo, Long userId);

    /**
     * 更新订单状态
     */
    void updateOrderStatus(Long orderId, String newStatus);

    /**
     * 确认支付
     */
    void confirmPayment(Long orderId, String transactionId, String paymentMethod);

    /**
     * 查询订单详情（P0-4: 需验证归属）
     */
    OrderVO getOrder(Long orderId, Long userId);

    /**
     * 按订单号查询（P0-4: 需验证归属）
     */
    OrderVO getByOrderNo(String orderNo, Long userId);

    /**
     * 分页查询用户订单
     */
    PageResultVO<OrderVO> searchByUser(OrderSearchVO vo, Long userId);

    /**
     * 发货
     */
    void shipOrder(Long orderId, String trackingNumber);

    /**
     * 完成订单
     */
    void completeOrder(Long orderId);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId);

    /**
     * 统计日期范围内的完成订单金额
     */
    BigDecimal sumCompletedAmount(String startDate, String endDate);
}
