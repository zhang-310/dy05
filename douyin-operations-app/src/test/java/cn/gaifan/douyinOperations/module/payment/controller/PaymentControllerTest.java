package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.module.payment.service.DouyinPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PaymentController 集成测试")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DouyinPaymentService paymentService;

    @Test
    @DisplayName("创建订单 - 应返回 200")
    void createOrder_shouldReturn200() throws Exception {
        DouyinPaymentService.CreateOrderRequest request = DouyinPaymentService.CreateOrderRequest.builder()
                .userId(1L)
                .productId(1L)
                .amount(BigDecimal.valueOf(99.99))
                .quantity(1)
                .discountCode(null)
                .remark(null)
                .build();

        DouyinPaymentService.PaymentResponse response = DouyinPaymentService.PaymentResponse.builder()
                .success(true)
                .orderNo("ORD20260408001")
                .paymentUrl("https://pay.douyin.com/xxx")
                .amount(BigDecimal.valueOf(99.99))
                .expiresAt(null)
                .errorMessage(null)
                .build();

        when(paymentService.createOrder(any(DouyinPaymentService.CreateOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payment/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("创建订单（缺少 userId）- 应返回 400")
    void createOrder_missingUserId_shouldReturn400() throws Exception {
        DouyinPaymentService.CreateOrderRequest request = DouyinPaymentService.CreateOrderRequest.builder()
                .userId(null)
                .productId(1L)
                .amount(BigDecimal.valueOf(99.99))
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/v1/payment/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("创建订单（金额为 0）- 应返回 400")
    void createOrder_zeroAmount_shouldReturn400() throws Exception {
        DouyinPaymentService.CreateOrderRequest request = DouyinPaymentService.CreateOrderRequest.builder()
                .userId(1L)
                .productId(1L)
                .amount(BigDecimal.ZERO)
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/v1/payment/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1002));
    }

    @Test
    @DisplayName("创建订单（服务失败）- 应返回 400")
    void createOrder_serviceFailed_shouldReturn400() throws Exception {
        DouyinPaymentService.CreateOrderRequest request = DouyinPaymentService.CreateOrderRequest.builder()
                .userId(1L)
                .productId(1L)
                .amount(BigDecimal.valueOf(99.99))
                .quantity(1)
                .build();

        DouyinPaymentService.PaymentResponse response = DouyinPaymentService.PaymentResponse.builder()
                .success(false)
                .orderNo(null)
                .paymentUrl(null)
                .amount(null)
                .expiresAt(null)
                .errorMessage("支付服务不可用")
                .build();

        when(paymentService.createOrder(any(DouyinPaymentService.CreateOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payment/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(3001));
    }

    @Test
    @DisplayName("查询订单 - 应返回 200")
    void getOrder_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/payment/order/ORD20260408001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("ORD20260408001"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("支付回调 - 应返回 200")
    void handlePaymentCallback_shouldReturn200() throws Exception {
        DouyinPaymentService.PaymentCallbackRequest callback = new DouyinPaymentService.PaymentCallbackRequest();
        callback.setOrderId("ORD20260408001");
        callback.setStatus("PAID");

        doNothing().when(paymentService).handlePaymentCallback(any(DouyinPaymentService.PaymentCallbackRequest.class));

        mockMvc.perform(post("/api/v1/payment/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("success"));
    }

    @Test
    @DisplayName("申请退款 - 应返回 200")
    void requestRefund_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/payment/refund")
                        .param("orderNo", "ORD20260408001")
                        .param("reason", "商品质量问题"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("获取订单列表 - 应返回 200")
    void listOrders_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/payment/orders")
                        .param("page", "0")
                        .param("rows", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(100));
    }

    @Test
    @DisplayName("触发对账 - 应返回 200")
    void triggerReconciliation_shouldReturn200() throws Exception {
        doNothing().when(paymentService).dailyReconciliation();

        mockMvc.perform(post("/api/v1/payment/reconciliation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取支付统计 - 应返回 200")
    void getPaymentStats_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/payment/stats")
                        .param("period", "today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalOrders").value(1000));
    }
}
