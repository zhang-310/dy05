package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.payment.service.OrderService;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSaveVO;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSearchVO;
import cn.gaifan.douyinOperations.module.payment.vo.OrderVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("OrderController 集成测试")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("创建订单 - 应返回 200")
    void createOrder_shouldReturn200() throws Exception {
        OrderSaveVO saveVO = new OrderSaveVO();
        saveVO.setOrderNo("ORD20260408001");
        saveVO.setProductId(1L);
        saveVO.setQuantity(2);
        saveVO.setAmount(BigDecimal.valueOf(199.00));
        saveVO.setActualAmount(BigDecimal.valueOf(199.00));

        when(orderService.createOrder(any(OrderSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/payment/order/create")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("创建订单（缺少必填字段）- 应返回 1001")
    void createOrder_missingFields_shouldReturn1001() throws Exception {
        OrderSaveVO saveVO = new OrderSaveVO();

        mockMvc.perform(post("/api/v1/payment/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("获取订单详情 - 应返回 200")
    void getOrder_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("orderId", 1L);

        OrderVO orderVO = new OrderVO();
        orderVO.setId(1L);
        orderVO.setOrderNo("ORD20260408001");
        orderVO.setAmount(BigDecimal.valueOf(199.00));

        when(orderService.getOrder(eq(1L)))
                .thenReturn(orderVO);

        mockMvc.perform(post("/api/v1/payment/order/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNo").value("ORD20260408001"));
    }

    @Test
    @DisplayName("按订单号获取 - 应返回 200")
    void getByOrderNo_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("orderNo", "ORD20260408001");

        OrderVO orderVO = new OrderVO();
        orderVO.setId(1L);
        orderVO.setOrderNo("ORD20260408001");

        when(orderService.getByOrderNo(eq("ORD20260408001")))
                .thenReturn(orderVO);

        mockMvc.perform(post("/api/v1/payment/order/getByOrderNo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("ORD20260408001"));
    }

    @Test
    @DisplayName("查询用户订单列表 - 应返回 200")
    void listOrders_shouldReturn200() throws Exception {
        OrderSearchVO searchVO = new OrderSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        OrderVO orderVO = new OrderVO();
        orderVO.setId(1L);
        orderVO.setOrderNo("ORD20260408001");

        PageResultVO<OrderVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(orderVO));

        when(orderService.searchByUser(any(OrderSearchVO.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/payment/order/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("查询用户订单列表（未登录）- 应返回 2001")
    void listOrders_withoutAuth_shouldReturn2001() throws Exception {
        OrderSearchVO searchVO = new OrderSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        mockMvc.perform(post("/api/v1/payment/order/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    @DisplayName("确认支付 - 应返回 200")
    void confirmPayment_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("orderId", "1");
        request.put("transactionId", "TXN20260408001");
        request.put("paymentMethod", "wechat");

        doNothing().when(orderService).confirmPayment(eq(1L), eq("TXN20260408001"), eq("wechat"));

        mockMvc.perform(post("/api/v1/payment/order/confirmPayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("发货 - 应返回 200")
    void shipOrder_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("orderId", "1");
        request.put("trackingNumber", "SF1234567890");

        doNothing().when(orderService).shipOrder(eq(1L), eq("SF1234567890"));

        mockMvc.perform(post("/api/v1/payment/order/ship")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("完成订单 - 应返回 200")
    void completeOrder_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("orderId", 1L);

        doNothing().when(orderService).completeOrder(eq(1L));

        mockMvc.perform(post("/api/v1/payment/order/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("取消订单 - 应返回 200")
    void cancelOrder_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("orderId", 1L);

        doNothing().when(orderService).cancelOrder(eq(1L));

        mockMvc.perform(post("/api/v1/payment/order/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
