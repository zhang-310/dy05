package cn.gaifan.douyinOperations.module.payment.service;

import cn.gaifan.douyinOperations.module.payment.entity.Subscription;
import cn.gaifan.douyinOperations.module.payment.repository.SubscriptionRepository;
import cn.gaifan.douyinOperations.module.payment.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionServiceImpl 单元测试")
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    @DisplayName("getActiveSubscription 遇到过期订阅应返回 null")
    void getActiveSubscription_shouldReturnNullWhenSubscriptionExpired() {
        Subscription expired = new Subscription();
        expired.setUserId(100L);
        expired.setStatus("active");
        expired.setExpiresAt(Timestamp.valueOf("2026-04-01 00:00:00"));

        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(100L, 0, "active"))
                .thenReturn(Optional.of(expired));

        Subscription result = subscriptionService.getActiveSubscription(100L);

        assertNull(result);
    }

    @Test
    @DisplayName("createOrUpgrade 应创建新订阅并写入套餐额度")
    void createOrUpgrade_shouldCreateSubscriptionWithPlanLimits() {
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(100L, 0, "active"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = subscriptionService.createOrUpgrade(100L, "pro");

        assertEquals(100L, result.getUserId());
        assertEquals("pro", result.getPlan());
        assertEquals("active", result.getStatus());
        assertEquals(50, result.getMaxLiveSessions());
        assertEquals(100, result.getMaxSvProjects());
        assertEquals(-1, result.getMaxAiGenerations());
        assertEquals(5000, result.getMaxStorageMb());
        assertNotNull(result.getStartedAt());
        assertNotNull(result.getExpiresAt());
        long activeDays = Duration.between(result.getStartedAt().toInstant(), result.getExpiresAt().toInstant()).toDays();
        assertTrue(activeDays >= 29 && activeDays <= 30);
    }

    @Test
    @DisplayName("createOrUpgrade 应复用已有激活订阅对象")
    void createOrUpgrade_shouldReuseExistingSubscription() {
        Subscription existing = new Subscription();
        existing.setId(9L);
        existing.setUserId(100L);
        existing.setPlan("free");

        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(100L, 0, "active"))
                .thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(existing)).thenReturn(existing);

        Subscription result = subscriptionService.createOrUpgrade(100L, "enterprise");

        assertEquals(9L, result.getId());
        assertEquals("enterprise", result.getPlan());
        assertEquals(-1, result.getMaxLiveSessions());
        assertEquals(-1, result.getMaxStorageMb());
        verify(subscriptionRepository).save(existing);
    }

    @Test
    @DisplayName("checkQuota 对无限额度套餐应标记 unlimited 和 allowed")
    void checkQuota_shouldMarkUnlimitedPlanAsAllowed() {
        Subscription active = new Subscription();
        active.setPlan("enterprise");
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(100L, 0, "active"))
                .thenReturn(Optional.of(active));

        Map<String, Object> result = subscriptionService.checkQuota(100L, "aiGenerations");

        assertEquals("enterprise", result.get("plan"));
        assertEquals("aiGenerations", result.get("metric"));
        assertEquals(-1, result.get("limit"));
        assertEquals(true, result.get("unlimited"));
        assertEquals(true, result.get("allowed"));
    }

    @Test
    @DisplayName("getPlanDetails 对未知套餐应回退免费版")
    void getPlanDetails_shouldFallbackToFreePlan() {
        Map<String, Object> result = subscriptionService.getPlanDetails("unknown");

        assertEquals("unknown", result.get("plan"));
        assertTrue(((Map<?, ?>) result.get("limits")).containsKey("maxAiGenerations"));
        assertEquals("免费", result.get("price"));
        assertFalse(((java.util.List<?>) result.get("features")).isEmpty());
    }
}
