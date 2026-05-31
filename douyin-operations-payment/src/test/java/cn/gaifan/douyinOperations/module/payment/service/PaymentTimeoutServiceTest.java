package cn.gaifan.douyinOperations.module.payment.service;

import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentTransactionLog;
import cn.gaifan.douyinOperations.module.payment.entity.TransactionStatus;
import cn.gaifan.douyinOperations.module.payment.entity.TransactionType;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentTransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTimeoutServiceTest {

    @Mock
    private PaymentOrderRepository orderRepository;

    @Mock
    private PaymentTransactionLogRepository transactionLogRepository;

    @Mock
    private DouyinPaymentService paymentService;

    @InjectMocks
    private PaymentTimeoutService service;

    @Test
    void cancelExpiredOrders_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(service, "schedulerEnabled", false);

        service.cancelExpiredOrders();

        verify(orderRepository, never()).findByStatusAndCreatedAtBefore(eq(OrderStatus.PENDING_PAYMENT),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void cancelExpiredOrders_shouldCancelOrderAndWriteTransactionLog() {
        ReflectionTestUtils.setField(service, "schedulerEnabled", true);
        PaymentOrder expiredOrder = PaymentOrder.builder()
                .id(10L)
                .orderNo("ORD_TIMEOUT")
                .actualAmount(new BigDecimal("88.50"))
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredOrder));

        service.cancelExpiredOrders();

        verify(orderRepository).save(expiredOrder);
        assertThat(expiredOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(expiredOrder.getRemark()).isEqualTo("支付超时自动取消");

        ArgumentCaptor<PaymentTransactionLog> captor = ArgumentCaptor.forClass(PaymentTransactionLog.class);
        verify(transactionLogRepository).save(captor.capture());
        PaymentTransactionLog log = captor.getValue();
        assertThat(log.getOrderId()).isEqualTo(10L);
        assertThat(log.getType()).isEqualTo(TransactionType.CANCEL);
        assertThat(log.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(log.getAmount()).isEqualByComparingTo("88.50");
        assertThat(log.getExternalTransactionId()).isEqualTo("TIMEOUT");
        assertThat(log.getRemarks()).isEqualTo("支付超时自动取消");
        assertThat(log.getCreatedAt()).isNotNull();
    }
}
