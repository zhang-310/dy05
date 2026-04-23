package cn.gaifan.douyinOperations.module.payment.service;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentRefund;
import cn.gaifan.douyinOperations.module.payment.entity.RefundStatus;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentRefundRepository;
import cn.gaifan.douyinOperations.module.payment.service.impl.RefundServiceImpl;
import cn.gaifan.douyinOperations.module.payment.vo.RefundSaveVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 退款服务单元测试
 */
@DisplayName("RefundService 单元测试")
class RefundServiceTest {

    @Mock
    private PaymentRefundRepository refundRepository;

    @Mock
    private PaymentOrderRepository orderRepository;

    @InjectMocks
    private RefundServiceImpl refundService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("应该成功创建退款申请")
    void testCreateRefund() {
        RefundSaveVO vo = RefundSaveVO.builder()
                .orderId(1L)
                .amount(BigDecimal.valueOf(50.00))
                .reason("Product defective")
                .build();

        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .status(OrderStatus.PAID)
                .actualAmount(BigDecimal.valueOf(100.00))
                .build();

        PaymentRefund refund = PaymentRefund.builder()
                .id(1L)
                .orderId(1L)
                .amount(BigDecimal.valueOf(50.00))
                .status(RefundStatus.PENDING)
                .reason("Product defective")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());
        when(refundRepository.save(any())).thenReturn(refund);

        long refundId = refundService.createRefund(vo);

        assertEquals(1L, refundId);
        verify(orderRepository, atLeastOnce()).findById(1L);
        verify(refundRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("应该拒绝超额退款")
    void testRefundAmountExceed() {
        RefundSaveVO vo = RefundSaveVO.builder()
                .orderId(1L)
                .amount(BigDecimal.valueOf(150.00))
                .reason("Product defective")
                .build();

        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .status(OrderStatus.PAID)
                .actualAmount(BigDecimal.valueOf(100.00))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> {
            refundService.createRefund(vo);
        });
    }

    @Test
    @DisplayName("应该成功批准退款")
    void testApproveRefund() {
        PaymentRefund refund = PaymentRefund.builder()
                .id(1L)
                .orderId(1L)
                .amount(BigDecimal.valueOf(50.00))
                .status(RefundStatus.PENDING)
                .build();

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any())).thenReturn(refund);

        refundService.approveRefund(1L);

        assertEquals(RefundStatus.APPROVED, refund.getStatus());
        assertNotNull(refund.getApprovedAt());
        verify(refundRepository, times(1)).save(refund);
    }

    @Test
    @DisplayName("应该拒绝非待审核状态的批准")
    void testApproveRefundWithInvalidStatus() {
        PaymentRefund refund = PaymentRefund.builder()
                .id(1L)
                .orderId(1L)
                .amount(BigDecimal.valueOf(50.00))
                .status(RefundStatus.COMPLETED)
                .build();

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        assertThrows(BusinessException.class, () -> {
            refundService.approveRefund(1L);
        });
    }

    @Test
    @DisplayName("应该成功拒绝退款")
    void testRejectRefund() {
        PaymentRefund refund = PaymentRefund.builder()
                .id(1L)
                .orderId(1L)
                .amount(BigDecimal.valueOf(50.00))
                .status(RefundStatus.PENDING)
                .build();

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any())).thenReturn(refund);

        refundService.rejectRefund(1L, "Invalid reason");

        assertEquals(RefundStatus.REJECTED, refund.getStatus());
        assertEquals("Invalid reason", refund.getReason());
        verify(refundRepository, times(1)).save(refund);
    }

    @Test
    @DisplayName("应该成功完成退款")
    void testCompleteRefund() {
        PaymentRefund refund = PaymentRefund.builder()
                .id(1L)
                .orderId(1L)
                .amount(BigDecimal.valueOf(50.00))
                .status(RefundStatus.APPROVED)
                .build();

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any())).thenReturn(refund);

        refundService.completeRefund(1L);

        assertEquals(RefundStatus.COMPLETED, refund.getStatus());
        assertNotNull(refund.getCompletedAt());
        verify(refundRepository, times(1)).save(refund);
    }

    @Test
    @DisplayName("应该成功计算可退款金额")
    void testCanRefund() {
        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .status(OrderStatus.PAID)
                .actualAmount(BigDecimal.valueOf(100.00))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        boolean canRefund = refundService.canRefund(1L, BigDecimal.valueOf(50.00));

        assertTrue(canRefund);
    }

    @Test
    @DisplayName("应该拒绝零金额退款")
    void testCannotRefundZeroAmount() {
        PaymentOrder order = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORDER_001")
                .status(OrderStatus.PAID)
                .actualAmount(BigDecimal.valueOf(100.00))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        boolean canRefund = refundService.canRefund(1L, BigDecimal.ZERO);

        assertFalse(canRefund);
    }
}
