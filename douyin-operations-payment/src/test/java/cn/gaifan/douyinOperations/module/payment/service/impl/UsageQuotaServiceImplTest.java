package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.common.tenant.TenantOrgResolutionHelper;
import cn.gaifan.douyinOperations.module.payment.entity.Subscription;
import cn.gaifan.douyinOperations.module.payment.entity.UsageRecord;
import cn.gaifan.douyinOperations.module.payment.repository.UsageRecordRepository;
import cn.gaifan.douyinOperations.module.payment.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageQuotaServiceImplTest {

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private TenantOrgResolutionHelper tenantOrgResolutionHelper;

    @InjectMocks
    private UsageQuotaServiceImpl usageQuotaService;

    @Test
    void recordUsage_ShouldPersistOrgScopedUsageRecord() {
        when(tenantOrgResolutionHelper.organizationIdForUser(100L)).thenReturn(200L);

        usageQuotaService.recordUsage(100L, "aiGenerations", 3);

        verify(usageRecordRepository).save(org.mockito.ArgumentMatchers.argThat((UsageRecord record) ->
                record.getUserId().equals(100L)
                        && record.getOrgId().equals(200L)
                        && "aiGenerations".equals(record.getMetric())
                        && record.getDelta() == 3
        ));
    }

    @Test
    void getUsageQuotaSummary_ShouldAggregateRealUsageAndSubscriptionLimits() {
        when(subscriptionService.checkQuota(100L, "aiGenerations"))
                .thenReturn(Map.of("limit", 1000, "allowed", true, "unlimited", false));
        when(subscriptionService.checkQuota(100L, "liveSessions"))
                .thenReturn(Map.of("limit", 50, "allowed", true, "unlimited", false));
        when(subscriptionService.checkQuota(100L, "svProjects"))
                .thenReturn(Map.of("limit", 100, "allowed", true, "unlimited", false));
        when(subscriptionService.checkQuota(100L, "storageMb"))
                .thenReturn(Map.of("limit", 5000, "allowed", true, "unlimited", false));
        when(usageRecordRepository.sumUsageSince(eq(100L), eq("aiGenerations"), any(Timestamp.class)))
                .thenReturn(120, 70);
        when(usageRecordRepository.sumUsageSince(eq(100L), eq("liveSessions"), any(Timestamp.class)))
                .thenReturn(8);
        when(usageRecordRepository.sumUsageSince(eq(100L), eq("svProjects"), any(Timestamp.class)))
                .thenReturn(12);
        when(usageRecordRepository.sumUsageSince(eq(100L), eq("storageMb"), any(Timestamp.class)))
                .thenReturn(640);

        Map<String, Object> result = usageQuotaService.getUsageQuotaSummary(100L);

        assertThat(result).containsEntry("source", "payment_usage_record");
        assertThat(result).containsEntry("aiCallUsed", 120);
        assertThat(result).containsEntry("aiCallLimit", 1000);
        assertThat(result).containsEntry("aiCallLast7d", 70);
        assertThat(result).containsEntry("liveSessionUsed", 8);
        assertThat(result).containsEntry("storageLimit", 5000);
        Map<?, ?> aiCall = (Map<?, ?>) result.get("aiCall");
        Map<?, ?> liveSession = (Map<?, ?>) result.get("liveSession");
        Map<?, ?> storage = (Map<?, ?>) result.get("storage");
        assertThat(aiCall.get("metric")).isEqualTo("aiGenerations");
        assertThat(liveSession.get("total")).isEqualTo(50);
        assertThat(storage.get("used")).isEqualTo(640);
    }

    @Test
    void isWithinQuota_ShouldSkipUsageLookupForUnlimitedPlan() {
        Subscription subscription = new Subscription();
        subscription.setMaxAiGenerations(-1);
        when(subscriptionService.getActiveSubscription(100L)).thenReturn(subscription);

        assertThat(usageQuotaService.isWithinQuota(100L, "aiGenerations")).isTrue();
    }
}
