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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    @CacheEvict(value = {"payment:order", "payment:order:no"}, allEntries = true)
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

        // P0-2: 服务端验证订单金额（防止客户端篡改）
        // 注意：payment 模块不依赖 product 模块，金额验证由调用方（如 live 模块）在创建订单前完成
        // 这里仅做基本的金额合理性检查
        if (vo.getAmount() == null || vo.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "订单金额必须大于 0");
        }
        if (vo.getActualAmount() == null || vo.getActualAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "实付金额必须大于 0");
        }
        if (vo.getActualAmount().compareTo(vo.getAmount()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "实付金额不能大于订单金额");
        }

        // P0-6: 获取 ownerId（简化实现：假设单租户，ownerId = 1）
        // TODO: 实际应该从 AuthTokenFilter 或用户上下文获取 ownerId
        Long ownerId = 1L;

        // 创建订单
        PaymentOrder order = PaymentOrder.builder()
                .orderNo(vo.getOrderNo())
                .userId(userId)
                .ownerId(ownerId)
                .productId(vo.getProductId())
                .quantity(vo.getQuantity())
                .amount(vo.getAmount())
                .actualAmount(vo.getActualAmount())
                .status(OrderStatus.PENDING_PAYMENT)
                .remark(vo.getRemark())
                .build();

        order = orderRepository.save(order);
        // P1-3: 敏感信息脱敏 - 不记录金额到日志
        log.info("Order created: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"payment:order", "payment:order:no"}, allEntries = true)
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
    @Cacheable(value = "payment:order", key = "#orderId", unless = "#result == null")
    public OrderVO getOrder(Long orderId, Long userId) {
        PaymentOrder order = getOrderEntity(orderId);
        // P0-4: 验证订单归属（防止 IDOR 漏洞）
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该订单");
        }
        // P0-6: 验证 ownerId（数据隔离）
        Long ownerId = 1L; // TODO: 从上下文获取
        if (!order.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该订单");
        }
        return convertToVO(order);
    }

    @Override
    @Cacheable(value = "payment:order:no", key = "#orderNo", unless = "#result == null")
    public OrderVO getByOrderNo(String orderNo, Long userId) {
        PaymentOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
        // P0-4: 验证订单归属（防止 IDOR 漏洞）
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该订单");
        }
        // P0-6: 验证 ownerId（数据隔离）
        Long ownerId = 1L; // TODO: 从上下文获取
        if (!order.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该订单");
        }
        return convertToVO(order);
    }

    @Override
    public PageResultVO<OrderVO> searchByUser(OrderSearchVO vo, Long userId) {
        vo.validateParams();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        // P0-6: 使用 Specification 强制过滤 ownerId
        Long ownerId = 1L; // TODO: 从上下文获取
        Specification<PaymentOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 数据隔离（必须）
            predicates.add(cb.equal(root.get("ownerId"), ownerId));
            predicates.add(cb.equal(root.get("userId"), userId));

            // 动态条件
            if (vo.getStatus() != null && !vo.getStatus().isEmpty()) {
                try {
                    OrderStatus status = OrderStatus.valueOf(vo.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    // 忽略无效状态
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, "createdAt"));
        Page<PaymentOrder> page = orderRepository.findAll(spec, pageable);
        var list = page.getContent().stream().map(this::convertToVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String trackingNumber) {
        PaymentOrder order = getOrderEntity(orderId);

        // P1-2: 状态转换验证
        validateStatusTransition(order.getStatus(), OrderStatus.SHIPPED);

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

        // P1-2: 状态转换验证
        validateStatusTransition(order.getStatus(), OrderStatus.COMPLETED);

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Order completed: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        PaymentOrder order = getOrderEntity(orderId);

        // P1-2: 状态转换验证
        validateStatusTransition(order.getStatus(), OrderStatus.CANCELLED);

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

    // P1-2: 完整的状态机验证
    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        // 定义合法的状态转换
        Map<OrderStatus, Set<OrderStatus>> allowedTransitions = Map.of(
            OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.COMPLETED),
            OrderStatus.COMPLETED, Set.of(),  // 终态
            OrderStatus.CANCELLED, Set.of()   // 终态
        );

        Set<OrderStatus> allowed = allowedTransitions.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID,
                String.format("不允许从 %s 转换到 %s", from, to));
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
