package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSearchVO;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSaveVO;
import cn.gaifan.douyinOperations.module.payment.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private PaymentOrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderSaveVO validOrderVO;
    private PaymentOrder mockOrder;

    @BeforeEach
    void setUp() {
        validOrderVO = new OrderSaveVO();
        validOrderVO.setOrderNo("ORD20260512001");
        validOrderVO.setProductId(1L);
        validOrderVO.setQuantity(1);
        validOrderVO.setAmount(new BigDecimal("100.00"));
        validOrderVO.setActualAmount(new BigDecimal("90.00"));
        validOrderVO.setRemark("测试订单");

        mockOrder = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORD20260512001")
                .userId(1L)
                .ownerId(1L)
                .productId(1L)
                .quantity(1)
                .amount(new BigDecimal("100.00"))
                .actualAmount(new BigDecimal("90.00"))
                .status(OrderStatus.PENDING_PAYMENT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createOrder_Success() {
        when(orderRepository.findByOrderNo(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(PaymentOrder.class))).thenReturn(mockOrder);

        long orderId = orderService.createOrder(validOrderVO, 1L);

        assertThat(orderId).isEqualTo(1L);
        verify(orderRepository).save(any(PaymentOrder.class));
    }

    @Test
    void createOrder_Idempotent() {
        when(orderRepository.findByOrderNo(anyString())).thenReturn(Optional.of(mockOrder));

        long orderId = orderService.createOrder(validOrderVO, 1L);

        assertThat(orderId).isEqualTo(1L);
        verify(orderRepository, never()).save(any(PaymentOrder.class));
    }

    @Test
    void createOrder_NullUserId_ThrowsException() {
        assertThatThrownBy(() -> orderService.createOrder(validOrderVO, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    void createOrder_InvalidAmount_ThrowsException() {
        validOrderVO.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> orderService.createOrder(validOrderVO, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL);
    }

    @Test
    void createOrder_ActualAmountExceedsAmount_ThrowsException() {
        validOrderVO.setActualAmount(new BigDecimal("110.00"));

        assertThatThrownBy(() -> orderService.createOrder(validOrderVO, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL);
    }

    @Test
    void updateOrderStatus_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(PaymentOrder.class))).thenReturn(mockOrder);

        orderService.updateOrderStatus(1L, "PAID");

        verify(orderRepository).save(argThat(order -> order.getStatus() == OrderStatus.PAID));
    }

    @Test
    void updateOrderStatus_InvalidStatus_ThrowsException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "INVALID_STATUS"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_STATUS_INVALID);
    }

    @Test
    void confirmPayment_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(PaymentOrder.class))).thenReturn(mockOrder);

        orderService.confirmPayment(1L, "TXN123", "alipay");

        verify(orderRepository).save(argThat(order ->
            order.getStatus() == OrderStatus.PAID &&
            order.getTransactionId().equals("TXN123") &&
            order.getPaymentMethod().equals("alipay") &&
            order.getPaidAt() != null
        ));
    }

    @Test
    void confirmPayment_InvalidStatus_ThrowsException() {
        mockOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> orderService.confirmPayment(1L, "TXN123", "alipay"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_STATUS_INVALID);
    }

    @Test
    void getOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        OrderVO result = orderService.getOrder(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderNo()).isEqualTo("ORD20260512001");
    }

    @Test
    void getOrder_WrongUser_ThrowsException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> orderService.getOrder(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void getOrder_NotFound_ThrowsException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void searchByUser_Success() {
        OrderSearchVO searchVO = new OrderSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        Page<PaymentOrder> mockPage = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResultVO<OrderVO> result = orderService.searchByUser(searchVO, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
    }

    @Test
    void shipOrder_Success() {
        mockOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(PaymentOrder.class))).thenReturn(mockOrder);

        orderService.shipOrder(1L, "SF123456");

        verify(orderRepository).save(argThat(order ->
            order.getStatus() == OrderStatus.SHIPPED &&
            order.getTrackingNumber().equals("SF123456") &&
            order.getShippedAt() != null
        ));
    }

    @Test
    void shipOrder_InvalidStatus_ThrowsException() {
        mockOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> orderService.shipOrder(1L, "SF123456"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_STATUS_INVALID);
    }

    @Test
    void completeOrder_Success() {
        mockOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(PaymentOrder.class))).thenReturn(mockOrder);

        orderService.completeOrder(1L);

        verify(orderRepository).save(argThat(order ->
            order.getStatus() == OrderStatus.COMPLETED &&
            order.getCompletedAt() != null
        ));
    }

    @Test
    void cancelOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(PaymentOrder.class))).thenReturn(mockOrder);

        orderService.cancelOrder(1L);

        verify(orderRepository).save(argThat(order -> order.getStatus() == OrderStatus.CANCELLED));
    }

    @Test
    void cancelOrder_CompletedOrder_ThrowsException() {
        mockOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_STATUS_INVALID);
    }
}
