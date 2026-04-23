package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.module.payment.entity.Subscription;
import cn.gaifan.douyinOperations.module.payment.service.SubscriptionService;
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

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("SubscriptionController 集成测试")
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("获取当前订阅 - 应返回 200")
    void current_shouldReturn200() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setUserId(1L);
        subscription.setPlan("premium");

        when(subscriptionService.getActiveSubscription(eq(1L)))
                .thenReturn(subscription);

        mockMvc.perform(post("/api/v1/payment/subscription/current")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.plan").value("premium"));
    }

    @Test
    @DisplayName("获取当前订阅（未登录）- 应返回 2001")
    void current_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/payment/subscription/current")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("升级订阅 - 应返回 200")
    void upgrade_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("plan", "premium");

        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setUserId(1L);
        subscription.setPlan("premium");

        when(subscriptionService.createOrUpgrade(eq(1L), eq("premium")))
                .thenReturn(subscription);

        mockMvc.perform(post("/api/v1/payment/subscription/upgrade")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.plan").value("premium"));
    }

    @Test
    @DisplayName("升级订阅（默认 plan）- 应返回 200")
    void upgrade_defaultPlan_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();

        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setUserId(1L);
        subscription.setPlan("free");

        when(subscriptionService.createOrUpgrade(eq(1L), eq("free")))
                .thenReturn(subscription);

        mockMvc.perform(post("/api/v1/payment/subscription/upgrade")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.plan").value("free"));
    }

    @Test
    @DisplayName("升级订阅（未登录）- 应返回 2001")
    void upgrade_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("plan", "premium");

        mockMvc.perform(post("/api/v1/payment/subscription/upgrade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("检查配额 - 应返回 200")
    void checkQuota_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("metric", "liveSessions");

        Map<String, Object> quota = new HashMap<>();
        quota.put("used", 5);
        quota.put("limit", 10);
        quota.put("remaining", 5);

        when(subscriptionService.checkQuota(eq(1L), eq("liveSessions")))
                .thenReturn(quota);

        mockMvc.perform(post("/api/v1/payment/subscription/check-quota")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.used").value(5))
                .andExpect(jsonPath("$.data.limit").value(10));
    }

    @Test
    @DisplayName("检查配额（默认 metric）- 应返回 200")
    void checkQuota_defaultMetric_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();

        Map<String, Object> quota = new HashMap<>();
        quota.put("used", 5);
        quota.put("limit", 10);

        when(subscriptionService.checkQuota(eq(1L), eq("liveSessions")))
                .thenReturn(quota);

        mockMvc.perform(post("/api/v1/payment/subscription/check-quota")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取套餐详情 - 应返回 200")
    void plans_shouldReturn200() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("plan", "premium");

        Map<String, Object> planDetails = new HashMap<>();
        planDetails.put("name", "Premium");
        planDetails.put("price", 99.99);
        planDetails.put("features", "Unlimited sessions");

        when(subscriptionService.getPlanDetails(eq("premium")))
                .thenReturn(planDetails);

        mockMvc.perform(post("/api/v1/payment/subscription/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("Premium"));
    }

    @Test
    @DisplayName("获取套餐详情（空 body）- 应返回 200")
    void plans_emptyBody_shouldReturn200() throws Exception {
        Map<String, Object> planDetails = new HashMap<>();
        planDetails.put("name", "Free");
        planDetails.put("price", 0);

        when(subscriptionService.getPlanDetails(eq("free")))
                .thenReturn(planDetails);

        mockMvc.perform(post("/api/v1/payment/subscription/plans")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("Free"));
    }
}
