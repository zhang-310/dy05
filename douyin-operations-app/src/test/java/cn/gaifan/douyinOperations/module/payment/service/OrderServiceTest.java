package cn.gaifan.douyinOperations.module.payment.service;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import cn.gaifan.douyinOperations.module.payment.service.impl.OrderServiceImpl;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSaveVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 订单服务单元测试
 */
@DisplayName("OrderService 单元测试")
class OrderServiceTest {

    @Mock
    private PaymentOrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("应该成功创建订单（幂等性）")
    void testCreateOrderIdempotency() {
        OrderSaveVO vo = OrderSaveVO.builder()
                .orderNo("ORDER_001")
                .productId(1L)
                .quantity(2)
                .amount(BigDecimal.valueOf(99.99))
                .actualAmount(BigDecimal.valueOf(89.99))
                .remark("Test order")
                .build();

        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .productId(1L)
                .quantity(2)
                .amount(BigDecimal.valueOf(99.99))
                .actualAmount(BigDecimal.valueOf(89.99))
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(orderRepository.findByOrderNo("ORDER_001")).thenReturn(Optional.of(order));

        long orderId = orderService.createOrder(vo, 1L);

        assertEquals(1L, orderId);
        verify(orderRepository, times(1)).findByOrderNo("ORDER_001");
    }

    @Test
    @DisplayName("应该创建订单并写入当前用户")
    void testCreateOrderAssignsCurrentUser() {
        OrderSaveVO vo = OrderSaveVO.builder()
                .orderNo("ORDER_002")
                .productId(2L)
                .quantity(1)
                .amount(BigDecimal.valueOf(59.99))
                .actualAmount(BigDecimal.valueOf(49.99))
                .remark("Owned order")
                .build();

        when(orderRepository.findByOrderNo("ORDER_002")).thenReturn(Optional.empty());
        when(orderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> {
            PaymentOrder saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        long orderId = orderService.createOrder(vo, 9L);

        assertEquals(2L, orderId);
        verify(orderRepository).save(argThat(order -> order.getUserId().equals(9L) && order.getProductId().equals(2L)));
    }

    @Test
    @DisplayName("应该成功确认支付")
    void testConfirmPayment() {
        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .productId(1L)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.confirmPayment(1L, "TXN_001", "DOUYIN_PAY");

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals("TXN_001", order.getTransactionId());
        assertNotNull(order.getPaidAt());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("应该拒绝非待支付状态的支付")
    void testConfirmPaymentWithInvalidStatus() {
        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .status(OrderStatus.COMPLETED)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> {
            orderService.confirmPayment(1L, "TXN_001", "DOUYIN_PAY");
        });
    }

    @Test
    @DisplayName("应该成功取消待支付订单")
    void testCancelOrder() {
        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("应该拒绝取消已完成订单")
    void testCancelCompletedOrder() {
        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .status(OrderStatus.COMPLETED)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> {
            orderService.cancelOrder(1L);
        });
    }

    @Test
    @DisplayName("应该成功发货")
    void testShipOrder() {
        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .status(OrderStatus.PAID)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.shipOrder(1L, "SF123456789");

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals("SF123456789", order.getTrackingNumber());
        assertNotNull(order.getShippedAt());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("应该成功完成订单")
    void testCompleteOrder() {
        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .status(OrderStatus.SHIPPED)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.completeOrder(1L);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertNotNull(order.getCompletedAt());
        verify(orderRepository, times(1)).save(order);
    }
}
