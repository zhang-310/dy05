package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.module.payment.service.RefundService;
import cn.gaifan.douyinOperations.module.payment.vo.RefundSaveVO;
import cn.gaifan.douyinOperations.module.payment.vo.RefundVO;
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
@DisplayName("RefundController 集成测试")
class RefundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RefundService refundService;

    @Test
    @DisplayName("创建退款申请 - 应返回 200")
    void createRefund_shouldReturn200() throws Exception {
        RefundSaveVO saveVO = new RefundSaveVO();
        saveVO.setOrderId(1L);
        saveVO.setAmount(BigDecimal.valueOf(99.99));
        saveVO.setReason("商品质量问题");

        when(refundService.createRefund(any(RefundSaveVO.class), eq(1L)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/payment/refund/create")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("创建退款申请（缺少必填字段）- 应返回 1001")
    void createRefund_missingFields_shouldReturn1001() throws Exception {
        RefundSaveVO saveVO = new RefundSaveVO();

        mockMvc.perform(post("/api/v1/payment/refund/create")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("获取退款详情 - 应返回 200")
    void getRefund_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("refundId", 1L);

        RefundVO refundVO = new RefundVO();
        refundVO.setId(1L);
        refundVO.setOrderId(1L);
        refundVO.setAmount(BigDecimal.valueOf(99.99));
        refundVO.setReason("商品质量问题");

        when(refundService.getRefund(eq(1L), eq(1L)))
                .thenReturn(refundVO);

        mockMvc.perform(post("/api/v1/payment/refund/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderId").value(1));
    }

    @Test
    @DisplayName("查询订单的所有退款 - 应返回 200")
    void listByOrder_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("orderId", 1L);

        RefundVO refundVO = new RefundVO();
        refundVO.setId(1L);
        refundVO.setOrderId(1L);

        when(refundService.getRefundsByOrderId(eq(1L), eq(1L)))
                .thenReturn(List.of(refundVO));

        mockMvc.perform(post("/api/v1/payment/refund/listByOrder")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("批准退款 - 应返回 200")
    void approveRefund_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("refundId", 1L);

        doNothing().when(refundService).approveRefund(eq(1L));

        mockMvc.perform(post("/api/v1/payment/refund/approve")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("拒绝退款 - 应返回 200")
    void rejectRefund_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("refundId", "1");
        request.put("reason", "不符合退款条件");

        doNothing().when(refundService).rejectRefund(eq(1L), eq("不符合退款条件"));

        mockMvc.perform(post("/api/v1/payment/refund/reject")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("完成退款 - 应返回 200")
    void completeRefund_shouldReturn200() throws Exception {
        Map<String, Long> request = new HashMap<>();
        request.put("refundId", 1L);

        doNothing().when(refundService).completeRefund(eq(1L));

        mockMvc.perform(post("/api/v1/payment/refund/complete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
