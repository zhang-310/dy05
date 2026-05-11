package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentRefund;
import cn.gaifan.douyinOperations.module.payment.entity.RefundStatus;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentRefundRepository;
import cn.gaifan.douyinOperations.module.payment.vo.RefundSaveVO;
import cn.gaifan.douyinOperations.module.payment.vo.RefundVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock
    private PaymentRefundRepository refundRepository;

    @Mock
    private PaymentOrderRepository orderRepository;

    @InjectMocks
    private RefundServiceImpl refundService;

    private PaymentOrder mockOrder;
    private PaymentRefund mockRefund;
    private RefundSaveVO validRefundVO;

    @BeforeEach
    void setUp() {
        mockOrder = PaymentOrder.builder()
                .id(1L)
                .orderNo("ORD20260512001")
                .userId(1L)
                .ownerId(1L)
                .productId(1L)
                .quantity(1)
                .amount(new BigDecimal("100.00"))
                .actualAmount(new BigDecimal("90.00"))
                .build();

        mockRefund = PaymentRefund.builder()
                .id(1L)
                .orderId(1L)
                .ownerId(1L)
                .amount(new BigDecimal("50.00"))
                .reason("商品质量问题")
                .status(RefundStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        validRefundVO = new RefundSaveVO();
        validRefundVO.setOrderId(1L);
        validRefundVO.setAmount(new BigDecimal("50.00"));
        validRefundVO.setReason("商品质量问题");
    }

    @Test
    void createRefund_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(refundRepository.findByOrderId(1L)).thenReturn(List.of());
        when(refundRepository.save(any(PaymentRefund.class))).thenReturn(mockRefund);

        long refundId = refundService.createRefund(validRefundVO);

        assertThat(refundId).isEqualTo(1L);
        verify(refundRepository).save(argThat(refund ->
            refund.getOrderId().equals(1L) &&
            refund.getOwnerId().equals(1L) &&
            refund.getAmount().equals(new BigDecimal("50.00")) &&
            refund.getStatus() == RefundStatus.PENDING
        ));
    }

    @Test
    void createRefund_OrderNotFound_ThrowsException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundService.createRefund(validRefundVO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void createRefund_AmountExceedsRefundable_ThrowsException() {
        validRefundVO.setAmount(new BigDecimal("100.00"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(refundRepository.findByOrderId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> refundService.createRefund(validRefundVO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFUND_AMOUNT_EXCEED);
    }

    @Test
    void approveRefund_Success() {
        when(refundRepository.findById(1L)).thenReturn(Optional.of(mockRefund));
        when(refundRepository.save(any(PaymentRefund.class))).thenReturn(mockRefund);

        refundService.approveRefund(1L);

        verify(refundRepository).save(argThat(refund ->
            refund.getStatus() == RefundStatus.APPROVED &&
            refund.getApprovedAt() != null
        ));
    }

    @Test
    void approveRefund_InvalidStatus_ThrowsException() {
        mockRefund.setStatus(RefundStatus.COMPLETED);
        when(refundRepository.findById(1L)).thenReturn(Optional.of(mockRefund));

        assertThatThrownBy(() -> refundService.approveRefund(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFUND_STATUS_INVALID);
    }

    @Test
    void rejectRefund_Success() {
        when(refundRepository.findById(1L)).thenReturn(Optional.of(mockRefund));
        when(refundRepository.save(any(PaymentRefund.class))).thenReturn(mockRefund);

        refundService.rejectRefund(1L, "库存不足");

        verify(refundRepository).save(argThat(refund ->
            refund.getStatus() == RefundStatus.REJECTED
        ));
    }

    @Test
    void completeRefund_Success() {
        mockRefund.setStatus(RefundStatus.APPROVED);
        when(refundRepository.findById(1L)).thenReturn(Optional.of(mockRefund));
        when(refundRepository.save(any(PaymentRefund.class))).thenReturn(mockRefund);

        refundService.completeRefund(1L);

        verify(refundRepository).save(argThat(refund ->
            refund.getStatus() == RefundStatus.COMPLETED &&
            refund.getCompletedAt() != null
        ));
    }

    @Test
    void completeRefund_InvalidStatus_ThrowsException() {
        mockRefund.setStatus(RefundStatus.PENDING);
        when(refundRepository.findById(1L)).thenReturn(Optional.of(mockRefund));

        assertThatThrownBy(() -> refundService.completeRefund(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFUND_STATUS_INVALID);
    }

    @Test
    void getRefund_Success() {
        when(refundRepository.findById(1L)).thenReturn(Optional.of(mockRefund));

        RefundVO result = refundService.getRefund(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void getRefund_WrongOwner_ThrowsException() {
        mockRefund.setOwnerId(999L);
        when(refundRepository.findById(1L)).thenReturn(Optional.of(mockRefund));

        assertThatThrownBy(() -> refundService.getRefund(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void getRefundsByOrderId_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(refundRepository.findByOrderId(1L)).thenReturn(List.of(mockRefund));

        List<RefundVO> result = refundService.getRefundsByOrderId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void calculateRefundedAmount_Success() {
        PaymentRefund completedRefund = PaymentRefund.builder()
                .id(2L)
                .orderId(1L)
                .ownerId(1L)
                .amount(new BigDecimal("30.00"))
                .status(RefundStatus.COMPLETED)
                .build();

        when(refundRepository.findByOrderId(1L)).thenReturn(List.of(mockRefund, completedRefund));

        BigDecimal result = refundService.calculateRefundedAmount(1L);

        assertThat(result).isEqualTo(new BigDecimal("30.00"));
    }

    @Test
    void canRefund_ValidAmount_ReturnsTrue() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(refundRepository.findByOrderId(1L)).thenReturn(List.of());

        boolean result = refundService.canRefund(1L, new BigDecimal("50.00"));

        assertThat(result).isTrue();
    }

    @Test
    void canRefund_ExceedsRefundable_ReturnsFalse() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(refundRepository.findByOrderId(1L)).thenReturn(List.of());

        boolean result = refundService.canRefund(1L, new BigDecimal("100.00"));

        assertThat(result).isFalse();
    }

    @Test
    void canRefund_NegativeAmount_ReturnsFalse() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(refundRepository.findByOrderId(1L)).thenReturn(List.of());

        boolean result = refundService.canRefund(1L, new BigDecimal("-10.00"));

        assertThat(result).isFalse();
    }
}
