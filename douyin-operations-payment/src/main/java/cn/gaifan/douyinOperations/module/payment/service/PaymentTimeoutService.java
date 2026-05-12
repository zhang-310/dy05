package cn.gaifan.douyinOperations.module.payment.service;

import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.entity.TransactionStatus;
import cn.gaifan.douyinOperations.module.payment.entity.TransactionType;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * P1-8: 支付超时自动取消服务
 * 定时扫描超时订单并自动取消
 */
@Slf4j
@Service
public class PaymentTimeoutService {

    @Resource
    private PaymentOrderRepository orderRepository;

    @Resource
    private DouyinPaymentService paymentService;

    /**
     * 每 5 分钟执行一次，取消超时订单
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredOrders() {
        log.info("开始取消超时订单...");

        // 支付超时时间为 30 分钟
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(
                DouyinPaymentService.PaymentConfig.TIMEOUT_MINUTES);

        List<PaymentOrder> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING_PAYMENT, expiredTime);

        for (PaymentOrder order : expiredOrders) {
            try {
                order.setStatus(OrderStatus.CANCELLED);
                order.setRemark("支付超时自动取消");
                orderRepository.save(order);

                log.info("订单超时取消: orderId={}, orderNo={}", order.getId(), order.getOrderNo());

                // 记录交易日志
                recordTransaction(order.getId(), TransactionType.CANCEL,
                        order.getActualAmount(), TransactionStatus.SUCCESS, "TIMEOUT");

            } catch (Exception e) {
                log.error("取消超时订单失败: orderId={}", order.getId(), e);
            }
        }

        log.info("超时订单取消完成，共处理 {} 个订单", expiredOrders.size());
    }

    /**
     * 记录交易日志（简化实现）
     */
    private void recordTransaction(Long orderId, TransactionType type,
                                   java.math.BigDecimal amount, TransactionStatus status, String txId) {
        log.debug("记录交易：orderId={}, type={}, status={}", orderId, type, status);
        // TODO: 实际应该调用 TransactionService 记录到数据库
    }
}
