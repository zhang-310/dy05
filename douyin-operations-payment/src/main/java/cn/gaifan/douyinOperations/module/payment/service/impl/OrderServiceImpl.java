package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import cn.gaifan.douyinOperations.module.payment.service.OrderService;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSearchVO;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSaveVO;
import cn.gaifan.douyinOperations.module.payment.vo.OrderVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private PaymentOrderRepository orderRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createOrder(OrderSaveVO vo, Long userId) {
        if (vo == null || vo.getProductId() == null || vo.getProductId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "参数校验失败");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        // 幂等性检查
        var existing = orderRepository.findByOrderNo(vo.getOrderNo());
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        // 创建订单
        PaymentOrder order = PaymentOrder.builder()
                .orderNo(vo.getOrderNo())
                .userId(userId)
                .productId(vo.getProductId())
                .quantity(vo.getQuantity())
                .amount(vo.getAmount())
                .actualAmount(vo.getActualAmount())
                .status(OrderStatus.PENDING_PAYMENT)
                .remark(vo.getRemark())
                .build();

        order = orderRepository.save(order);
        log.info("Order created: orderId={}, orderNo={}, amount={}", order.getId(), order.getOrderNo(), order.getAmount());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long orderId, String newStatus) {
        PaymentOrder order = getOrderEntity(orderId);

        try {
            OrderStatus status = OrderStatus.valueOf(newStatus.toUpperCase());
            validateStatusTransition(order.getStatus(), status);
            order.setStatus(status);
            orderRepository.save(order);
            log.info("Order status updated: orderId={}, newStatus={}", orderId, newStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态无效: " + newStatus);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(Long orderId, String transactionId, String paymentMethod) {
        PaymentOrder order = getOrderEntity(orderId);

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不允许支付");
        }

        order.setTransactionId(transactionId);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Payment confirmed: orderId={}, transactionId={}", orderId, transactionId);
    }

    @Override
    public OrderVO getOrder(Long orderId) {
        return convertToVO(getOrderEntity(orderId));
    }

    @Override
    public OrderVO getByOrderNo(String orderNo) {
        PaymentOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
        return convertToVO(order);
    }

    @Override
    public PageResultVO<OrderVO> searchByUser(OrderSearchVO vo, Long userId) {
        vo.validateParams();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, "createdAt"));
        Page<PaymentOrder> page = orderRepository.findByUserId(userId, pageable);
        var list = page.getContent().stream().map(this::convertToVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String trackingNumber) {
        PaymentOrder order = getOrderEntity(orderId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不允许发货");
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        order.setShippedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Order shipped: orderId={}, trackingNumber={}", orderId, trackingNumber);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long orderId) {
        PaymentOrder order = getOrderEntity(orderId);

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不允许完成");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Order completed: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        PaymentOrder order = getOrderEntity(orderId);

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不允许取消");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order cancelled: orderId={}", orderId);
    }

    @Override
    public BigDecimal sumCompletedAmount(String startDate, String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime end = LocalDateTime.parse(endDate + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Long sum = orderRepository.sumCompletedAmountByDateRange(start, end);
        return sum != null ? BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
    }

    private PaymentOrder getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        if (from == OrderStatus.COMPLETED || from == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "已完成或已取消订单不能更改状态");
        }
    }

    private OrderVO convertToVO(PaymentOrder order) {
        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .actualAmount(order.getActualAmount())
                .status(order.getStatus().name())
                .paymentMethod(order.getPaymentMethod())
                .transactionId(order.getTransactionId())
                .paidAt(order.getPaidAt())
                .trackingNumber(order.getTrackingNumber())
                .shippedAt(order.getShippedAt())
                .completedAt(order.getCompletedAt())
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
