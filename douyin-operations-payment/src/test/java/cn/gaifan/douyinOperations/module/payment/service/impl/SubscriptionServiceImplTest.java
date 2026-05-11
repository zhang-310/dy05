package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.module.payment.entity.Subscription;
import cn.gaifan.douyinOperations.module.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private Subscription mockSubscription;

    @BeforeEach
    void setUp() {
        mockSubscription = new Subscription();
        mockSubscription.setId(1L);
        mockSubscription.setUserId(1L);
        mockSubscription.setOwnerId(1L);
        mockSubscription.setPlan("free");
        mockSubscription.setStatus("active");
        mockSubscription.setMaxLiveSessions(5);
        mockSubscription.setMaxSvProjects(10);
        mockSubscription.setMaxAiGenerations(50);
        mockSubscription.setMaxStorageMb(500);
        mockSubscription.setStartedAt(new Timestamp(System.currentTimeMillis()));
        mockSubscription.setExpiresAt(new Timestamp(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));
    }

    @Test
    void getActiveSubscription_Success() {
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(1L, 0, "active"))
                .thenReturn(Optional.of(mockSubscription));

        Subscription result = subscriptionService.getActiveSubscription(1L);

        assertThat(result).isNotNull();
        assertThat(result.getPlan()).isEqualTo("free");
        assertThat(result.getStatus()).isEqualTo("active");
    }

    @Test
    void getActiveSubscription_NotFound_ReturnsNull() {
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(1L, 0, "active"))
                .thenReturn(Optional.empty());

        Subscription result = subscriptionService.getActiveSubscription(1L);

        assertThat(result).isNull();
    }

    @Test
    void getActiveSubscription_Expired_ReturnsNull() {
        mockSubscription.setExpiresAt(new Timestamp(System.currentTimeMillis() - 1000));
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(1L, 0, "active"))
                .thenReturn(Optional.of(mockSubscription));

        Subscription result = subscriptionService.getActiveSubscription(1L);

        assertThat(result).isNull();
    }

    @Test
    void createOrUpgrade_NewSubscription_Success() {
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(1L, 0, "active"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(mockSubscription);

        Subscription result = subscriptionService.createOrUpgrade(1L, "pro");

        verify(subscriptionRepository).save(argThat(sub ->
            sub.getUserId().equals(1L) &&
            sub.getOwnerId().equals(1L) &&
            sub.getPlan().equals("pro") &&
            sub.getStatus().equals("active") &&
            sub.getMaxLiveSessions() == 50 &&
            sub.getMaxSvProjects() == 100 &&
            sub.getMaxAiGenerations() == -1 &&
            sub.getMaxStorageMb() == 5000
        ));
    }

    @Test
    void createOrUpgrade_UpgradeExisting_Success() {
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(1L, 0, "active"))
                .thenReturn(Optional.of(mockSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(mockSubscription);

        Subscription result = subscriptionService.createOrUpgrade(1L, "enterprise");

        verify(subscriptionRepository).save(argThat(sub ->
            sub.getId().equals(1L) &&
            sub.getPlan().equals("enterprise") &&
            sub.getMaxLiveSessions() == -1 &&
            sub.getMaxSvProjects() == -1 &&
            sub.getMaxAiGenerations() == -1 &&
            sub.getMaxStorageMb() == -1
        ));
    }

    @Test
    void checkQuota_FreePlan_ReturnsCorrectLimits() {
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(1L, 0, "active"))
                .thenReturn(Optional.of(mockSubscription));

        Map<String, Object> result = subscriptionService.checkQuota(1L, "liveSessions");

        assertThat(result).containsEntry("plan", "free");
        assertThat(result).containsEntry("metric", "liveSessions");
        assertThat(result).containsEntry("limit", 5);
        assertThat(result).containsEntry("unlimited", false);
    }

    @Test
    void checkQuota_ProPlan_UnlimitedAiGenerations() {
        mockSubscription.setPlan("pro");
        mockSubscription.setMaxAiGenerations(-1);
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(1L, 0, "active"))
                .thenReturn(Optional.of(mockSubscription));

        Map<String, Object> result = subscriptionService.checkQuota(1L, "aiGenerations");

        assertThat(result).containsEntry("plan", "pro");
        assertThat(result).containsEntry("limit", -1);
        assertThat(result).containsEntry("unlimited", true);
    }

    @Test
    void checkQuota_NoSubscription_DefaultsToFree() {
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(1L, 0, "active"))
                .thenReturn(Optional.empty());

        Map<String, Object> result = subscriptionService.checkQuota(1L, "liveSessions");

        assertThat(result).containsEntry("plan", "free");
        assertThat(result).containsEntry("limit", 5);
    }

    @Test
    void checkQuota_EnterprisePlan_AllUnlimited() {
        mockSubscription.setPlan("enterprise");
        mockSubscription.setMaxLiveSessions(-1);
        mockSubscription.setMaxSvProjects(-1);
        mockSubscription.setMaxAiGenerations(-1);
        mockSubscription.setMaxStorageMb(-1);
        when(subscriptionRepository.findByUserIdAndDeletedAndStatus(1L, 0, "active"))
                .thenReturn(Optional.of(mockSubscription));

        Map<String, Object> result = subscriptionService.checkQuota(1L, "storageMb");

        assertThat(result).containsEntry("plan", "enterprise");
        assertThat(result).containsEntry("unlimited", true);
    }

    @Test
    void isExpired_NotExpired_ReturnsFalse() {
        assertThat(mockSubscription.isExpired()).isFalse();
    }

    @Test
    void isExpired_Expired_ReturnsTrue() {
        mockSubscription.setExpiresAt(new Timestamp(System.currentTimeMillis() - 1000));
        assertThat(mockSubscription.isExpired()).isTrue();
    }

    @Test
    void isExpired_NoExpiryDate_ReturnsFalse() {
        mockSubscription.setExpiresAt(null);
        assertThat(mockSubscription.isExpired()).isFalse();
    }
}
